package de.debuglevel.activedirectory.domain.activedirectory

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream


@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ActiveDirectoryTests {
    @ParameterizedTest
    @MethodSource("validDomainProvider")
    fun `build DN for domain`(testData: DnTestData) {
        // Arrange

        // Act
        val dn = ActiveDirectory.getBaseDN(testData.value)

        //Assert
        assertThat(dn).isEqualTo(testData.expectedDN)
    }

    fun validDomainProvider() = Stream.of(
            DnTestData(value = "debuglevel.de", expectedDN = "DC=DEBUGLEVEL,DC=DE"),
            DnTestData(value = "www.debuglevel.de", expectedDN = "DC=WWW,DC=DEBUGLEVEL,DC=DE")
    )

    @ParameterizedTest
    @MethodSource("validFilterProvider")
    fun `build domain DN`(testData: FilterTestData) {
        // Arrange

        // Act
        val dn = ActiveDirectory.getFilter(testData.value, testData.by)

        //Assert
        assertThat(dn).isEqualTo(testData.expectedFilter)
    }

    fun validFilterProvider() = Stream.of(
            FilterTestData(value = "my@mail.example", by = SearchScope.Email, expectedFilter = "(&((&(objectCategory=Person)(objectClass=User)))(mail=my@mail.example))"),
            FilterTestData(value = "myAccountName", by = SearchScope.Username, expectedFilter = "(&((&(objectCategory=Person)(objectClass=User)))(samaccountname=myAccountName))")
    )

    data class DnTestData(
            val value: String,
            val expectedDN: String? = null
    )

    data class FilterTestData(
            val value: String,
            val by: SearchScope,
            val expectedFilter: String? = null
    )
}