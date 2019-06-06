package de.debuglevel.activedirectory.rest.user

import de.debuglevel.activedirectory.domain.activedirectory.User

data class UserDTO(val username: String,
                   val givenname: String?,
                   val mail: String?,
                   val cn: String?,
                   val sn: String?,
                   val displayName: String?,
                   val disabled: Boolean?,
                   val lastLogon: String?
) {
    constructor(user: User) : this(
        user.username,
        user.givenname,
        user.mail,
        user.cn,
        user.sn,
        user.displayName,
        user.disabled,
        user.lastLogonFormatted
    )

}