package de.debuglevel.activedirectory.user

import de.debuglevel.activedirectory.ActiveDirectoryEntity
import java.time.format.DateTimeFormatter
import java.util.*

data class User(
    val username: String,
    val givenname: String? = null,
    val mail: String? = null,
    val cn: String? = null,
    val sn: String? = null,
    val displayName: String? = null,
    val userAccountControl: Int? = null,
    val lastLogon: GregorianCalendar? = null,
    val whenCreated: GregorianCalendar? = null
) : ActiveDirectoryEntity {
    /**
     * Account is disabled if bitwise 2 is set
     */
    val disabled: Boolean
        get() = ((userAccountControl?.and(2)) ?: 0) > 0

    val lastLogonFormatted: String?
        get() = this.lastLogon?.toZonedDateTime()?.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))

    val whenCreatedFormatted: String?
        get() = this.whenCreated?.toZonedDateTime()?.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
}