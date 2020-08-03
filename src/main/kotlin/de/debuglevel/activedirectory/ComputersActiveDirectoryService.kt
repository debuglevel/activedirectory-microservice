package de.debuglevel.activedirectory

import io.micronaut.context.annotation.Property
import mu.KotlinLogging
import java.io.IOException
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import javax.inject.Singleton
import javax.naming.Context
import javax.naming.NamingEnumeration
import javax.naming.NamingException
import javax.naming.directory.SearchControls
import javax.naming.directory.SearchResult
import javax.naming.ldap.*

// see original at https://myjeeva.com/querying-active-directory-using-java.html
@Singleton
class ComputersActiveDirectoryService(
    @Property(name = "app.activedirectory.username") private val username: String,
    @Property(name = "app.activedirectory.password") private val password: String,
    @Property(name = "app.activedirectory.server") private val domainController: String,
    @Property(name = "app.activedirectory.searchbase") private val searchBase: String
) {
    private val logger = KotlinLogging.logger {}

    private val searchControls: SearchControls
    private val returnAttributes =
        arrayOf(
            "cn",
            "userAccountControl",
            "lastLogon",
            "whenCreated",
            "logonCount",
            "operatingSystem",
            "operatingSystemVersion"
        )
    private val pageSize = 1000

    init {
        // initializing search controls
        searchControls = SearchControls()
        searchControls.searchScope = SearchControls.SUBTREE_SCOPE
        searchControls.returningAttributes = returnAttributes
    }

    fun createLdapContext(): LdapContext {
        val properties: Properties = Properties()
        properties[Context.INITIAL_CONTEXT_FACTORY] = "com.sun.jndi.ldap.LdapCtxFactory"
        properties[Context.PROVIDER_URL] = "LDAP://$domainController"
        properties[Context.SECURITY_PRINCIPAL] = username
        properties[Context.SECURITY_CREDENTIALS] = password

        // initializing Active Directory LDAP connection
        return try {
            logger.debug { "Initializing LDAP connection with properties $properties..." }
            InitialLdapContext(properties, null)
        } catch (e: Exception) {
            logger.error(e) { "Initializing LDAP connection failed." }
            throw ConnectionException(e)
        }
    }

    /**
     * Gets users from the Active Directory by username/email id for given search base
     *
     * @param searchValue a [java.lang.String] object - search value used for AD search for eg. username or email
     * @param searchBy a [java.lang.String] object - scope of search by username or by email id
     * @return search result a [javax.naming.NamingEnumeration] object - active directory search result
     * @throws NamingException
     */
    fun getComputers(searchValue: String, searchBy: ComputerSearchScope): List<Computer> {
        val computers = ArrayList<Computer>()

        try {
            val filter = getFilter(
                searchValue,
                searchBy
            )

            val ldapContext = createLdapContext()

            // Activate paged results
            var cookie: ByteArray? = null
            ldapContext.requestControls = arrayOf<Control>(PagedResultsControl(pageSize, Control.NONCRITICAL))

            do {
                logger.debug { "Requesting page..." }

                val results = ldapContext.search(searchBase, filter, searchControls)

                while (results.hasMoreElements()) {
                    val result = results.nextElement()
                    val computer = buildComputer(result)
                    computers.add(computer)
                }

                // Examine the paged results control response
                val controls = ldapContext.responseControls
                if (controls != null) {
                    val pagedResultsResponseControls = controls
                        .filter { it is PagedResultsResponseControl }
                        .map { it as PagedResultsResponseControl }
                    for (pagedResultsResponseControl in pagedResultsResponseControls) {
                        val resultSize = if (pagedResultsResponseControl.resultSize != 0) {
                            "${pagedResultsResponseControl.resultSize}"
                        } else {
                            "unknown"
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
    fun getComputers(): List<Computer> {
        //TODO("Does not work with more then 1000 users due to missing paging")
        logger.debug { "Getting all computers..." }

        val computers = getComputers("*", ComputerSearchScope.Name)

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
    fun getComputer(searchValue: String, searchBy: ComputerSearchScope): Computer {
        logger.debug { "Getting computer '$searchValue' by $searchBy..." }

        val computers = getComputers(searchValue, searchBy)

        if (computers.count() > 1) {
            throw MoreThanOneResultException(computers)
        } else if (computers.isEmpty()) {
            throw NoUserFoundException()
        }

        val computer = computers.first()
        return computer
    }

    private fun buildComputers(results: NamingEnumeration<SearchResult>): List<Computer> {
        logger.debug { "Building computer from search results..." }

        val computers = results.asSequence()
            .map {
                buildComputer(it)
            }
            .onEach { logger.debug { "Got computer $it" } }
            .toList()

        logger.debug { "Built ${computers.count()} computers." }

        return computers
    }

    private fun buildComputer(it: SearchResult): Computer {
        val commonName = it.attributes.get("cn")?.toString()?.substringAfter(": ")
        val operatingSystem = it.attributes.get("operatingSystem")?.toString()?.substringAfter(": ")
        val operatingSystemVersion = it.attributes.get("operatingSystemVersion")?.toString()?.substringAfter(": ")
        val logonCount = it.attributes.get("logonCount")?.toString()?.substringAfter(": ")?.toInt()
        val userAccountControl =
            it.attributes.get("userAccountControl")?.toString()?.substringAfter(": ")?.toIntOrNull()
        val lastLogon = {
            val lastLogonTimestamp =
                it.attributes.get("lastLogon")?.toString()?.substringAfter(": ")?.toLong()
            if (lastLogonTimestamp != null && lastLogonTimestamp != 0L) {
                convertLdapTimestampToDate(lastLogonTimestamp)
            } else {
                null
            }
        }()
        val whenCreated = {
            val whenCreatedTimestamp =
                it.attributes.get("whenCreated")?.toString()?.substringAfter(": ")
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

    private fun convertLdapTimestampToDate(timestamp: Long): GregorianCalendar {
        // TODO: unknown if respects time zones
        val fileTime = timestamp / 10000L - +11644473600000L
        val date = Date(fileTime)
        val calendar = GregorianCalendar()
        calendar.time = date
        return calendar
    }

    private fun convertLdapTimestampToDate(timestamp: String): GregorianCalendar {
        // TODO: unknown if respects time zones
        val dateTimeFormatter = DateTimeFormatter.ofPattern("uuuuMMddHHmmss[,S][.S]X")
        val zonedDateTime = OffsetDateTime.parse(timestamp, dateTimeFormatter).toZonedDateTime()
        val calendar = GregorianCalendar.from(zonedDateTime)
        return calendar
    }

    /**
     * Closes the LDAP connection with Domain controller
     */
    fun closeLdapContext(ldapContext: LdapContext) {
        try {
            logger.debug { "Closing LDAP connection..." }
            ldapContext.close()
        } catch (e: NamingException) {
            logger.error(e) { "Failed closing LDAP connection." }
        }
    }

    companion object {
        private val logger = KotlinLogging.logger {}

        private const val baseFilter =
            "(&((&(objectCategory=Computer)(objectClass=Computer)))" //"(&(((objectClass=Computer)))"

        /**
         * Active Directory filter string value
         *
         * @param searchValue a [java.lang.String] object - search value of username/email id for active directory
         * @param searchBy a [java.lang.String] object - scope of search by username or email id
         * @return a [java.lang.String] object - filter string
         */
        fun getFilter(searchValue: String, searchBy: ComputerSearchScope): String {
            logger.debug { "Building filter for searching by '$searchBy' for '$searchValue'..." }

            val filter = when (searchBy) {
                ComputerSearchScope.Name -> "$baseFilter(name=$searchValue))"
            }

            logger.debug { "Built filter for searching by '$searchBy' for '$searchValue': $filter" }
            return filter
        }

        /**
         * Create a domain DN from domain controller name
         *
         * @param domain a [java.lang.String] object - name of the domain controller (e.g. debuglevel.de)
         * @return a [java.lang.String] object - domain DN for domain (eg. DC=debuglevel,DC=de)
         */
        fun getBaseDN(domain: String): String {
            logger.debug { "Build base DN for domain '$domain'..." }

            val dn = domain
                .toUpperCase()
                .split('.')
                .map { "DC=$it" }
                .joinToString(",")

            logger.debug { "Built base DN for domain '$domain': '$dn'" }
            return dn
        }
    }

    class ConnectionException(e: Exception) : Exception("Could not connect to LDAP server", e)
    class MoreThanOneResultException(computers: List<Computer>) : Exception("Found more than one result: $computers")
    class NoUserFoundException : Exception("No such computer found")
}
