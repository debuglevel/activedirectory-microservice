package de.debuglevel.activedirectory.computer

import de.debuglevel.activedirectory.TestDataProvider
import io.micronaut.core.type.Argument
import io.micronaut.http.HttpRequest
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.annotation.Client
import io.micronaut.http.client.exceptions.HttpClientResponseException
import io.micronaut.http.uri.UriBuilder
import io.micronaut.runtime.server.EmbeddedServer
import io.micronaut.test.annotation.MicronautTest
import io.micronaut.test.annotation.MockBean
import mu.KotlinLogging
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.catchThrowable
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.Mockito.mock
import javax.inject.Inject

@MicronautTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ComputerControllerTests {
    private val logger = KotlinLogging.logger {}

    @Inject
    lateinit var server: EmbeddedServer

    @Inject
    @field:Client("/computers")
    lateinit var httpClient: HttpClient

    @Inject
    var computerActiveDirectoryServiceMock: ComputerActiveDirectoryService? = null

    @MockBean(ComputerActiveDirectoryService::class)
    fun computerActiveDirectoryServiceMock(): ComputerActiveDirectoryService {
        logger.debug { "Creating mock..." }
        return mock(ComputerActiveDirectoryService::class.java)
    }

    @BeforeEach
    fun `set up mock`() {
        logger.debug { "Setting up mock..." }
        TestDataProvider.`set up computerActiveDirectoryService mock`(this.computerActiveDirectoryServiceMock!!)
    }

    @ParameterizedTest
    @MethodSource("validComputerSearchProvider")
    fun `retrieve computer`(computerTestData: TestDataProvider.ComputerTestData) {
        // Arrange
        val computerResponse = ComputerResponse(computerTestData.computer!!)

        // Act
        val retrieveUri = UriBuilder.of("/{name}")
            .expand(mutableMapOf("name" to computerTestData.value))
            .toString()
        val httpRequest = HttpRequest
            .GET<String>(retrieveUri)
            .basicAuth("SECRET_USERNAME", "SECRET_PASSWORD")
        val retrievedPerson = httpClient.toBlocking()
            .retrieve(httpRequest, ComputerResponse::class.java)

        // Assert
        Assertions.assertThat(retrievedPerson).isEqualTo(computerResponse)
    }

    @Test
    fun `retrieve list`() {
        // Arrange

        // Act
        val retrieveUri = UriBuilder.of("/").build()
        val httpRequest = HttpRequest
            .GET<String>(retrieveUri)
            .basicAuth("SECRET_USERNAME", "SECRET_PASSWORD")
        val argument = Argument.of(List::class.java, ComputerResponse::class.java)
        val retrievedPersons = httpClient.toBlocking()
            .retrieve(httpRequest, argument) as List<ComputerResponse>

        // Assert
        validComputerSearchProvider().forEach { testData ->
            val computerResponse = ComputerResponse(testData.computer!!)

            Assertions.assertThat(retrievedPersons)
                .anyMatch { retrieved -> retrieved == computerResponse }
        }
    }

    @Test
    fun `fail retrieving list`() {
        // Arrange

        // Act
        val retrieveUri = UriBuilder.of("/").build()
        val httpRequest = HttpRequest
            .GET<String>(retrieveUri)
        val thrown = catchThrowable {
            httpClient.toBlocking().retrieve(httpRequest)
        }

        // Assert
        Assertions.assertThat(thrown)
            .isInstanceOf(HttpClientResponseException::class.java)
            .hasMessageContaining("Unauthorized")
    }

    fun validComputerSearchProvider() =
        TestDataProvider.validComputerSearchProvider()
}