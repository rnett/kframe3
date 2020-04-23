package com.rnett.kframe.routing

import kotlinx.serialization.KSerializer
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract


expect class RouteInstance<T>(route: Route<T>, data: T, url: String) {
    val route: Route<T>
    val url: String
    fun getData(): T
}

//TODO logging (w/ exception thrown)
private fun <T> parseUrl(url: String, route: Route<T>): RouteInstance<T>? {

    val tailcard = url.substringAfter('?', "").substringBefore("#").split("&").filter { it.isNotBlank() }
        .map { it.split("=").let { it[0] to it[1] } }.toMap()
    val parts = url.substringAfter("://").substringAfter("/").substringBefore('?').split("/").filter { it.isNotBlank() }

    var urlParts = PartedUrl(parts, tailcard)

    val urlParams = mutableMapOf<String, Any?>()
    val tailcardParams = mutableMapOf<String, Any?>()
    val rawParams = mutableMapOf<String, String>()

    route.route.forEach {
        val result = it.parse(urlParts)
        when (result) {
            is RouterOption.Fail -> return null
            is RouterOption.Success -> urlParts = result.newUrl
            is RouterOption.AddParam<*> -> run {
                if (result.tailcard)
                    tailcardParams[result.name] = result.value
                else
                    urlParams[result.name] = result.value

                rawParams[result.name] = result.raw

                urlParts = result.newUrl
            }
        }

        try {
            it.handler?.invoke(UrlData(urlParams, tailcardParams))
        } catch (e: Exception){
            return null
        }
    }

    if(urlParts.empty)
        return RouteInstance(route, route.parseData(UrlData(urlParams, tailcardParams), rawParams), url)
    else
        return null
}

abstract class BaseRoutes internal constructor() {
    internal val routes = mutableListOf<Route<*>>()

    internal operator fun plusAssign(route: Route<*>) {
        //TODO check for duplicates w/o tailcard (or optional?) params
        //  more of a conflict check
        if (routes.any { it.route == route.route })
            error("Route already registered: ${route.route}")

        routes += route
    }

    operator fun contains(route: Route<*>) = route in routes

    fun replace(route: Route<*>, newRoute: Route<*>) {
        routes.removeAll { it.route == route.route }
        routes += newRoute
    }

    fun tryParse(url: String): RouteInstance<*>? {
        routes.forEach {
            parseUrl(url, it)?.let { return it }
        }

        return null
    }

    @RoutingDSL
    inline infix fun <T> Route<T>.asReactive(crossinline toUrl: (T) -> String): ReactiveRoute<T> {
        val newRoute = object : ReactiveRoute<T>(this.page, this.route, this.routing) {
            override fun parseData(urlData: UrlData, rawData: Map<String, String>): T =
                this@asReactive.parseData(urlData, rawData)

            override fun toUrl(data: T): String = toUrl(data)
        }

        replace(this, newRoute)
        return newRoute
    }
}

@RoutingDSL
inline fun RoutingDefinition.routing(builder: RoutingBuilder.() -> Unit) {
    contract {
        callsInPlace(builder, InvocationKind.EXACTLY_ONCE)
    }

    RoutingBuilder(this).builder()
}

@OptIn(ExperimentalContracts::class)
class RoutingBuilder(val routing: RoutingDefinition) : RouteBuilder() {
    override fun <T> makeParam(name: String, location: ParamLocation, transform: (String) -> T) =
        RoutePart.Param(name, location, transform, null, routing)

    override fun <T> makeOptionalParam(name: String, location: ParamLocation, transform: (String) -> T) =
        RoutePart.OptionalParam(name, location, transform, null, routing)

    override fun <T> makeAnonymousParam(name: String, transform: (String) -> T) =
        RoutePart.AnonymousParam(name, transform, null, routing)

    override fun makeStatic(name: String) = RoutePart.Static(name, null, routing)
//        override fun makeWildcard() = RoutePart.Wildcard(null, this@BaseRoutes)

    private var pageRegistered = false

    @PublishedApi
    internal fun <T> addRoute(route: Route<T>) {
        if (pageRegistered)
            error("Page already registered for this route")

        routing.routes += route

        pageRegistered = true
    }

    @RoutingDSL
    final override inline operator fun <T> PageDef<T>.invoke(crossinline dataBuilder: UrlData.() -> T): Route<T> {
        val route = object : Route<T>(this, listOf(), routing) {
            override fun parseData(urlData: UrlData, rawData: Map<String, String>): T = dataBuilder(urlData)

        }

        addRoute(route)
        return route
    }

    @RoutingDSL
    final override inline operator fun <T> PageDef<T>.invoke(
        crossinline toUrl: (T) -> String,
        crossinline dataBuilder: UrlData.() -> T
    ): ReactiveRoute<T> {
        val route = object : ReactiveRoute<T>(this, listOf(), routing) {
            override fun parseData(urlData: UrlData, rawData: Map<String, String>): T = dataBuilder(urlData)
            override fun toUrl(data: T): String = toUrl(data)
        }

        addRoute(route)
        return route
    }

    @RoutingDSL
    override operator fun PageDef<Unit>.invoke(): ReactiveRoute<Unit> {
        return invoke("")
    }

    @RoutingDSL
    override operator fun PageDef<Unit>.invoke(url: String): ReactiveRoute<Unit> {
        return invoke({ url }) { Unit }
    }

    @RoutingDSL
    override operator fun <T> JsonPageDef<T>.invoke(renames: Map<String, String>): JsonRoute<T> {
        val route = JsonRoute(this, listOf(), routing, renames)
        addRoute(route)
        return route
    }
}

expect abstract class RoutingDefinition() : BaseRoutes

fun <T> RoutingDefinition.pageDef() = PageDef<T>(this)
fun RoutingDefinition.unitPageDef() = pageDef<Unit>()
fun <T> RoutingDefinition.jsonPageDef(serializer: KSerializer<T>) = JsonPageDef<T>(serializer, this)
