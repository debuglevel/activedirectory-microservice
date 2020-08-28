package de.debuglevel.activedirectory.computer

import de.debuglevel.activedirectory.ActiveDirectorySearchScope
import de.debuglevel.activedirectory.ActiveDirectoryService
import de.debuglevel.activedirectory.ActiveDirectoryUtils.convertLdapTimestampToDate
import de.debuglevel.activedirectory.ActiveDirectoryUtils.getAttributeValue
import de.debuglevel.activedirectory.ActiveDirectoryUtils.getBinaryAttributeValue
import de.debuglevel.activedirectory.ActiveDirectoryUtils.getDate
import de.debuglevel.activedirectory.ActiveDirectoryUtils.getLastLogon
import de.debuglevel.activedirectory.ActiveDirectoryUtils.toUUID
import de.debuglevel.activedirectory.EntityActiveDirectoryService
import mu.KotlinLogging
import java.util.*
import javax.inject.Singleton
import javax.naming.directory.SearchResult

// see original at https://myjeeva.com/querying-active-directory-using-java.html
@Singleton
class ComputerActiveDirectoryService(
    private val activeDirectoryService: ActiveDirectoryService
) : EntityActiveDirectoryService<Computer> {
    private val logger = KotlinLogging.logger {}

    override val baseFilter = "(&(objectCategory=Computer)(objectClass=Computer))"

    override val entityName = "computer"

    override val returnAttributes =
        arrayOf(
            "cn",
            "userAccountControl",
            "lastLogon",
            "whenCreated",
            "logonCount",
            "operatingSystem",
            "operatingSystemVersion",
            "objectGUID"
        )

    override fun getAll(): List<Computer> {
        logger.debug { "Getting all computers..." }

        val computers = getAll("*", ComputerSearchScope.Name)

        logger.debug { "Got ${computers.count()} computers" }
        return computers
    }

    override fun getAll(searchValue: String, searchBy: ActiveDirectorySearchScope): List<Computer> {
        logger.debug { "Getting all computers with $searchBy='$searchValue'..." }

        val filter = buildFilter(searchValue, searchBy)
        val searchResults = activeDirectoryService.getAll(filter, returnAttributes)
        val computers = searchResults.map { build(it) }

        logger.debug { "Got ${computers.count()} computers with $searchBy='$searchValue'" }
        return computers
    }

    override fun get(searchValue: String, searchBy: ActiveDirectorySearchScope): Computer {
        logger.debug { "Getting computer $searchBy='$searchValue'..." }

        val filter = buildFilter(searchValue, searchBy)
        val searchResult = activeDirectoryService.get(filter, returnAttributes)
        val computer = build(searchResult)

        logger.debug { "Got computer $searchBy='$searchValue': $computer" }
        return computer
    }

    override fun build(it: SearchResult): Computer {
        val commonName = it.getAttributeValue("cn")
        val operatingSystem = it.getAttributeValue("operatingSystem")
        val operatingSystemVersion = it.getAttributeValue("operatingSystemVersion")
        val guid = toUUID(it.getBinaryAttributeValue("objectGUID"))
        val logonCount = it.getAttributeValue("logonCount")?.toInt()
        val userAccountControl = it.getAttributeValue("userAccountControl")?.toIntOrNull()
        val lastLogon = getLastLogon(it)
        val whenCreated = getDate(it, "whenCreated")

        return Computer(
            commonName,
            userAccountControl,
            logonCount,
            operatingSystem,
            operatingSystemVersion,
            guid,
            lastLogon,
            whenCreated
        )
    }

    override fun buildFilter(searchValue: String, searchBy: ActiveDirectorySearchScope): String {
        logger.debug { "Building filter for searching by '$searchBy' for '$searchValue'..." }

        val filter = when (searchBy) {
            ComputerSearchScope.Name -> "(&($baseFilter)(name=$searchValue))"
            else -> throw EntityActiveDirectoryService.InvalidSearchScope("ComputerSearchScope")
        }

        logger.debug { "Built filter for searching by '$searchBy' for '$searchValue': $filter" }
        return filter
    }
}
