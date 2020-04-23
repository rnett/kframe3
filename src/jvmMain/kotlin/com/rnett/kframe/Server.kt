package com.rnett.kframe

import com.rnett.kframe.routing.flatten
import com.rnett.kframe.routing.unFlatten
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
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

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
data class Test(val test: Int, val test2: Test2)
@Serializable
data class Test2(val a: String, val b: List<Int>)

fun main() {
    val original = Test(3, Test2("str", listOf(1, 2, 3, 4, 5)))
    val json = Json.toJson(Test.serializer(), original) as JsonObject
    val flat = json.flatten()
    val test = unFlatten(flat)

    val ex = Json.Default.fromJson(Test.serializer(), test)

    val equal = test == json
    val equal2 = original == ex

    println()
}
