package de.debuglevel.activedirectory.domain.activedirectory

import mu.KotlinLogging
import java.io.Closeable
import java.util.*
import javax.naming.Context
import javax.naming.NamingEnumeration
import javax.naming.NamingException
import javax.naming.directory.DirContext
import javax.naming.directory.InitialDirContext
import javax.naming.directory.SearchControls
import javax.naming.directory.SearchResult

// see original at https://myjeeva.com/querying-active-directory-using-java.html
class ActiveDirectory(username: String, password: String, private val domainController: String) : Closeable {
    private val logger = KotlinLogging.logger {}

    private var dirContext: DirContext
    private val searchControls: SearchControls
    private val returnAttributes = arrayOf("sAMAccountName", "givenName", "cn", "mail")

    init {
        val properties: Properties = Properties()
        properties[Context.INITIAL_CONTEXT_FACTORY] = "com.sun.jndi.ldap.LdapCtxFactory"
        properties[Context.PROVIDER_URL] = "LDAP://$domainController"
        properties[Context.SECURITY_PRINCIPAL] = "$username@$domainController"
        properties[Context.SECURITY_CREDENTIALS] = password

        // initializing Active Directory LDAP connection
        try {
            logger.debug { "Initialize LDAP connection..." }
            dirContext = InitialDirContext(properties)
        } catch (e: Exception) {
            logger.error(e) { "Failed initializing LDAP connection." }
            throw ConnectionException(e)
        }

        // initializing search controls
        searchControls = SearchControls()
        searchControls.searchScope = SearchControls.SUBTREE_SCOPE
        searchControls.returningAttributes = returnAttributes
    }

    /**
     * Search the Active Directory by username/email id for given search base
     *
     * @param searchValue a [java.lang.String] object - search value used for AD search for eg. username or email
     * @param searchBy a [java.lang.String] object - scope of search by username or by email id
     * @param searchBase a [java.lang.String] object - search base value for scope tree for eg. DC=debuglevel,DC=de
     * @return search result a [javax.naming.NamingEnumeration] object - active directory search result
     * @throws NamingException
     */
    private fun searchUser(searchValue: String, searchBy: SearchScope, searchBase: String? = null): NamingEnumeration<SearchResult> {
        logger.debug { "Searching for user '$searchValue' by $searchBy..." }

        val filter = getFilter(searchValue, searchBy)
        val base = getBaseDN(searchBase ?: this.domainController)

        return try {
            this.dirContext.search(base, filter, this.searchControls)
        } catch (e: NamingException) {
            logger.error(e) { "Failed searching for user." }
            throw e
        }
    }

    /**
     * Gets an user from the Active Directory by username/email id for given search base
     *
     * @param searchValue a [java.lang.String] object - search value used for AD search for eg. username or email
     * @param searchBy a [java.lang.String] object - scope of search by username or by email id
     * @param searchBase a [java.lang.String] object - search base value for scope tree for eg. DC=debuglevel,DC=de
     * @return search result a [javax.naming.NamingEnumeration] object - active directory search result
     * @throws NamingException
     */
    fun getUsers(searchValue: String, searchBy: SearchScope, searchBase: String? = null): List<User> {
        logger.debug { "Getting user '$searchValue' by $searchBy..." }

        val results = searchUser(searchValue, searchBy, searchBase)
        val users = buildUsers(results)

        logger.debug { "Got ${users.count()} users for '$searchValue' by $searchBy..." }
        return users
    }

    private fun buildUsers(results: NamingEnumeration<SearchResult>): List<User> {
        logger.debug { "Building users from search results..." }

        return results.asSequence()
                .map {
                    val samaaccountname = it.attributes.get("samaccountname").toString().substringAfter(':')
                    val givenname = it.attributes.get("givenname").toString().substringAfter(':')
                    val mail = it.attributes.get("mail").toString().substringAfter(':')
                    val cn = it.attributes.get("cn").toString().substringAfter(':')

                    User(
                            samaaccountname,
                            givenname,
                            mail,
                            cn)
                }.toList()
    }

    /**
     * Closes the LDAP connection with Domain controller
     */
    override fun close() {
        try {
            logger.debug { "Closing LDAP connection..." }
            dirContext.close()
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
}

class ConnectionException(e: Exception) : Exception("Could not connect to LDAP server", e)
