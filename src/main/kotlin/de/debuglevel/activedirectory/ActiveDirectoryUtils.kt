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

    fun SearchResult.getAttributeValue(attributeName: String): String? {
        logger.trace { "Getting attribute '$attributeName' value..." }
        val attribute = attributes.get(attributeName)
        logger.trace { "Got attribute '$attributeName': $attribute" }
        val value = attribute?.toString()?.substringAfter(": ")
        logger.trace { "Got attribute '$attributeName' value: $value" }
        return value
    }

    fun toUUID(attributeValue: String?): UUID? {
        logger.debug { "Converting '$attributeValue' to UUID..." }

        if (attributeValue.isNullOrBlank()) {
            return null
        }

        val string = attributeValue
        logger.trace { "String: $string" }
        val bytes = string.toByteArray(Charsets.US_ASCII)
        logger.debug { "Bytes: ${bytesToHexString(bytes)} (${bytes.size})" } // BUG: sometimes it's not 16 bytes
        val uuid = UUIDUtils.bytesToUUID(bytes)
        logger.trace { "UUID: $uuid" }

        logger.debug { "Converted $attributeValue to UUID: $uuid" }
        return uuid
    }

    private fun bytesToHexString(bytes: ByteArray): String {
        return bytes.joinToString(" ") { String.format("%02X", it) }
    }

    class ConnectionException(e: Exception) : Exception("Could not connect to LDAP server", e)
}