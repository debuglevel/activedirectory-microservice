package de.debuglevel.activedirectory.domain.activedirectory

data class User(val username: String,
                val givenname: String? = null,
                val mail: String? = null,
                val cn: String? = null,
                val sn: String? = null,
                val displayName: String? = null,
                val userAccountControl: Int? = null
) {
    /**
     * Account is disabled if bitwise 2 is set
     */
    val disabled: Boolean
        get() = ((userAccountControl?.and(2)) ?: 0) > 0
}