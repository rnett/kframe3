package com.rnett.kframe

import com.rnett.kframe.routing.RoutingDefinition
import io.ktor.application.Application
import io.ktor.application.ApplicationCallPipeline
import io.ktor.application.call
import io.ktor.http.ContentType
import io.ktor.request.uri
import io.ktor.response.respondText

fun Application.allRoutes(routing: RoutingDefinition, kotlinUrl: String, kframeUrl: String) {
    val index = basePage(kotlinUrl, kframeUrl)

    intercept(ApplicationCallPipeline.Call) {
        val url = call.request.uri

        //TODO cache documents per page + data?  preload certain urls?
        if (routing.tryParse(url) != null)
            call.respondText(index, ContentType.Text.Html)
    }
}

fun basePage(kotlinUrl: String, kframeUrl: String): String =
    """<html>
    <head>
        <script src="$kotlinUrl" type="text/javascript"></script>
        <script src="$kframeUrl" type="text/javascript"></script>
    </head>
    <body>
    </body>
</html>""".trimIndent()