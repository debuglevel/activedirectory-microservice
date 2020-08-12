package de.debuglevel.activedirectory.user

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UserTests {

    @Test
    fun `is disabled on appropriate userAccountControl attribute`() {
        // Arrange

        // Act
        val user1 = User("test", userAccountControl = 2, guid = null)
        val user2 = User("test", userAccountControl = 514, guid = null)

        // Assert
        assertThat(user1.disabled).isEqualTo(true)
        assertThat(user2.disabled).isEqualTo(true)
    }

    @Test
    fun `is enabled on appropriate userAccountControl attribute`() {
        // Arrange

        // Act
        val user1 = User("test", userAccountControl = 0, guid = null)
        val user2 = User("test", userAccountControl = 512, guid = null)

        // Assert
        assertThat(user1.disabled).isEqualTo(false)
        assertThat(user2.disabled).isEqualTo(false)
    }
}