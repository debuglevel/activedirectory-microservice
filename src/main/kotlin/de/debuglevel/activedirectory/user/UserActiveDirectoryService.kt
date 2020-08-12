package de.debuglevel.activedirectory.user

import de.debuglevel.activedirectory.ActiveDirectorySearchScope
import de.debuglevel.activedirectory.ActiveDirectoryService
import de.debuglevel.activedirectory.ActiveDirectoryUtils
import de.debuglevel.activedirectory.ActiveDirectoryUtils.convertLdapTimestampToDate
import de.debuglevel.activedirectory.ActiveDirectoryUtils.getAttributeValue
import de.debuglevel.activedirectory.EntityActiveDirectoryService
import mu.KotlinLogging
import javax.inject.Singleton
import javax.naming.directory.SearchResult

// see original at https://myjeeva.com/querying-active-directory-using-java.html
@Singleton
class UserActiveDirectoryService(
    private val activeDirectoryService: ActiveDirectoryService
) : EntityActiveDirectoryService<User> {
    private val logger = KotlinLogging.logger {}

    override val baseFilter = "(&(objectCategory=Person)(objectClass=User))"

    override val entityName = "user"

    override val returnAttributes =
        arrayOf(
            "sAMAccountName",
            "givenName",
            "sn",
            "cn",
            "mail",
            "displayName",
            "userAccountControl",
            "lastLogon",
            "whenCreated",
            "objectGUID"
        )

    override fun getAll(): List<User> {
        logger.debug { "Getting all users..." }

        val computers = getAll("*", UserSearchScope.Username)

        logger.debug { "Got ${computers.count()} users" }
        return computers
    }

    override fun getAll(searchValue: String, searchBy: ActiveDirectorySearchScope): List<User> {
        logger.debug { "Getting all users with $searchBy='$searchValue'..." }

        val filter = buildFilter(searchValue, searchBy)
        val searchResults = activeDirectoryService.getAll(filter, returnAttributes)
        val users = searchResults.map { build(it) }

        logger.debug { "Got ${users.count()} users with $searchBy='$searchValue'" }
        return users
    }

    override fun get(searchValue: String, searchBy: ActiveDirectorySearchScope): User {
        logger.debug { "Getting user $searchBy='$searchValue'..." }

        val filter = buildFilter(searchValue, searchBy)
        val searchResult = activeDirectoryService.get(filter, returnAttributes)
        val user = build(searchResult)

        logger.debug { "Got user $searchBy='$searchValue': $user" }
        return user
    }

    override fun build(it: SearchResult): User {
        val samaaccountname = it.getAttributeValue("samaccountname")!!
        val givenname = it.getAttributeValue("givenname")
        val mailaddress = it.getAttributeValue("mail")
        val commonName = it.getAttributeValue("cn")
        val surname = it.getAttributeValue("sn")
        val displayName = it.getAttributeValue("displayName")
        val userAccountControl = it.getAttributeValue("userAccountControl")?.toIntOrNull()
        val guid = ActiveDirectoryUtils.toUUID(it.getAttributeValue("objectGUID"))
        val lastLogon = {
            val lastLogonTimestamp = it.getAttributeValue("lastLogon")?.toLong()
            if (lastLogonTimestamp != null && lastLogonTimestamp != 0L) {
                convertLdapTimestampToDate(lastLogonTimestamp)
            } else {
                null
            }
        }()
        val whenCreated = {
            val whenCreatedTimestamp = it.getAttributeValue("whenCreated")
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
            whenCreated,
            guid
        )
    }

    override fun buildFilter(searchValue: String, searchBy: ActiveDirectorySearchScope): String {
        logger.debug { "Building filter for searching by '$searchBy' for '$searchValue'..." }

        val filter = when (searchBy) {
            UserSearchScope.Email -> "(&($baseFilter)(mail=$searchValue))"
            UserSearchScope.Username -> "(&($baseFilter)(samaccountname=$searchValue))"
            else -> throw EntityActiveDirectoryService.InvalidSearchScope("UserSearchScope")
        }

        logger.debug { "Built filter for searching by '$searchBy' for '$searchValue': $filter" }
        return filter
    }
}
