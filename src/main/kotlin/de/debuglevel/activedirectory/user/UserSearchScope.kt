package de.debuglevel.activedirectory.user

import de.debuglevel.activedirectory.ActiveDirectorySearchScope

enum class UserSearchScope : ActiveDirectorySearchScope {
    Email,
    Username
}