package de.debuglevel.activedirectory

import com.github.trevershick.test.ldap.LdapServerResource
import com.github.trevershick.test.ldap.annotations.LdapAttribute
import com.github.trevershick.test.ldap.annotations.LdapConfiguration
import com.github.trevershick.test.ldap.annotations.LdapEntry
import io.micronaut.context.ApplicationContext
import mu.KotlinLogging
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.*
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

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
        ),
        LdapEntry
            (
            dn = "cn=Alex Aloah,dc=root",
            objectclass = arrayOf("User"),
            attributes = arrayOf
                (
                LdapAttribute(name = "objectCategory", value = arrayOf("Person")),
                LdapAttribute(name = "samaccountname", value = arrayOf("alexaloah")),
                LdapAttribute(name = "givenname", value = arrayOf("Alex")),
                LdapAttribute(name = "mail", value = arrayOf("alex@aloah.de"))
            )
        )
    ),
    // bypass schema validation; so we can use arbitrary attributes and objectclasses
    useSchema = false
)
//@MicronautTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ActiveDirectoryServiceTests {
    private val logger = KotlinLogging.logger {}

    private lateinit var applicationContext: ApplicationContext
    private lateinit var activeDirectoryService: ActiveDirectoryService

    private lateinit var ldapServer: LdapServerResource

    private fun getValidActiveDirectory() = activeDirectoryService
    private fun getInvalidActiveDirectory() =
        ActiveDirectoryService(
            "cn=admin",
            "dsghkjfgh",
            "localhost:4711",
            "dc=root"
        )

    @BeforeAll
    fun setup() {
        // @MicronautTest is disabled, as the @Inject'ion would occur before the LDAP server is started
        // Therefore, the LDAP server is started, then the ApplicationContext and then the Bean-wiring is done manually
        logger.debug { "Setting up..." }
        startLdap()
        startMicronaut()
    }

    private fun startMicronaut() {
        logger.debug { "Starting Micronaut ApplicationContext..." }
        applicationContext = ApplicationContext.run()
        logger.debug { "Initializing beans..." }
        activeDirectoryService = applicationContext.getBean(ActiveDirectoryService::class.java)
    }

    private fun startLdap() {
        logger.debug { "Starting in-memory LDAP server..." }
        ldapServer = LdapServerResource(this)
        ldapServer.start()
        logger.debug { "Started in-memory LDAP server" }
    }

    @AfterAll
    fun shutdown() {
        logger.debug { "Shutting down..." }
        shutdownMicronaut()
        stopLdap()
    }

    private fun shutdownMicronaut() {
        logger.debug { "Shutting down Micronaut ApplicationContext..." }
        applicationContext.close()
        applicationContext.stop()
    }

    private fun stopLdap() {
        logger.debug { "Stopping in-memory LDAP server..." }
        ldapServer.stop()
        logger.debug { "Stopped in-memory LDAP server" }
    }

    @ParameterizedTest
    @MethodSource("validDomainProvider")
    fun `build DN for domain`(testData: TestDataProvider.DnTestData) {
        // Arrange

        // Act
        val dn = ActiveDirectoryService.getBaseDN(testData.value)

        //Assert
        assertThat(dn).isEqualTo(testData.expectedDN)
    }

    fun validDomainProvider() = TestDataProvider.validDomainProvider()

    @ParameterizedTest
    @MethodSource("validFilterProvider")
    fun `build domain DN`(testData: TestDataProvider.FilterTestData) {
        // Arrange

        // Act
        val dn = ActiveDirectoryService.getFilter(testData.value, testData.by)

        //Assert
        assertThat(dn).isEqualTo(testData.expectedFilter)
    }

    fun validFilterProvider() = TestDataProvider.validFilterProvider()

    @Test
    fun `connect to valid Active Directory`() {
        // Arrange

        // Act & Assert
        Assertions.assertDoesNotThrow {
            ActiveDirectoryService(
                "cn=admin",
                "password",
                "localhost:10389",
                "dc=root"
            ).initialize()
        }
    }

    @Test
    fun `connect to invalid Active Directory`() {
        // Arrange

        // Act & Assert
        assertThrows<ActiveDirectoryService.ConnectionException> {
            getInvalidActiveDirectory().initialize()
        }
    }

    @ParameterizedTest
    @MethodSource("validUserSearchProvider")
    fun `search valid users`(testData: TestDataProvider.AccountTestData) {
        // Arrange

        // Act
        val users = activeDirectoryService.getUsers(testData.value, testData.searchScope)

        //Assert
        assertThat(users).hasSize(1)
        assertThat(users).contains(testData.user)
    }

    @Test
    fun `get all users`() {
        // Arrange

        // Act
        val users = activeDirectoryService.getUsers()

        //Assert
        assertThat(users).hasSize(2)
        validUserSearchProvider().forEach { assertThat(users).contains(it.user) }
    }

    fun validUserSearchProvider() = TestDataProvider.validUserSearchProvider()

    @ParameterizedTest
    @MethodSource("invalidUserSearchProvider")
    fun `search invalid users`(testData: TestDataProvider.AccountTestData) {
        // Arrange

        // Act
        val users = activeDirectoryService.getUsers(testData.value, testData.searchScope)

        //Assert
        assertThat(users).hasSize(0)
    }

    fun invalidUserSearchProvider() = TestDataProvider.invalidUserSearchProvider()
}