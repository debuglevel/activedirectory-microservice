package de.debuglevel.activedirectory.computer

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

    override fun getAll(): List<Computer> {
        logger.debug { "Getting all computers..." }

        val computers = getAll("*", ComputerSearchScope.Name)

        logger.debug { "Got ${computers.count()} computers..." }
        return computers
    }

    override fun build(it: SearchResult): Computer {
        val commonName = it.getAttributeValue("cn")
        val operatingSystem = it.getAttributeValue("operatingSystem")
        val operatingSystemVersion = it.getAttributeValue("operatingSystemVersion")
        val logonCount = it.getAttributeValue("logonCount")?.toInt()
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

    override fun buildFilter(searchValue: String, searchBy: ActiveDirectorySearchScope): String {
        logger.debug { "Building filter for searching by '$searchBy' for '$searchValue'..." }

        val filter = when (searchBy) {
            ComputerSearchScope.Name -> "(&($baseFilter)(name=$searchValue))"
            else -> throw InvalidSearchScope("ComputerSearchScope")
        }

        logger.debug { "Built filter for searching by '$searchBy' for '$searchValue': $filter" }
        return filter
    }

    override val baseFilter = "(&(objectCategory=Computer)(objectClass=Computer))"

    override val entityName = "computer"
}
