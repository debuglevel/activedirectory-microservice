package de.debuglevel.activedirectory

import javax.naming.NamingException
import javax.naming.directory.SearchResult

interface EntityActiveDirectoryService<T> {
    /**
     * Attributes which should be returned on a query
     */
    val returnAttributes: Array<String>

    /**
     * LDAP filter to match certain group of objects
     */
    val baseFilter: String

    /**
     * Name of the entity
     */
    val entityName: String

    /**
     * Active Directory filter string value
     *
     * @param searchValue a [java.lang.String] object - search value of username/email id for active directory
     * @param searchBy a [java.lang.String] object - scope of search by username or email id
     * @return a [java.lang.String] object - filter string
     */
    fun buildFilter(searchValue: String, searchBy: ActiveDirectorySearchScope): String

    /**
     * Builds an entity object from the LDAP search result.
     */
    fun build(it: SearchResult): T

    /**
     * Gets all computers from the Active Directory for given search base
     *
     * @return search result a [javax.naming.NamingEnumeration] object - active directory search result
     * @throws NamingException
     */
    fun getAll(): List<T>

    fun getAll(searchValue: String, searchBy: ActiveDirectorySearchScope): List<T>

    fun get(searchValue: String, searchBy: ActiveDirectorySearchScope): T

    class InvalidSearchScope(correct: String) : Exception("Invalid SearchScope, use $correct instead.")
}