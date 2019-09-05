package de.debuglevel.activedirectory

import com.github.trevershick.test.ldap.LdapServerResource
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
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import javax.inject.Inject
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock

@MicronautTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UserControllerTests {
    private val logger = KotlinLogging.logger {}

    @Inject
    lateinit var server: EmbeddedServer

    @Inject
    @field:Client("/users")
    lateinit var httpClient: HttpClient

    @Inject
    var activeDirectoryServiceMock: ActiveDirectoryService? = null

    @MockBean(ActiveDirectoryService::class)
    fun activeDirectoryServiceMock(): ActiveDirectoryService {
        return mock(ActiveDirectoryService::class.java)
    }

    @BeforeEach
    fun `set up mock`() {
        TestDataProvider.`set up activeDirectoryService mock`(this.activeDirectoryServiceMock!!)
    }

    @ParameterizedTest
    @MethodSource("validUserSearchProvider")
    fun `retrieve person`(accountTestData: TestDataProvider.AccountTestData) {
        // Arrange
        val userResponse = UserResponse(accountTestData.user!!)

        // Act
        val retrieveUri = UriBuilder.of("/{username}")
            .expand(mutableMapOf("username" to accountTestData.value))
            .toString()
        val httpRequest = HttpRequest
            .GET<String>(retrieveUri)
            .basicAuth("SECRET_USERNAME", "SECRET_PASSWORD")
        val retrievedPerson = httpClient.toBlocking()
            .retrieve(httpRequest, UserResponse::class.java)

        // Assert
        Assertions.assertThat(retrievedPerson).isEqualTo(userResponse)
    }

    @Test
    fun `retrieve list`() {
        // Arrange

        // Act
        val retrieveUri = UriBuilder.of("/").build()
        val httpRequest = HttpRequest
            .GET<String>(retrieveUri)
            .basicAuth("SECRET_USERNAME", "SECRET_PASSWORD")
        val argument = Argument.of(List::class.java, UserResponse::class.java)
        val retrievedPersons = httpClient.toBlocking()
            .retrieve(httpRequest, argument) as List<UserResponse>

        // Assert
        validUserSearchProvider().forEach { testData ->
            val userResponse = UserResponse(testData.user!!)

            Assertions.assertThat(retrievedPersons)
                .anyMatch { retrieved -> retrieved == userResponse }
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

    fun validUserSearchProvider() = TestDataProvider.validUserSearchProvider()
}