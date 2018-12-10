package de.debuglevel.activedirectory.domain.activedirectory

data class User(val username: String,
                val givenname: String?,
                val mail: String?,
                val cn: String?)