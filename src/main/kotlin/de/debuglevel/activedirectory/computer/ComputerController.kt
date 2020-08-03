package de.debuglevel.activedirectory.computer

import de.debuglevel.activedirectory.ActiveDirectoryService
import de.debuglevel.activedirectory.ActiveDirectoryUtils
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
@Controller("/computers")
class ComputerController(private val activeDirectoryService: ComputerActiveDirectoryService) {
    private val logger = KotlinLogging.logger {}

    @Get("/{name}")
    fun getOne(name: String): HttpResponse<ComputerResponse> {
        logger.debug("Called getOne($name)")

        return try {
            val computer = activeDirectoryService.get(
                name,
                ComputerSearchScope.Name
            )
            HttpResponse.ok(ComputerResponse(computer))
        } catch (e: ActiveDirectoryService.NoItemFoundException) {
            HttpResponse.notFound(ComputerResponse(error = "Computer '$name' not found"))
        } catch (e: ActiveDirectoryService.MoreThanOneResultException) {
            HttpResponse.serverError(ComputerResponse(error = "Computer name '$name' is ambiguous"))
        } catch (e: ActiveDirectoryUtils.ConnectionException) {
            HttpResponse.serverError(ComputerResponse(error = "Could not connect to Active Directory"))
        }
    }

    @Get("/")
    fun getList(): HttpResponse<Set<ComputerResponse>> {
        logger.debug("Called getList()")

        return try {
            val users = activeDirectoryService.getAll()
                .map {
                    ComputerResponse(it)
                }.toSet()

            HttpResponse.ok(users)
        } catch (e: ActiveDirectoryUtils.ConnectionException) {
            val response = setOf(ComputerResponse(error = "Could not connect to Active Directory"))
            HttpResponse.serverError(response)
        }
    }
}