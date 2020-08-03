package de.debuglevel.activedirectory.computer

import io.micronaut.http.MediaType
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Header
import io.micronaut.http.annotation.Produces
import io.micronaut.http.client.annotation.Client
import io.reactivex.Single
import javax.validation.constraints.NotBlank

@Client("/computers")
interface ComputerClient {
    @Get("/{name}")
    fun getOne(@NotBlank name: String, @Header authorization: String): Single<ComputerResponse>

    @Get("/")
    @Produces(MediaType.APPLICATION_JSON)
    fun getJsonList(@Header authorization: String): Set<ComputerResponse>

    @Get("/")
    @Produces(MediaType.APPLICATION_XML)
    fun getXmlList(@Header authorization: String): Set<ComputerResponse>
}