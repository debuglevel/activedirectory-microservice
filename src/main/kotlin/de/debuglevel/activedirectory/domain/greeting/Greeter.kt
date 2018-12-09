package de.debuglevel.activedirectory.domain.greeting

import de.debuglevel.activedirectory.rest.greeting.GreetingDTO
import mu.KotlinLogging

/**
 * Greets persons
 */
object Greeter {
    private val logger = KotlinLogging.logger {}

    /**
     * Greets a person.
     *
     * A name like `Mozart` gets greeted like `Hello, Mozart!`.
     *
     * @param name a (valid) name which should be greeted
     * @throws GreetingException thrown if the given name is invalid (i.e. blank)
     * @return a activedirectory
     */
    @Throws(GreetingException::class)
    fun greet(name: String): GreetingDTO {
        logger.debug { "Greeting '$name'..." }

        if (name.isBlank()) {
            throw GreetingException("Cannot activedirectory a blank name.")
        }

        val greeting = GreetingDTO(name)

        logger.debug { "Greeted '$name' like '${greeting.greeting}'..." }

        return greeting
    }

    class GreetingException(message: String) : Exception(message)
}