package de.debuglevel.activedirectory

import de.debuglevel.activedirectory.computer.Computer
import de.debuglevel.activedirectory.computer.ComputerActiveDirectoryService
import de.debuglevel.activedirectory.computer.ComputerSearchScope
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

    fun validUserFilterProvider() = Stream.of(
        UserFilterTestData(
            value = "my@mail.example",
            by = UserSearchScope.Email,
            expectedFilter = "(&((&(objectCategory=Person)(objectClass=User)))(mail=my@mail.example))"
        ),
        UserFilterTestData(
            value = "myAccountName",
            by = UserSearchScope.Username,
            expectedFilter = "(&((&(objectCategory=Person)(objectClass=User)))(samaccountname=myAccountName))"
        )
    )

    data class ComputerFilterTestData(
        val value: String,
        val by: ComputerSearchScope,
        val expectedFilter: String? = null
    )

    fun validComputerFilterProvider() = Stream.of(
        ComputerFilterTestData(
            value = "Laptop",
            by = ComputerSearchScope.Name,
            expectedFilter = "(&((&(objectCategory=Computer)(objectClass=Computer)))(name=Laptop))"
        )
    )

    data class UserFilterTestData(
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

    fun validComputerSearchProvider() = Stream.of(
        ComputerTestData(
            value = "Desktop",
            searchScope = ComputerSearchScope.Name,
            computer = Computer(
                cn = "Desktop",
                logonCount = 100,
                operatingSystem = "SuperOS 4.2",
                operatingSystemVersion = "4.2"
            )
        ),
        ComputerTestData(
            value = "Laptop",
            searchScope = ComputerSearchScope.Name,
            computer = Computer(
                cn = "Laptop",
                logonCount = 100,
                operatingSystem = "SuperOS 4.2",
                operatingSystemVersion = "4.2"
            )
        )
    )

    fun invalidComputerSearchProvider() = Stream.of(
        ComputerTestData(
            value = "Amiga",
            searchScope = ComputerSearchScope.Name
        )
    )

    data class ComputerTestData(
        val value: String,
        val searchScope: ComputerSearchScope,
        val computer: Computer? = null
    )

    fun `set up userActiveDirectoryService mock`(activeDirectoryServiceMock: UserActiveDirectoryService) {
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

    fun `set up computerActiveDirectoryService mock`(activeDirectoryServiceMock: ComputerActiveDirectoryService) {
        // getUser(searchValue, searchBy)
        run {
            for (computerTestData in validComputerSearchProvider()) {
                Mockito.`when`(activeDirectoryServiceMock.get(computerTestData.value, ComputerSearchScope.Name))
                    .then { invocation -> computerTestData.computer }

                // check that mock works
                val resultData =
                    activeDirectoryServiceMock.get(computerTestData.value, ComputerSearchScope.Name)
                Assertions.assertThat(resultData).isEqualTo(computerTestData.computer)
                //verify(dataService)?.fetchData(ISBN(bookData.isbn))
            }
        }

        // getUsers()
        run {
            val computers = validComputerSearchProvider().map { it.computer }.toList()

            Mockito.`when`(activeDirectoryServiceMock.getAll())
                .then { invocation -> computers }

            // check that mock works
            val resultData = activeDirectoryServiceMock.getAll()
            Assertions.assertThat(resultData).contains(*computers.toTypedArray())
        }
    }
}