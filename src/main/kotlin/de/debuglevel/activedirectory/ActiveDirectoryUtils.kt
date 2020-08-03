package de.debuglevel.activedirectory

import mu.KotlinLogging
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import javax.naming.Context
import javax.naming.directory.SearchResult
import javax.naming.ldap.InitialLdapContext
import javax.naming.ldap.LdapContext

object ActiveDirectoryUtils {
    private val logger = KotlinLogging.logger {}

    fun convertLdapTimestampToDate(timestamp: Long): GregorianCalendar {
        logger.trace { "Converting LDAP timestamp '$timestamp' to date..." }
        // TODO: unknown if this respects time zones
        val fileTime = timestamp / 10000L - +11644473600000L
        val date = Date(fileTime)
        val calendar = GregorianCalendar()
        calendar.time = date

        logger.trace { "Converted LDAP timestamp '$timestamp' to date: $calendar" }
        return calendar
    }

    fun convertLdapTimestampToDate(timestamp: String): GregorianCalendar {
        logger.trace { "Converting LDAP timestamp '$timestamp' to date..." }
        // TODO: unknown if this respects time zones
        val dateTimeFormatter = DateTimeFormatter.ofPattern("uuuuMMddHHmmss[,S][.S]X")
        val zonedDateTime = OffsetDateTime.parse(timestamp, dateTimeFormatter).toZonedDateTime()
        val calendar = GregorianCalendar.from(zonedDateTime)

        logger.trace { "Converted LDAP timestamp '$timestamp' to date: $calendar" }
        return calendar
    }

    /**
     * Create a domain DN from domain controller name
     *
     * @param domain a [java.lang.String] object - name of the domain controller (e.g. debuglevel.de)
     * @return a [java.lang.String] object - domain DN for domain (eg. DC=debuglevel,DC=de)
     */
    fun getBaseDN(domain: String): String {
        logger.debug { "Building base DN for domain '$domain'..." }

        val dn = domain
            .toUpperCase()
            .split('.')
            .joinToString(",") { "DC=$it" }

        logger.debug { "Built base DN for domain '$domain': '$dn'" }
        return dn
    }

    fun createLdapContext(domainController: String, username: String, password: String): LdapContext {
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

    fun SearchResult.getAttributeValue(attributeName: String) =
        attributes.get(attributeName)?.toString()?.substringAfter(": ")

    class ConnectionException(e: Exception) : Exception("Could not connect to LDAP server", e)
}