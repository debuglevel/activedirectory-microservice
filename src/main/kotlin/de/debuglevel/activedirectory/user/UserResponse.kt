package de.debuglevel.activedirectory.user

import java.util.*

data class UserResponse(
    val username: String? = null,
    val givenname: String? = null,
    val mail: String? = null,
    val cn: String? = null,
    val sn: String? = null,
    val displayName: String? = null,
    val disabled: Boolean? = null,
    val lastLogon: String? = null,
    val guid: UUID? = null,
    val whenCreated: String? = null,
    val error: String? = null
) {
    constructor(user: User) : this(
        user.username,
        user.givenname,
        user.mail,
        user.cn,
        user.sn,
        user.displayName,
        user.disabled,
        user.lastLogonFormatted,
        user.guid,
        user.whenCreatedFormatted
    )

}