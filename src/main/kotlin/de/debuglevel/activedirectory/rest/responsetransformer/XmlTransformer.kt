package de.debuglevel.activedirectory.rest.responsetransformer

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import mu.KotlinLogging
import spark.ResponseTransformer

/**
 * Transformer which converts objects to XML
 * Note: the responseTransformer parameter will be removed in Spark Kotlin and must be called explicitly.
 */
object XmlTransformer : ResponseTransformer {
    private val logger = KotlinLogging.logger {}

    private val xmlMapper = XmlMapper()

    init {
        xmlMapper.enable(SerializationFeature.INDENT_OUTPUT)
    }

    override fun render(model: Any?): String {
        return xmlMapper.writeValueAsString(model)
    }
}