package de.debuglevel.activedirectory.rest.user

import de.debuglevel.activedirectory.domain.activedirectory.ActiveDirectory
import de.debuglevel.activedirectory.domain.activedirectory.ConnectionException
import de.debuglevel.activedirectory.domain.activedirectory.SearchScope
import de.debuglevel.activedirectory.rest.Configuration
import de.debuglevel.activedirectory.rest.responsetransformer.JsonTransformer
import mu.KotlinLogging
import spark.kotlin.RouteHandler

object UserController {
    private val logger = KotlinLogging.logger {}

    fun getOne(): RouteHandler.() -> String {
        return {
            val username = params(":username")

            try {
                val users = ActiveDirectory(Configuration.username, Configuration.password, Configuration.server)
                        .getUsers(username, SearchScope.Username)

                val user = users.first()
                val userDTO = UserDTO(
                        user.username,
                        user.givenname,
                        user.mail,
                        user.cn)

                type(contentType = "application/json")
                JsonTransformer.render(userDTO)
            } catch (e: NoSuchElementException) {
                logger.info("User with username '$username' does not exist.")
                response.type("application/json")
                response.status(404)
                "{\"message\":\"username '$username' does not exist\"}"
            } catch (e: ConnectionException) {
                logger.info("Could not connect to Active Directory.")
                response.type("application/json")
                response.status(404)
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