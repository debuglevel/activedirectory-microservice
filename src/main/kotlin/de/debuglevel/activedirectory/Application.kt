package de.debuglevel.activedirectory

import io.micronaut.context.ApplicationContext
import io.micronaut.runtime.Micronaut
import io.swagger.v3.oas.annotations.OpenAPIDefinition
import io.swagger.v3.oas.annotations.info.Contact
import io.swagger.v3.oas.annotations.info.Info
import io.swagger.v3.oas.annotations.info.License
import mu.KotlinLogging

/**
 * Application entry point, which starts the Micronaut server.
 *
 * @param args parameters from the command line call
 */
@OpenAPIDefinition(
    info = Info(
        title = "ActiveDirectory Microservice",
        version = "0.2.0",
        description = "Microservice for managing a Microsoft Active Directory",
        license = License(name = "WTFPL 2.0", url = "http://www.wtfpl.net/"),
        contact = Contact(url = "http://debuglevel.de", name = "Marc Kohaupt", email = "debuglevel at gmail.com")
    )
)
object Application {
    private val logger = KotlinLogging.logger {}

    lateinit var applicationContext: ApplicationContext

    @JvmStatic
    fun main(args: Array<String>) {
        logger.info { "Starting up..." }
        applicationContext = Micronaut
            .build(*args)
            .classes(Application.javaClass)
            .banner(false)
            .start()
    }
}


