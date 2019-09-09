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
class ActiveDirectoryService(
    @Property(name = "app.activedirectory.username") private val username: String,
    @Property(name = "app.activedirectory.password") private val password: String,
    @Property(name = "app.activedirectory.server") private val domainController: String,
    @Property(name = "app.activedirectory.searchbase") private val searchBase: String
) {
    private val logger = KotlinLogging.logger {}

    private val searchControls: SearchControls
    private val returnAttributes =
        arrayOf(
            "sAMAccountName",
            "givenName",
            "sn",
            "cn",
            "mail",
            "displayName",
            "userAccountControl",
            "lastLogon",
            "whenCreated"
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
    fun getUsers(searchValue: String, searchBy: SearchScope): List<User> {
        val users = ArrayList<User>()

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
                    val user = buildUser(result)
                    users.add(user)
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

                logger.debug { "Fetched a total of ${users.count()} entries." }
            } while (cookie != null)

            closeLdapContext(ldapContext)
        } catch (e: NamingException) {
            logger.error(e) { "PagedSearch failed." }
        } catch (e: IOException) {
            logger.error(e) { "PagedSearch failed." }
        } catch (e: Exception) {
            logger.error(e) { "PagedSearch failed." }
        }

        return users
    }

    /**
     * Gets all users from the Active Directory for given search base
     *
     * @return search result a [javax.naming.NamingEnumeration] object - active directory search result
     * @throws NamingException
     */
    fun getUsers(): List<User> {
        //TODO("Does not work with more then 1000 users due to missing paging")
        logger.debug { "Getting all users..." }

        val users = getUsers("*", SearchScope.Username)

        logger.debug { "Got ${users.count()} users..." }
        return users
    }

    /**
     * Gets an user from the Active Directory by username/email id for given search base
     *
     * @param searchValue a [java.lang.String] object - search value used for AD search for eg. username or email
     * @param searchBy a [java.lang.String] object - scope of search by username or by email id
     * @return search result a [javax.naming.NamingEnumeration] object - active directory search result
     * @throws NamingException
     */
    fun getUser(searchValue: String, searchBy: SearchScope): User {
        logger.debug { "Getting user '$searchValue' by $searchBy..." }

        val users = getUsers(searchValue, searchBy)

        if (users.count() > 1) {
            throw MoreThanOneResultException(users)
        } else if (users.isEmpty()) {
            throw NoUserFoundException()
        }

        val user = users.first()
        return user
    }

    private fun buildUsers(results: NamingEnumeration<SearchResult>): List<User> {
        logger.debug { "Building users from search results..." }

        val users = results.asSequence()
            .map {
                buildUser(it)
            }
            .onEach { logger.debug { "Got user $it" } }
            .toList()

        logger.debug { "Built ${users.count()} users." }

        return users
    }

    private fun buildUser(it: SearchResult): User {
        val samaaccountname = it.attributes.get("samaccountname").toString().substringAfter(": ")
        val givenname = it.attributes.get("givenname")?.toString()?.substringAfter(": ")
        val mailaddress = it.attributes.get("mail")?.toString()?.substringAfter(": ")
        val commonName = it.attributes.get("cn")?.toString()?.substringAfter(": ")
        val surname = it.attributes.get("sn")?.toString()?.substringAfter(": ")
        val displayName = it.attributes.get("displayName")?.toString()?.substringAfter(": ")
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

        return User(
            samaaccountname,
            givenname,
            mailaddress,
            commonName,
            surname,
            displayName,
            userAccountControl,
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

        private const val baseFilter = "(&((&(objectCategory=Person)(objectClass=User)))"

        /**
         * Active Directory filter string value
         *
         * @param searchValue a [java.lang.String] object - search value of username/email id for active directory
         * @param searchBy a [java.lang.String] object - scope of search by username or email id
         * @return a [java.lang.String] object - filter string
         */
        fun getFilter(searchValue: String, searchBy: SearchScope): String {
            logger.debug { "Building filter for searching by '$searchBy' for '$searchValue'..." }

            val filter = when (searchBy) {
                SearchScope.Email -> "$baseFilter(mail=$searchValue))"
                SearchScope.Username -> "$baseFilter(samaccountname=$searchValue))"
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
    class MoreThanOneResultException(users: List<User>) : Exception("Found more than one result: $users")
    class NoUserFoundException : Exception("No such user found")
}
