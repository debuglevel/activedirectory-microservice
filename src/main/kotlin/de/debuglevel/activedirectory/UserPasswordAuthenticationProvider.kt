package de.debuglevel.activedirectory

import io.micronaut.context.annotation.Property
import io.micronaut.security.authentication.*
import io.reactivex.Flowable
import mu.KotlinLogging
import org.reactivestreams.Publisher
import java.util.*
import javax.inject.Singleton

/**
 * Example for an AuthenticationProvider, which just checks for hardcoded credentials
 */
@Singleton
class UserPasswordAuthenticationProvider(
    @Property(name = "app.security.username") private val username: String,
    @Property(name = "app.security.password") private val password: String
    ) : AuthenticationProvider {
    private val logger = KotlinLogging.logger {}

    override fun authenticate(authenticationRequest: AuthenticationRequest<*, *>): Publisher<AuthenticationResponse> {
//        logger.debug { "Got request with username '${authenticationRequest.identity}' and password '${authenticationRequest.secret}'" }
        if (authenticationRequest.identity == username &&
            authenticationRequest.secret == password
        ) {
            logger.debug { "Credentials are valid" }
            return Flowable.just(UserDetails(username, ArrayList()))
        } else {
            logger.debug { "Credentials are invalid" }
//            logger.debug { "Credentials are invalid: '${authenticationRequest.identity}' != '$username', '${authenticationRequest.secret}' != '$password'" }
            return Flowable.just(AuthenticationFailed())
        }
    }
}