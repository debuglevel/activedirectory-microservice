package de.debuglevel.activedirectory.user

import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.security.annotation.Secured
import io.micronaut.security.rules.SecurityRule
import mu.KotlinLogging

/**
 * Generates greetings for persons
 */
@Secured(SecurityRule.IS_AUTHENTICATED)
@Controller("/users")
class UserController(private val activeDirectoryService: UserActiveDirectoryService) {
    private val logger = KotlinLogging.logger {}

    @Get("/{username}")
    fun getOne(username: String): HttpResponse<UserResponse> {
        logger.debug("Called getOne($username)")

        return try {
            val user = activeDirectoryService.getUser(username, SearchScope.Username)
            HttpResponse.ok(UserResponse(user))
        } catch (e: UserActiveDirectoryService.NoUserFoundException) {
            HttpResponse.notFound(UserResponse(error = "User '$username' not found"))
        } catch (e: UserActiveDirectoryService.MoreThanOneResultException) {
            HttpResponse.serverError(UserResponse(error = "Username '$username' is ambiguous"))
        } catch (e: UserActiveDirectoryService.ConnectionException) {
            HttpResponse.serverError(UserResponse(error = "Could not connect to Active Directory"))
        }
    }

    @Get("/")
    fun getList(): HttpResponse<Set<UserResponse>> {
        logger.debug("Called getList()")

        return try {
            val users = activeDirectoryService.getUsers()
                .map {
                    UserResponse(it)
                }.toSet()

            HttpResponse.ok(users)
        } catch (e: UserActiveDirectoryService.ConnectionException) {
            val response = setOf(UserResponse(error = "Could not connect to Active Directory"))
            HttpResponse.serverError(response)
        }
    }
}