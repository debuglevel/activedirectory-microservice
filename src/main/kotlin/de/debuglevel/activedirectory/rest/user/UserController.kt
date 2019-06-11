package de.debuglevel.activedirectory.rest.user

import de.debuglevel.activedirectory.domain.activedirectory.ActiveDirectory
import de.debuglevel.activedirectory.domain.activedirectory.SearchScope
import de.debuglevel.activedirectory.rest.Configuration
import de.debuglevel.activedirectory.rest.responsetransformer.JsonTransformer
import de.debuglevel.activedirectory.rest.responsetransformer.XmlTransformer
import mu.KotlinLogging
import spark.kotlin.RouteHandler


object UserController {
    private val logger = KotlinLogging.logger {}

    fun getOne(): RouteHandler.() -> String {
        return {
            val username = params(":username")

            try {
                val user = ActiveDirectory(Configuration.username, Configuration.password, Configuration.server, Configuration.searchBase)
                        .getUser(username, SearchScope.Username)

                val userDTO = UserDTO(user)

                type(contentType = "application/json")
                JsonTransformer.render(userDTO)
            } catch (e: ActiveDirectory.NoUserFoundException) {
                logger.info("User with username '$username' does not exist.")
                response.type("application/json")
                response.status(404)
                "{\"message\":\"username '$username' does not exist\"}"
            } catch (e: ActiveDirectory.MoreThanOneResultException) {
                logger.warn("User with username '$username' is ambiguous.")
                response.type("application/json")
                response.status(502)
                "{\"message\":\"username '$username' is ambiguous\"}"
            } catch (e: ActiveDirectory.ConnectionException) {
                logger.warn("Could not connect to Active Directory.")
                response.type("application/json")
                response.status(502)
                "{\"message\":\"could not connect to Active Directory\"}"
            }
        }
    }

    fun getList(): RouteHandler.() -> String {
        return {
            try {
                val users = ActiveDirectory(Configuration.username, Configuration.password, Configuration.server, Configuration.searchBase)
                        .getUsers()

                val usersDTO = users.map {
                    UserDTO(it)
                }

                type(contentType = "application/json")
                JsonTransformer.render(usersDTO)
            } catch (e: ActiveDirectory.ConnectionException) {
                logger.info("Could not connect to Active Directory.")
                response.type("application/json")
                response.status(502)
                "{\"message\":\"could not connect to Active Directory\"}"
            }
        }
    }

    fun getListXml(): RouteHandler.() -> String {
        return {
            try {
                val users = ActiveDirectory(
                    Configuration.username,
                    Configuration.password,
                    Configuration.server,
                    Configuration.searchBase
                )
                    .getUsers()

                val usersDTO = users.map {
                    UserDTO(it)
                }

                type(contentType = "application/xml")
                XmlTransformer.render(usersDTO)
            } catch (e: ActiveDirectory.ConnectionException) {
                logger.info("Could not connect to Active Directory.")
                response.type("application/xml")
                response.status(502)
                "{\"message\":\"could not connect to Active Directory\"}"
            }
        }
    }

//    fun getOneHtml(): RouteHandler.() -> String {
//        return {
//            val greetingId = request.params(":greetingId").toInt()
//
//            val model = HashMap<String, Any>()
//            MustacheTemplateEngine().render(ModelAndView(model, "activedirectory/show.html.mustache"))
//        }
//    }

//    fun getList(): RouteHandler.() -> String {
//        return {
//            val greetings = setOf<UserDTO>(
//                    UserDTO("Mozart"),
//                    UserDTO("Beethoven"),
//                    UserDTO("Haydn")
//            )
//
//            type(contentType = "application/json")
//            JsonTransformer.render(greetings)
//        }
//    }

//    fun getListHtml(): RouteHandler.() -> String {
//        return {
//            val model = HashMap<String, Any>()
//            MustacheTemplateEngine().render(ModelAndView(model, "activedirectory/list.html.mustache"))
//        }
//    }

//    fun getAddFormHtml(): RouteHandler.() -> String {
//        return {
//            val model = HashMap<String, Any>()
//            MustacheTemplateEngine().render(ModelAndView(model, "activedirectory/add.html.mustache"))
//        }
//    }
}