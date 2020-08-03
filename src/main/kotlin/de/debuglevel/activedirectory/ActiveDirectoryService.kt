package de.debuglevel.activedirectory

import io.micronaut.context.annotation.Property
import mu.KotlinLogging
import java.io.IOException
import javax.inject.Singleton
import javax.naming.NamingException
import javax.naming.directory.SearchControls
import javax.naming.directory.SearchResult
import javax.naming.ldap.Control
import javax.naming.ldap.LdapContext
import javax.naming.ldap.PagedResultsControl
import javax.naming.ldap.PagedResultsResponseControl

@Singleton
class ActiveDirectoryService(
    @Property(name = "app.activedirectory.username") private val username: String,
    @Property(name = "app.activedirectory.password") private val password: String,
    @Property(name = "app.activedirectory.server") private val domainController: String,
    @Property(name = "app.activedirectory.searchbase") private val searchBase: String
) {
    private val logger = KotlinLogging.logger {}

    protected val pageSize = 1000

    private fun buildSearchControls(returnAttributes: Array<String>): SearchControls {
        return SearchControls().apply {
            searchScope = SearchControls.SUBTREE_SCOPE
            returningAttributes = returnAttributes
        }
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

    fun getAll(filter: String, returnAttributes: Array<String>): List<SearchResult> {
        logger.debug { "Getting all items of filter='$filter'..." }

        val searchResults = mutableListOf<SearchResult>()
        val searchControls = buildSearchControls(returnAttributes)

        try {
            val ldapContext = createLdapContext()

            // Activate paged results
            var cookie: ByteArray? = null
            ldapContext.requestControls = arrayOf<Control>(PagedResultsControl(pageSize, Control.NONCRITICAL))

            do {
                logger.debug { "Requesting page..." }

                val results = ldapContext.search(searchBase, filter, searchControls)

                while (results.hasMoreElements()) {
                    val searchResult = results.nextElement()
                    searchResults.add(searchResult)
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

                logger.debug { "Fetched a total of ${searchResults.count()} entries." }
            } while (cookie != null)

            closeLdapContext(ldapContext)
        } catch (e: NamingException) {
            logger.error(e) { "PagedSearch failed." }
        } catch (e: IOException) {
            logger.error(e) { "PagedSearch failed." }
        } catch (e: Exception) {
            logger.error(e) { "PagedSearch failed." }
        }

        logger.debug { "Got ${searchResults.count()} items of filter='$filter'" }
        return searchResults
    }

    fun get(filter: String, returnAttributes: Array<String>): SearchResult {
        logger.debug { "Getting item with filter='$filter'..." }

        val searchResults = getAll(filter, returnAttributes)

        if (searchResults.count() > 1) {
            throw MoreThanOneResultException(searchResults)
        } else if (searchResults.isEmpty()) {
            throw NoItemFoundException()
        }

        val searchResult = searchResults.first()

        logger.debug { "Got item with filter='$filter: $searchResult" }
        return searchResult
    }

    class MoreThanOneResultException(items: List<SearchResult>) :
        Exception("Found more than one result: $items")

    class NoItemFoundException : Exception("No such item found")
}