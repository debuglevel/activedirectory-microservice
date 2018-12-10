package de.debuglevel.activedirectory.rest

import de.debuglevel.activedirectory.rest.user.UserController
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
            path("/users") {
                //get("/", "text/html", UserController.getListHtml())
                //get("/", "application/json", UserController.getList())
                //post("/", function = UserController.postOne())

                path("/:username") {
                    get("", "application/json", UserController.getOne())
                    get("/", "application/json", UserController.getOne())
                    //get("/", "text/html", UserController.getOneHtml())
                }
            }
        }

        // add loggers
        before { logger.debug(buildRequestLog(request)) }
        after { logger.debug(buildResponseLog(request, response)) }
    }
}
