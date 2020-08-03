package de.debuglevel.activedirectory.user

import de.debuglevel.activedirectory.TestDataProvider
import de.debuglevel.activedirectory.TestDataProvider.`set up userActiveDirectoryService mock`
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
class UserClientTests {
    private val logger = KotlinLogging.logger {}

    @Inject
    lateinit var userClient: UserClient

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
    var userActiveDirectoryServiceMock: UserActiveDirectoryService? = null

    @MockBean(UserActiveDirectoryService::class)
    fun userActiveDirectoryServiceMock(): UserActiveDirectoryService {
        return mock(UserActiveDirectoryService::class.java)
    }

    @BeforeEach
    fun `set up mock`() {
        `set up userActiveDirectoryService mock`(this.userActiveDirectoryServiceMock!!)
    }

    @ParameterizedTest
    @MethodSource("validUserSearchProvider")
    fun `retrieve user`(userTestData: TestDataProvider.UserTestData) {
        // Arrange
        val userResponse = UserResponse(userTestData.user!!)

        // Act
        val retrievedPerson = userClient.getOne(userTestData.value, basicAuthenticationHeader).blockingGet()

        // Assert
        Assertions.assertThat(retrievedPerson).isEqualTo(userResponse)
    }

    @ParameterizedTest
    @MethodSource("validUserSearchProvider")
    fun `retrieve user with bad authentication`(userTestData: TestDataProvider.UserTestData) {
        // Arrange

        // Act
        val thrown = Assertions.catchThrowable {
            userClient.getOne(userTestData.value, badBasicAuthenticationHeader).blockingGet()
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
        val retrievedPersons = userClient.getJsonList(basicAuthenticationHeader)

        // Assert
        validUserSearchProvider().forEach { testData ->
            val userResponse = UserResponse(testData.user!!)

            Assertions.assertThat(retrievedPersons)
                .anyMatch { retrieved -> retrieved == userResponse }
        }
    }

    @Test
    fun `retrieve list with bad authentication`() {
        // Arrange

        // Act
        val thrown = Assertions.catchThrowable {
            userClient.getJsonList(badBasicAuthenticationHeader)
        }

        // Assert
        Assertions.assertThat(thrown)
            .isInstanceOf(HttpClientResponseException::class.java)
            .hasMessageContaining("Unauthorized")
    }

    fun validUserSearchProvider() =
        TestDataProvider.validUserSearchProvider()
}