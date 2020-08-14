package de.debuglevel.activedirectory.computer

import java.util.*

data class ComputerResponse(
    val cn: String? = null,
    val disabled: Boolean? = null,
    val logonCount: Int? = null,
    val operatingSystem: String? = null,
    val operatingSystemVersion: String? = null,
    val guid: UUID? = null,
    val lastLogon: String? = null,
    val whenCreated: String? = null,
    val error: String? = null
) {
    constructor(computer: Computer) : this(
        computer.cn,
        computer.disabled,
        computer.logonCount,
        computer.operatingSystem,
        computer.operatingSystemVersion,
        computer.guid,
        computer.lastLogonFormatted,
        computer.whenCreatedFormatted
    )

}