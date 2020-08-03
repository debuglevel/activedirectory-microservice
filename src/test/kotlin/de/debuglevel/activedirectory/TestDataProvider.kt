package de.debuglevel.activedirectory

import de.debuglevel.activedirectory.user.User
import de.debuglevel.activedirectory.user.UserActiveDirectoryService
import de.debuglevel.activedirectory.user.UserSearchScope
import org.assertj.core.api.Assertions
import org.mockito.Mockito
import java.util.stream.Stream
import kotlin.streams.toList

object TestDataProvider {
    fun validDomainProvider() = Stream.of(
        DnTestData(
            value = "debuglevel.de",
            expectedDN = "DC=DEBUGLEVEL,DC=DE"
        ),
        DnTestData(
            value = "www.debuglevel.de",
            expectedDN = "DC=WWW,DC=DEBUGLEVEL,DC=DE"
        )
    )

    data class DnTestData(
        val value: String,
        val expectedDN: String? = null
    )

    fun validFilterProvider() = Stream.of(
        FilterTestData(
            value = "my@mail.example",
            by = UserSearchScope.Email,
            expectedFilter = "(&((&(objectCategory=Person)(objectClass=User)))(mail=my@mail.example))"
        ),
        FilterTestData(
            value = "myAccountName",
            by = UserSearchScope.Username,
            expectedFilter = "(&((&(objectCategory=Person)(objectClass=User)))(samaccountname=myAccountName))"
        )
    )

    data class FilterTestData(
        val value: String,
        val by: UserSearchScope,
        val expectedFilter: String? = null
    )

    fun validUserSearchProvider() = Stream.of(
        AccountTestData(
            value = "maxmustermann", searchScope = UserSearchScope.Username, user = User(
                "maxmustermann",
                "Max",
                "max@mustermann.de",
                "Max Mustermann"
            )
        ),
        AccountTestData(
            value = "max@mustermann.de", searchScope = UserSearchScope.Email, user = User(
                "maxmustermann",
                "Max",
                "max@mustermann.de",
                "Max Mustermann"
            )
        ),
        AccountTestData(
            value = "alexaloah", searchScope = UserSearchScope.Username, user = User(
                "alexaloah",
                "Alex",
                "alex@aloah.de",
                "Alex Aloah"
            )
        ),
        AccountTestData(
            value = "alex@aloah.de", searchScope = UserSearchScope.Email, user = User(
                "alexaloah",
                "Alex",
                "alex@aloah.de",
                "Alex Aloah"
            )
        )
    )

    fun invalidUserSearchProvider() = Stream.of(
        AccountTestData(
            value = "heinzstrunk",
            searchScope = UserSearchScope.Username
        ),
        AccountTestData(
            value = "heinz@strunk.de",
            searchScope = UserSearchScope.Email
        ),

        // search by wrong value/scope combination
        AccountTestData(
            value = "maxmustermann", searchScope = UserSearchScope.Email, user = User(
                "maxmustermann",
                "Max",
                "max@mustermann.de",
                "Max Mustermann"
            )
        ),
        AccountTestData(
            value = "max@mustermann.de", searchScope = UserSearchScope.Username, user = User(
                "maxmustermann",
                "Max",
                "max@mustermann.de",
                "Max Mustermann"
            )
        )
    )

    data class AccountTestData(
        val value: String,
        val searchScope: UserSearchScope,
        val user: User? = null
    )

    fun `set up activeDirectoryService mock`(activeDirectoryServiceMock: UserActiveDirectoryService) {
        // getUser(searchValue, searchBy)
        run {
            for (accountTestData in validUserSearchProvider()) {
                Mockito.`when`(activeDirectoryServiceMock.getUser(accountTestData.value, UserSearchScope.Username))
                    .then { invocation -> accountTestData.user }

                // check that mock works
                val resultData = activeDirectoryServiceMock.getUser(accountTestData.value, UserSearchScope.Username)
                Assertions.assertThat(resultData).isEqualTo(accountTestData.user)
                //verify(dataService)?.fetchData(ISBN(bookData.isbn))
            }
        }

        // getUsers()
        run {
            val users = validUserSearchProvider().map { it.user }.toList()

            Mockito.`when`(activeDirectoryServiceMock.getUsers())
                .then { invocation -> users }

            // check that mock works
            val resultData = activeDirectoryServiceMock.getUsers()
            Assertions.assertThat(resultData).contains(*users.toTypedArray())
        }
    }
}