package de.debuglevel.activedirectory.user

import de.debuglevel.activedirectory.ActiveDirectorySearchScope
import de.debuglevel.activedirectory.ActiveDirectoryService
import de.debuglevel.activedirectory.ActiveDirectoryUtils.convertLdapTimestampToDate
import de.debuglevel.activedirectory.ActiveDirectoryUtils.getAttributeValue
import io.micronaut.context.annotation.Property
import mu.KotlinLogging
import javax.inject.Singleton
import javax.naming.directory.SearchResult

// see original at https://myjeeva.com/querying-active-directory-using-java.html
@Singleton
class UserActiveDirectoryService(
    @Property(name = "app.activedirectory.username") username: String,
    @Property(name = "app.activedirectory.password") password: String,
    @Property(name = "app.activedirectory.server") domainController: String,
    @Property(name = "app.activedirectory.searchbase") searchBase: String
) : ActiveDirectoryService<User>(
    username,
    password,
    domainController,
    searchBase
) {
    private val logger = KotlinLogging.logger {}

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
            "whenCreated"
        )

    override fun getAll(): List<User> {
        logger.debug { "Getting all users..." }

        val users = getAll("*", UserSearchScope.Username)

        logger.debug { "Got ${users.count()} users..." }
        return users
    }

    override fun build(it: SearchResult): User {
        val samaaccountname = it.getAttributeValue("samaccountname")!!
        val givenname = it.getAttributeValue("givenname")
        val mailaddress = it.getAttributeValue("mail")
        val commonName = it.getAttributeValue("cn")
        val surname = it.getAttributeValue("sn")
        val displayName = it.getAttributeValue("displayName")
        val userAccountControl = it.getAttributeValue("userAccountControl")?.toIntOrNull()
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
            whenCreated
        )
    }

    override fun buildFilter(searchValue: String, searchBy: ActiveDirectorySearchScope): String {
        logger.debug { "Building filter for searching by '$searchBy' for '$searchValue'..." }

        val filter = when (searchBy) {
            UserSearchScope.Email -> "(&($baseFilter)(mail=$searchValue))"
            UserSearchScope.Username -> "(&($baseFilter)(samaccountname=$searchValue))"
            else -> throw InvalidSearchScope("UserSearchScope")
        }

        logger.debug { "Built filter for searching by '$searchBy' for '$searchValue': $filter" }
        return filter
    }

    override val baseFilter = "(&(objectCategory=Person)(objectClass=User))"

    override val entityName = "user"
}
