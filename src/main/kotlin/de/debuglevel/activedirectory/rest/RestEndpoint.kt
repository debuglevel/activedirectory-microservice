package de.debuglevel.activedirectory.rest

import de.debuglevel.activedirectory.rest.greeting.GreetingController
import de.debuglevel.microservices.utils.apiversion.apiVersion
import de.debuglevel.microservices.utils.logging.buildRequestLog
import de.debuglevel.microservices.utils.logging.buildResponseLog
import de.debuglevel.microservices.utils.spark.configuredPort
import de.debuglevel.microservices.utils.status.status
import mu.KotlinLogging
import spark.Spark.path
import spark.kotlin.after
import spark.kotlin.before
import spark.kotlin.get


/**
 * REST endpoint
 */
class RestEndpoint {
    private val logger = KotlinLogging.logger {}

    /**
     * Starts the REST endpoint to enter a listening state
     *
     * @param args parameters to be passed from main() command line
     */
    fun start(args: Array<String>) {
        logger.info("Starting...")
        configuredPort()
        status(this::class.java)

        apiVersion("1", true)
        {
            path("/greetings") {
                //get("/", "text/html", GreetingController.getListHtml())
                get("/", "application/json", GreetingController.getList())
                //post("/", function = GreetingController.postOne())

                path("/:name") {
                    get("", "application/json", GreetingController.getOne())
                    get("/", "application/json", GreetingController.getOne())
                    //get("/", "text/html", GreetingController.getOneHtml())
                }
            }
        }

        // add loggers
        before { logger.debug(buildRequestLog(request)) }
        after { logger.debug(buildResponseLog(request, response)) }
    }
}
