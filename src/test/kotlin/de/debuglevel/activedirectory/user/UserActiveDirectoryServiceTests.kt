package de.debuglevel.activedirectory.user

import com.github.trevershick.test.ldap.LdapServerResource
import com.github.trevershick.test.ldap.annotations.LdapAttribute
import com.github.trevershick.test.ldap.annotations.LdapConfiguration
import com.github.trevershick.test.ldap.annotations.LdapEntry
import de.debuglevel.activedirectory.ActiveDirectoryUtils
import de.debuglevel.activedirectory.ActiveDirectoryUtils.getBaseDN
import de.debuglevel.activedirectory.TestDataProvider
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
class UserActiveDirectoryServiceTests {
    private val logger = KotlinLogging.logger {}

    private lateinit var applicationContext: ApplicationContext
    private lateinit var activeDirectoryService: UserActiveDirectoryService

    private lateinit var ldapServer: LdapServerResource

    private fun getValidActiveDirectory() = activeDirectoryService
    private fun getInvalidActiveDirectory() =
        UserActiveDirectoryService(
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
        activeDirectoryService = applicationContext.getBean(UserActiveDirectoryService::class.java)
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
        val dn = getBaseDN(testData.value)

        //Assert
        assertThat(dn).isEqualTo(testData.expectedDN)
    }

    fun validDomainProvider() = TestDataProvider.validDomainProvider()

    @ParameterizedTest
    @MethodSource("validUserFilterProvider")
    fun `build domain DN`(testData: TestDataProvider.UserFilterTestData) {
        // Arrange

        // Act
        val dn = activeDirectoryService.buildFilter(testData.value, testData.by)

        //Assert
        assertThat(dn).isEqualTo(testData.expectedFilter)
    }

    fun validUserFilterProvider() = TestDataProvider.validUserFilterProvider()

    @Test
    fun `connect to valid Active Directory`() {
        // Arrange

        // Act & Assert
        Assertions.assertDoesNotThrow {
            UserActiveDirectoryService(
                "cn=admin",
                "password",
                "localhost:10389",
                "dc=root"
            ).createLdapContext()
        }
    }

    @Test
    fun `connect to invalid Active Directory`() {
        // Arrange

        // Act & Assert
        assertThrows<ActiveDirectoryUtils.ConnectionException> {
            getInvalidActiveDirectory().createLdapContext()
        }
    }

    @ParameterizedTest
    @MethodSource("validUserSearchProvider")
    fun `search valid users`(testData: TestDataProvider.AccountTestData) {
        // Arrange

        // Act
        val users = activeDirectoryService.getAll(testData.value, testData.searchScope)

        //Assert
        assertThat(users).hasSize(1)
        assertThat(users).contains(testData.user)
    }

    @Test
    fun `get all users`() {
        // Arrange

        // Act
        val users = activeDirectoryService.getAll()

        //Assert
        assertThat(users).hasSize(2)
        validUserSearchProvider().forEach { assertThat(users).contains(it.user) }
    }

    fun validUserSearchProvider() =
        TestDataProvider.validUserSearchProvider()

    @ParameterizedTest
    @MethodSource("invalidUserSearchProvider")
    fun `search invalid users`(testData: TestDataProvider.AccountTestData) {
        // Arrange

        // Act
        val users = activeDirectoryService.getAll(testData.value, testData.searchScope)

        //Assert
        assertThat(users).hasSize(0)
    }

    fun invalidUserSearchProvider() =
        TestDataProvider.invalidUserSearchProvider()

    companion object {
        private val logger = KotlinLogging.logger {}

        /**
         * Just a small hack to get the LDAP server used in the tests running for manual tests.
         * Note: IntelliJ somehow does not want to run this main() via the Gutter icon; create a Configuration instead to start it.
         */
        @JvmStatic
        fun main(args: Array<String>) {
            logger.info { "Starting up..." }
            val obj = UserActiveDirectoryServiceTests()
            obj.startLdap()
            logger.info { "Started LDAP server on port ${obj.ldapServer.port()}." }
            println("Press enter to stop LDAP server")
            readLine()
            obj.stopLdap()
        }
    }
}