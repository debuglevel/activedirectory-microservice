package de.debuglevel.activedirectory.rest.user

data class UserDTO(val username: String,
                   val givenname: String?,
                   val mail: String?,
                   val cn: String?)