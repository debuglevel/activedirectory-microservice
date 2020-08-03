package de.debuglevel.activedirectory.computer

import de.debuglevel.activedirectory.TestDataProvider
import de.debuglevel.activedirectory.TestDataProvider.`set up computerActiveDirectoryService mock`
import io.micronaut.http.client.exceptions.HttpClientResponseException
import io.micronaut.test.annotation.MicronautTest
import io.micronaut.test.annotation.MockBean
import mu.KotlinLogging
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.Mockito.mock
import java.util.*
import javax.inject.Inject

@MicronautTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ComputerClientTests {
    private val logger = KotlinLogging.logger {}

    @Inject
    lateinit var computerClient: ComputerClient

    private val basicAuthenticationHeader: String
        get() {
            val encodedCredentials =
                Base64.getEncoder().encodeToString("SECRET_USERNAME:SECRET_PASSWORD".byteInputStream().readBytes())
            return "Basic $encodedCredentials"
        }

    private val badBasicAuthenticationHeader: String
        get() {
            val encodedCredentials =
                Base64.getEncoder().encodeToString("SECRET_USERNAME:WRONG_PASSWORD".byteInputStream().readBytes())
            return "Basic $encodedCredentials"
        }

    @Inject
    var computerActiveDirectoryServiceMock: ComputerActiveDirectoryService? = null

    @MockBean(ComputerActiveDirectoryService::class)
    fun computerActiveDirectoryServiceMock(): ComputerActiveDirectoryService {
        return mock(ComputerActiveDirectoryService::class.java)
    }

    @BeforeEach
    fun `set up mock`() {
        `set up computerActiveDirectoryService mock`(this.computerActiveDirectoryServiceMock!!)
    }

    @ParameterizedTest
    @MethodSource("validComputerSearchProvider")
    fun `retrieve computer`(computerTestData: TestDataProvider.ComputerTestData) {
        // Arrange
        val computerResponse = ComputerResponse(computerTestData.computer!!)

        // Act
        val retrievedComputer = computerClient.getOne(computerTestData.value, basicAuthenticationHeader).blockingGet()

        // Assert
        Assertions.assertThat(retrievedComputer).isEqualTo(computerResponse)
    }

    @ParameterizedTest
    @MethodSource("validComputerSearchProvider")
    fun `retrieve computer with bad authentication`(computerTestData: TestDataProvider.ComputerTestData) {
        // Arrange

        // Act
        val thrown = Assertions.catchThrowable {
            computerClient.getOne(computerTestData.value, badBasicAuthenticationHeader).blockingGet()
        }

        // Assert
        Assertions.assertThat(thrown)
            .isInstanceOf(HttpClientResponseException::class.java)
            .hasMessageContaining("Unauthorized")
    }

    @Test
    fun `retrieve list`() {
        // Arrange

        // Act
        val retrievedComputers = computerClient.getJsonList(basicAuthenticationHeader)

        // Assert
        validComputerSearchProvider().forEach { testData ->
            val computerResponse = ComputerResponse(testData.computer!!)

            Assertions.assertThat(retrievedComputers)
                .anyMatch { retrieved -> retrieved == computerResponse }
        }
    }

    @Test
    fun `retrieve list with bad authentication`() {
        // Arrange

        // Act
        val thrown = Assertions.catchThrowable {
            computerClient.getJsonList(badBasicAuthenticationHeader)
        }

        // Assert
        Assertions.assertThat(thrown)
            .isInstanceOf(HttpClientResponseException::class.java)
            .hasMessageContaining("Unauthorized")
    }

    fun validComputerSearchProvider() =
        TestDataProvider.validComputerSearchProvider()
}