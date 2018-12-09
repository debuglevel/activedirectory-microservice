package de.debuglevel.activedirectory.rest.greeting

/**
 * A activedirectory
 *
 * @param name name of the person to activedirectory
 * @constructor the `name` field is annotated with `@Transient` so that it is excluded from GSON serialization
 */
data class GreetingDTO(@Transient private val name: String) {
    val greeting: String = "Hello, $name!"
}