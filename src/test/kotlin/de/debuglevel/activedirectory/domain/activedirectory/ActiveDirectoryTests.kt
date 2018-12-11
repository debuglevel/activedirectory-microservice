package de.debuglevel.activedirectory.domain.activedirectory

import com.github.trevershick.test.ldap.LdapServerResource
import com.github.trevershick.test.ldap.annotations.LdapAttribute
import com.github.trevershick.test.ldap.annotations.LdapConfiguration
import com.github.trevershick.test.ldap.annotations.LdapEntry
import mu.KotlinLogging
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

@LdapConfiguration
(
        bindDn = "cn=admin",
        password = "password",
        port = 10389,
//        base = LdapEntry
//        (
//                dn = "dc=root",
//                objectclass = arrayOf("top", "domain")
//        ),
        entries = arrayOf
        (
                LdapEntry
                (
                        dn = "cn=Max Mustermann,dc=root",
                        objectclass = arrayOf("User"),
                        attributes = arrayOf
                        (
                                LdapAttribute(name = "objectCategory", value = arrayOf("Person")),
                                LdapAttribute(name = "samaccountname", value = arrayOf("maxmustermann")),
                                LdapAttribute(name = "givenname", value = arrayOf("Max")),
                                LdapAttribute(name = "mail", value = arrayOf("max@mustermann.de"))
                        )
                )
        ),
        // bypass schema validation; so we can use arbitrary attributes and objectclasses
        useSchema = false
)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ActiveDirectoryTests {
    private val logger = KotlinLogging.logger {}

    private lateinit var ldapServer: LdapServerResource

    @BeforeAll
    fun startLdap() {
        logger.debug { "Starting in-memory LDAP..." }

        ldapServer = LdapServerResource(this)
        ldapServer.start()

        logger.debug { "Starting in-memory LDAP done." }
    }

    @AfterAll
    fun stopLdap() {
        logger.debug { "Stopping in-memory LDAP..." }

        ldapServer.stop()

        logger.debug { "Stopping in-memory LDAP done." }
    }

    @ParameterizedTest
    @MethodSource("validDomainProvider")
    fun `build DN for domain`(testData: DnTestData) {
        // Arrange

        // Act
        val dn = ActiveDirectory.getBaseDN(testData.value)

        //Assert
        assertThat(dn).isEqualTo(testData.expectedDN)
    }

    fun validDomainProvider() = Stream.of(
            DnTestData(value = "debuglevel.de", expectedDN = "DC=DEBUGLEVEL,DC=DE"),
            DnTestData(value = "www.debuglevel.de", expectedDN = "DC=WWW,DC=DEBUGLEVEL,DC=DE")
    )

    data class DnTestData(
            val value: String,
            val expectedDN: String? = null
    )

    @ParameterizedTest
    @MethodSource("validFilterProvider")
    fun `build domain DN`(testData: FilterTestData) {
        // Arrange

        // Act
        val dn = ActiveDirectory.getFilter(testData.value, testData.by)

        //Assert
        assertThat(dn).isEqualTo(testData.expectedFilter)
    }

    fun validFilterProvider() = Stream.of(
            FilterTestData(value = "my@mail.example", by = SearchScope.Email, expectedFilter = "(&((&(objectCategory=Person)(objectClass=User)))(mail=my@mail.example))"),
            FilterTestData(value = "myAccountName", by = SearchScope.Username, expectedFilter = "(&((&(objectCategory=Person)(objectClass=User)))(samaccountname=myAccountName))")
    )

    data class FilterTestData(
            val value: String,
            val by: SearchScope,
            val expectedFilter: String? = null
    )

    @ParameterizedTest
    @MethodSource("validUserSearchProvider")
    fun `search valid users`(testData: AccountTestData) {
        // Arrange
        val activeDirectory = ActiveDirectory("cn=admin", "password", "localhost:10389", "dc=root")

        // Act
        val users = activeDirectory.getUsers(testData.value, testData.searchScope)

        //Assert
        assertThat(users).hasSize(1)
        assertThat(users).contains(testData.user)
    }

    fun validUserSearchProvider() = Stream.of(
            AccountTestData(value = "maxmustermann", searchScope = SearchScope.Username, user = User("maxmustermann", "Max", "max@mustermann.de", "Max Mustermann")),
            AccountTestData(value = "max@mustermann.de", searchScope = SearchScope.Email, user = User("maxmustermann", "Max", "max@mustermann.de", "Max Mustermann"))
    )

    @ParameterizedTest
    @MethodSource("invalidUserSearchProvider")
    fun `search invalid users`(testData: AccountTestData) {
        // Arrange
        val activeDirectory = ActiveDirectory("cn=admin", "password", "localhost:10389", "dc=root")

        // Act
        val users = activeDirectory.getUsers(testData.value, testData.searchScope)

        //Assert
        assertThat(users).hasSize(0)
    }

    fun invalidUserSearchProvider() = Stream.of(
            AccountTestData(value = "heinzstrunk", searchScope = SearchScope.Username),
            AccountTestData(value = "heinz@strunk.de", searchScope = SearchScope.Email),

            // search by wrong value/scope combination
            AccountTestData(value = "maxmustermann", searchScope = SearchScope.Email, user = User("maxmustermann", "Max", "max@mustermann.de", "Max Mustermann")),
            AccountTestData(value = "max@mustermann.de", searchScope = SearchScope.Username, user = User("maxmustermann", "Max", "max@mustermann.de", "Max Mustermann"))
    )

    data class AccountTestData(
            val value: String,
            val searchScope: SearchScope,
            val user: User? = null
    )
}