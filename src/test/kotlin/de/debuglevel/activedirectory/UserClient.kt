package de.debuglevel.activedirectory

import io.micronaut.http.MediaType
import io.micronaut.http.annotation.*
import io.micronaut.http.client.annotation.Client
import io.reactivex.Single
import javax.validation.constraints.NotBlank

@Client("/users")
interface UserClient {
    @Get("/{username}")
    fun getOne(@NotBlank username: String, @Header authorization: String): Single<UserResponse>

    @Get("/")
    @Produces(MediaType.APPLICATION_JSON)
    fun getJsonList(@Header authorization: String): Set<UserResponse>

    @Get("/")
    @Produces(MediaType.APPLICATION_XML)
    fun getXmlList(@Header authorization: String): Set<UserResponse>
}