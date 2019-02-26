package de.debuglevel.activedirectory.rest

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import spark.Spark
import java.io.IOException
import java.net.ServerSocket


@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MainKtTest {

    @Test
    fun `standalone startup`() {
        // Arrange
        main(arrayOf())
        Spark.awaitInitialization()

        // Act
        val response = ApiTestUtils.request("GET", "/greetings/test", null)

        // Assert
        // HTTP Codes begin from "100". So something from 100 and above was probably a response to a HTTP request
        Assertions.assertThat(response?.status).isGreaterThanOrEqualTo(100)
    }

    @AfterAll
    fun stopServer() {
        SparkTestUtils.awaitShutdown()
    }

    private fun isLocalPortInUse(port: Int): Boolean {
        return try {
            // ServerSocket try to open a LOCAL port
            ServerSocket(port).close()
            // local port can be opened, it's available
            false
        } catch (e: IOException) {
            // local port cannot be opened, it's in use
            true
        }
    }
}