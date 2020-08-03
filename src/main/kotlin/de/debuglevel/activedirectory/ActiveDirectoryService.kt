package de.debuglevel.activedirectory

import mu.KotlinLogging
import javax.naming.NamingException
import javax.naming.directory.SearchControls
import javax.naming.directory.SearchResult
import javax.naming.ldap.LdapContext

abstract class ActiveDirectoryService<T>(
    protected val username: String,
    protected val password: String,
    protected val domainController: String,
    protected val searchBase: String
) {
    private val logger = KotlinLogging.logger {}

    protected val pageSize = 1000

    /**
     * Attributes which should be returned on a query
     */
    protected abstract val returnAttributes: Array<String>

    /**
     * LDAP filter to match certain group of objects
     */
    protected abstract val baseFilter: String

    protected val searchControls: SearchControls = SearchControls()

    init {
        // initializing search controls
        searchControls.searchScope = SearchControls.SUBTREE_SCOPE
        searchControls.returningAttributes = returnAttributes
    }

    fun createLdapContext(): LdapContext {
        return ActiveDirectoryUtils.createLdapContext(domainController, username, password)
    }

    /**
     * Closes the LDAP connection with Domain controller
     */
    protected fun closeLdapContext(ldapContext: LdapContext) {
        try {
            logger.debug { "Closing LDAP connection..." }
            ldapContext.close()
        } catch (e: NamingException) {
            logger.error(e) { "Failed closing LDAP connection." }
        }
    }

    /**
     * Active Directory filter string value
     *
     * @param searchValue a [java.lang.String] object - search value of username/email id for active directory
     * @param searchBy a [java.lang.String] object - scope of search by username or email id
     * @return a [java.lang.String] object - filter string
     */
    abstract fun buildFilter(searchValue: String, searchBy: ActiveDirectorySearchScope): String

    /**
     * Builds an entity object from the LDAP search result.
     */
    protected abstract fun build(it: SearchResult): ActiveDirectoryEntity

    /**
     * Gets users from the Active Directory by username/email id for given search base
     *
     * @param searchValue a [java.lang.String] object - search value used for AD search for eg. username or email
     * @param searchBy a [java.lang.String] object - scope of search by username or by email id
     * @return search result a [javax.naming.NamingEnumeration] object - active directory search result
     * @throws NamingException
     */
    abstract fun getAll(searchValue: String, searchBy: ActiveDirectorySearchScope): List<ActiveDirectoryEntity>

    /**
     * Gets all computers from the Active Directory for given search base
     *
     * @return search result a [javax.naming.NamingEnumeration] object - active directory search result
     * @throws NamingException
     */
    abstract fun getAll(): List<ActiveDirectoryEntity>

    /**
     * Gets an user from the Active Directory by username/email id for given search base
     *
     * @param searchValue a [java.lang.String] object - search value used for AD search for eg. username or email
     * @param searchBy a [java.lang.String] object - scope of search by username or by email id
     * @return search result a [javax.naming.NamingEnumeration] object - active directory search result
     * @throws NamingException
     */
    abstract fun get(searchValue: String, searchBy: ActiveDirectorySearchScope): ActiveDirectoryEntity

    class MoreThanOneResultException(items: List<ActiveDirectoryEntity>) :
        Exception("Found more than one result: $items")

    class NoItemFoundException : Exception("No such item found")

    class InvalidSearchScope(correct: String) : Exception("Invalid SearchScope, use $correct instead.")
}