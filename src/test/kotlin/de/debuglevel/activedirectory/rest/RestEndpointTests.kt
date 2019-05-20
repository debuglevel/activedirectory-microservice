package de.debuglevel.activedirectory.rest

import de.debuglevel.microservices.utils.spark.SparkTestUtils
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import spark.Spark.awaitInitialization
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RestEndpointTests {

    init {
        val restEndpoint = RestEndpoint()
        restEndpoint.start(arrayOf())

        awaitInitialization()
    }

    @AfterAll
    fun stopServer() {
        SparkTestUtils.awaitShutdown()
    }

    @Test
    fun `server listens on default port`() {
        // Arrange

        // Act
        val response = ApiTestUtils.request("GET", "/users/", null)

        // Assert
        // HTTP Codes begin from "100". So something from 100 and above was probably a response to a HTTP request
        assertThat(response?.status).isGreaterThanOrEqualTo(100)
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class `valid requests on users` {
//        @Test
//        fun `server lists users`() {
//            // Arrange
//
//            // Act
//            val response = ApiTestUtils.request("GET", "/users/", null)
//
//            // Assert
//            assertThat(response?.body).contains("Mozart")
//            assertThat(response?.body).contains("Beethoven")
//            assertThat(response?.body).contains("Haydn")
//        }

//        @ParameterizedTest
//        @MethodSource("validDomainProvider")
//        fun `server sends user in body`(testData: NameTestData) {
//            // Arrange
//
//            // Act
//            val response = ApiTestUtils.request("GET", "/users/${testData.value}", null)
//
//            // Assert
//            assertThat(response?.body).contains(testData.expected)
//        }

//        @ParameterizedTest
//        @MethodSource("validDomainProvider")
//        fun `server sends correct user on api version 2 and default`(testData: NameTestData) {
//            // Arrange
//
//            // Act
//            val responseApiDefault = ApiTestUtils.request("GET", "/users/${testData.value}", null)
//            val responseApiV2 = ApiTestUtils.request("GET", "/v2/users/${testData.value}", null)
//
//            // Assert
//            assertThat(responseApiDefault?.body).contains(testData.expected)
//            assertThat(responseApiV2?.body).contains(testData.expected)
//        }

//        @ParameterizedTest
//        @MethodSource("validNameProviderApiV1")
//        fun `server sends correct user on api version 1`(testData: NameTestData) {
//            // Arrange
//
//            // Act
//            val responseApiV1 = ApiTestUtils.request("GET", "/v1/users/${testData.value}", null)
//
//            // Assert
//            assertThat(responseApiV1?.body).contains(testData.expected)
//        }

//        @ParameterizedTest
//        @MethodSource("validDomainProvider")
//        fun `server sends status code 200`(testData: NameTestData) {
//            // Arrange
//
//            // Act
//            val response = ApiTestUtils.request("GET", "/users/${testData.value}", null)
//
//            // Assert
//            assertThat(response?.status).isEqualTo(200)
//        }

        @ParameterizedTest
        @MethodSource("validNameProvider")
        fun `server sends json content type`(testData: NameTestData) {
            // Arrange

            // Act
            val response = ApiTestUtils.request("GET", "/users/${testData.value}", null)

            // Assert
            assertThat(response?.contentType).isEqualTo("application/json")
        }

        @ParameterizedTest
        @MethodSource("validNameProvider")
        fun `server sends json content`(testData: NameTestData) {
            // Arrange

            // Act
            val response = ApiTestUtils.request("GET", "/users/${testData.value}", null)

            // Assert
            val validJson = JsonUtils.isJSONValid(response?.body)
            assertThat(validJson).isTrue()
        }

        fun validNameProvider() = Stream.of(
                NameTestData(value = "Mozart", expected = "Hello, Mozart!"),
                NameTestData(value = "Amadeus", expected = "Hello, Amadeus!"),
                // TODO: Umlauts do not work when executed as gradle task in Windows
//                DnTestData(value = "H%C3%A4nschen", expectedFilter = "Hello, Hänschen!"),
                NameTestData(value = "Max%20Mustermann", expected = "Hello, Max Mustermann!")
        )

        fun validNameProviderApiV1() = Stream.of(
                NameTestData(value = "Mozart", expected = "Hello from API v1, Mozart!"),
                NameTestData(value = "Amadeus", expected = "Hello from API v1, Amadeus!"),
                // TODO: Umlauts do not work when executed as gradle task in Windows
//                DnTestData(value = "H%C3%A4nschen", expectedFilter = "Hello, Hänschen!"),
                NameTestData(value = "Max%20Mustermann", expected = "Hello from API v1, Max Mustermann!")
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class `invalid requests on greet` {
//        @ParameterizedTest
//        @MethodSource("invalidNameProvider")
//        fun `server does not send user in body`(testData: NameTestData) {
//            // Arrange
//
//            // Act
//            val response = ApiTestUtils.request("GET", "/users/${testData.value}", null)
//
//            // Assert
//            assertThat(response?.body).doesNotContain("Hello, ${testData.value}!")
//        }

//        @ParameterizedTest
//        @MethodSource("invalidNameProvider")
//        fun `server sends error message`(testData: NameTestData) {
//            // Arrange
//
//            // Act
//            val response = ApiTestUtils.request("GET", "/users/${testData.value}", null)
//
//            // Assert
//            assertThat(response?.body).contains("message")
//        }

//        @ParameterizedTest
//        @MethodSource("invalidNameProvider")
//        fun `server sends status code 400`(testData: NameTestData) {
//            // Arrange
//
//            // Act
//            val response = ApiTestUtils.request("GET", "/users/${testData.value}", null)
//
//            // Assert
//            assertThat(response?.status).isEqualTo(400)
//        }

        @ParameterizedTest
        @MethodSource("invalidNameProvider")
        fun `server sends json content type`(testData: NameTestData) {
            // Arrange

            // Act
            val response = ApiTestUtils.request("GET", "/users/${testData.value}", null)

            // Assert
            assertThat(response?.contentType).isEqualTo("application/json")
        }

        @ParameterizedTest
        @MethodSource("invalidNameProvider")
        fun `server sends json content`(testData: NameTestData) {
            // Arrange

            // Act
            val response = ApiTestUtils.request("GET", "/users/${testData.value}", null)

            // Assert
            val validJson = JsonUtils.isJSONValid(response?.body)
            assertThat(validJson).isTrue()
        }

        fun invalidNameProvider() = Stream.of(
                //DnTestData(value = ""),
                NameTestData(value = "%20")
        )
    }

    data class NameTestData(
            val value: String,
            val expected: String? = null
    )
}