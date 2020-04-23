package com.rnett.kframe

import io.ktor.application.Application
import io.ktor.application.install
import io.ktor.features.CORS
import io.ktor.features.Compression
import io.ktor.features.DefaultHeaders
import io.ktor.features.gzip
import io.ktor.http.content.resource
import io.ktor.http.content.static
import io.ktor.routing.routing
import kotlinx.serialization.Serializable

fun Application.server(){
    install(CORS) {
        anyHost()
    }

    install(Compression) {
        gzip()
    }

    install(DefaultHeaders)

    routing {
        static("js") {
            resource("kframe.js")
            resource("kframe.js.map")
        }

        allRoutes(Routing, "https://cdn.jsdelivr.net/npm/kotlin@1.3.71/kotlin.js", "/js/kframe.js")
    }
}

@Serializable
data class Test(val first: Int, val second: String)

fun main() {
    val x = Test.serializer().descriptor
    println(x)
    println()
}
