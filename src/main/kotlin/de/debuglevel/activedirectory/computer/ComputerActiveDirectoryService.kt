package de.debuglevel.activedirectory.computer

import de.debuglevel.activedirectory.ActiveDirectoryService
import de.debuglevel.activedirectory.ActiveDirectoryUtils.convertLdapTimestampToDate
import io.micronaut.context.annotation.Property
import mu.KotlinLogging
import java.io.IOException
import java.util.*
import javax.inject.Singleton
import javax.naming.NamingException
import javax.naming.directory.SearchResult
import javax.naming.ldap.Control
import javax.naming.ldap.PagedResultsControl
import javax.naming.ldap.PagedResultsResponseControl

// see original at https://myjeeva.com/querying-active-directory-using-java.html
@Singleton
class ComputerActiveDirectoryService(
    @Property(name = "app.activedirectory.username") username: String,
    @Property(name = "app.activedirectory.password") password: String,
    @Property(name = "app.activedirectory.server") domainController: String,
    @Property(name = "app.activedirectory.searchbase") searchBase: String
) : ActiveDirectoryService<Computer>(
    username,
    password,
    domainController,
    searchBase
) {
    private val logger = KotlinLogging.logger {}

    override val returnAttributes =
        arrayOf(
            "cn",
            "userAccountControl",
            "lastLogon",
            "whenCreated",
            "logonCount",
            "operatingSystem",
            "operatingSystemVersion"
        )

    /**
     * Gets users from the Active Directory by username/email id for given search base
     *
     * @param searchValue a [java.lang.String] object - search value used for AD search for eg. username or email
     * @param searchBy a [java.lang.String] object - scope of search by username or by email id
     * @return search result a [javax.naming.NamingEnumeration] object - active directory search result
     * @throws NamingException
     */
    fun getAll(searchValue: String, searchBy: ComputerSearchScope): List<Computer> {
        val computers = ArrayList<Computer>()

        try {
            val filter = buildFilter(searchValue, searchBy)

            val ldapContext = createLdapContext()

            // Activate paged results
            var cookie: ByteArray? = null
            ldapContext.requestControls = arrayOf<Control>(PagedResultsControl(pageSize, Control.NONCRITICAL))

            do {
                logger.debug { "Requesting page..." }

                val results = ldapContext.search(searchBase, filter, searchControls)

                while (results.hasMoreElements()) {
                    val result = results.nextElement()
                    val computer = build(result)
                    computers.add(computer)
                }

                // Examine the paged results control response
                val controls = ldapContext.responseControls
                if (controls != null) {
                    val pagedResultsResponseControls = controls
                        .filterIsInstance<PagedResultsResponseControl>()
                        .map { it }
                    for (pagedResultsResponseControl in pagedResultsResponseControls) {
                        val resultSize = when {
                            pagedResultsResponseControl.resultSize != 0 -> "${pagedResultsResponseControl.resultSize}"
                            else -> "unknown"
                        }

                        logger.debug { "Page ended (total: $resultSize)" }

                        cookie = pagedResultsResponseControl.cookie
                    }
                } else {
                    logger.debug("No controls were sent from the server")
                }

                // Re-activate paged results
                ldapContext.requestControls = arrayOf<Control>(PagedResultsControl(pageSize, cookie, Control.CRITICAL))

                logger.debug { "Fetched a total of ${computers.count()} entries." }
            } while (cookie != null)

            closeLdapContext(ldapContext)
        } catch (e: NamingException) {
            logger.error(e) { "PagedSearch failed." }
        } catch (e: IOException) {
            logger.error(e) { "PagedSearch failed." }
        } catch (e: Exception) {
            logger.error(e) { "PagedSearch failed." }
        }

        return computers
    }

    /**
     * Gets all computers from the Active Directory for given search base
     *
     * @return search result a [javax.naming.NamingEnumeration] object - active directory search result
     * @throws NamingException
     */
    fun getAll(): List<Computer> {
        //TODO("Does not work with more then 1000 users due to missing paging")
        logger.debug { "Getting all computers..." }

        val computers = getAll("*", ComputerSearchScope.Name)

        logger.debug { "Got ${computers.count()} computers..." }
        return computers
    }

    /**
     * Gets an user from the Active Directory by username/email id for given search base
     *
     * @param searchValue a [java.lang.String] object - search value used for AD search for eg. username or email
     * @param searchBy a [java.lang.String] object - scope of search by username or by email id
     * @return search result a [javax.naming.NamingEnumeration] object - active directory search result
     * @throws NamingException
     */
    fun get(searchValue: String, searchBy: ComputerSearchScope): Computer {
        logger.debug { "Getting computer '$searchValue' by $searchBy..." }

        val computers = getAll(searchValue, searchBy)

        if (computers.count() > 1) {
            throw MoreThanOneResultException(computers)
        } else if (computers.isEmpty()) {
            throw NoItemFoundException()
        }

        val computer = computers.first()

        logger.debug { "Got computer '$searchValue' by $searchBy: $computer" }
        return computer
    }

//    private fun build(results: NamingEnumeration<SearchResult>): List<Computer> {
//        logger.debug { "Building computer from search results..." }
//
//        val computers = results.asSequence()
//            .map { build(it) }
//            .onEach { logger.debug { "Got computer $it" } }
//            .toList()
//
//        logger.debug { "Built ${computers.count()} computers." }
//        return computers
//    }

    override fun build(it: SearchResult): Computer {
        val commonName = it.attributes.get("cn")?.toString()?.substringAfter(": ")
        val operatingSystem = it.attributes.get("operatingSystem")?.toString()?.substringAfter(": ")
        val operatingSystemVersion = it.attributes.get("operatingSystemVersion")?.toString()?.substringAfter(": ")
        val logonCount = it.attributes.get("logonCount")?.toString()?.substringAfter(": ")?.toInt()
        val userAccountControl =
            it.attributes.get("userAccountControl")?.toString()?.substringAfter(": ")?.toIntOrNull()
        val lastLogon = {
            val lastLogonTimestamp = it.attributes.get("lastLogon")?.toString()?.substringAfter(": ")?.toLong()
            if (lastLogonTimestamp != null && lastLogonTimestamp != 0L) {
                convertLdapTimestampToDate(lastLogonTimestamp)
            } else {
                null
            }
        }()
        val whenCreated = {
            val whenCreatedTimestamp = it.attributes.get("whenCreated")?.toString()?.substringAfter(": ")
            if (whenCreatedTimestamp != null) {
                convertLdapTimestampToDate(whenCreatedTimestamp)
            } else {
                null
            }
        }()

        return Computer(
            commonName,
            userAccountControl,
            logonCount,
            operatingSystem,
            operatingSystemVersion,
            lastLogon,
            whenCreated
        )
    }

    override fun buildFilter(searchValue: String, searchBy: ComputerSearchScope): String {
        logger.debug { "Building filter for searching by '$searchBy' for '$searchValue'..." }

        val filter = when (searchBy) {
            ComputerSearchScope.Name -> "(&($baseFilter)(name=$searchValue))"
        }

        logger.debug { "Built filter for searching by '$searchBy' for '$searchValue': $filter" }
        return filter
    }

    override val baseFilter = "(&(objectCategory=Computer)(objectClass=Computer))"
}
