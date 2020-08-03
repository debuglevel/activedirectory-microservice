package de.debuglevel.activedirectory

import mu.KotlinLogging
import java.io.IOException
import javax.naming.NamingException
import javax.naming.directory.SearchControls
import javax.naming.directory.SearchResult
import javax.naming.ldap.Control
import javax.naming.ldap.LdapContext
import javax.naming.ldap.PagedResultsControl
import javax.naming.ldap.PagedResultsResponseControl

abstract class ActiveDirectoryService<T : ActiveDirectoryEntity>(
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
    protected abstract fun build(it: SearchResult): T

    fun getAll(searchValue: String, searchBy: ActiveDirectorySearchScope): List<T> {
        val items = ArrayList<T>()

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
                    val item = build(result)
                    items.add(item)
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

                logger.debug { "Fetched a total of ${items.count()} entries." }
            } while (cookie != null)

            closeLdapContext(ldapContext)
        } catch (e: NamingException) {
            logger.error(e) { "PagedSearch failed." }
        } catch (e: IOException) {
            logger.error(e) { "PagedSearch failed." }
        } catch (e: Exception) {
            logger.error(e) { "PagedSearch failed." }
        }

        return items
    }

    /**
     * Gets all computers from the Active Directory for given search base
     *
     * @return search result a [javax.naming.NamingEnumeration] object - active directory search result
     * @throws NamingException
     */
    abstract fun getAll(): List<T>

    /**
     * Gets an user from the Active Directory by username/email id for given search base
     *
     * @param searchValue a [java.lang.String] object - search value used for AD search for eg. username or email
     * @param searchBy a [java.lang.String] object - scope of search by username or by email id
     * @return search result a [javax.naming.NamingEnumeration] object - active directory search result
     * @throws NamingException
     */
    abstract fun get(searchValue: String, searchBy: ActiveDirectorySearchScope): T

    class MoreThanOneResultException(items: List<ActiveDirectoryEntity>) :
        Exception("Found more than one result: $items")

    class NoItemFoundException : Exception("No such item found")

    class InvalidSearchScope(correct: String) : Exception("Invalid SearchScope, use $correct instead.")
}