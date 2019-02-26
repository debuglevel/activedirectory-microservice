package de.debuglevel.activedirectory.rest

import com.natpryce.konfig.Key
import com.natpryce.konfig.intType
import spark.Service
import spark.Spark
import java.io.IOException
import java.net.ServerSocket

object SparkTestUtils {
    fun awaitShutdown() {
        // HACK: nasty workaround to close Spark after test and ensure that it's down when we exit the method (stopping is done in a Thread).
        // But even this does not necessarily catch all race conditions
        Spark.stop()
        while (isLocalPortInUse(Configuration.configuration[Key("port", intType)])) {
            Thread.sleep(100)
        }
        while (sparkIsInitialized()) {
            Thread.sleep(100)
        }
    }

    private fun sparkIsInitialized(): Boolean {
        // HACK: even nastier workaround to access the internals of Spark to check if the "initialized" flag is already set to false.
        val sparkClass = Spark::class.java
        val getInstanceMethod = sparkClass.getDeclaredMethod("getInstance")
        getInstanceMethod.isAccessible = true
        val service = getInstanceMethod.invoke(null) as Service

        val serviceClass = service::class.java
        val initializedField = serviceClass.getDeclaredField("initialized")
        initializedField.isAccessible = true
        val initialized = initializedField.get(service) as Boolean

        return initialized
    }

    private fun isLocalPortInUse(port: Int): Boolean {
        return try {
            // ServerSocket try to open a LOCAL port
            ServerSocket(port).close()
            // local port can be opened, it's available
            false
        } catch (e: IOException) {
            // local port cannot be opened, it's in use
            true
        }
    }
}