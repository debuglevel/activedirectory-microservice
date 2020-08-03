package de.debuglevel.activedirectory.user

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
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.Mockito.mock
import javax.inject.Inject

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
    var userActiveDirectoryServiceMock: UserActiveDirectoryService? = null

    @MockBean(UserActiveDirectoryService::class)
    fun userActiveDirectoryServiceMock(): UserActiveDirectoryService {
        return mock(UserActiveDirectoryService::class.java)
    }

    @BeforeEach
    fun `set up mock`() {
        TestDataProvider.`set up userActiveDirectoryService mock`(this.userActiveDirectoryServiceMock!!)
    }

    @ParameterizedTest
    @MethodSource("validUserSearchProvider")
    fun `retrieve user`(userTestData: TestDataProvider.UserTestData) {
        // Skip on test data with email search value, because the Controller only searches for username
        Assumptions.assumeTrue(userTestData.searchScope != UserSearchScope.Email)

        // Arrange
        val userResponse = UserResponse(userTestData.user!!)

        // Act
        val retrieveUri = UriBuilder.of("/{username}")
            .expand(mutableMapOf("username" to userTestData.value))
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

    fun validUserSearchProvider() =
        TestDataProvider.validUserSearchProvider()
}