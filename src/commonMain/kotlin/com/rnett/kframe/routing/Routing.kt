package com.rnett.kframe.routing

import com.rnett.kframe.utils.PropNameHelper
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.reflect.KClass

data class Route<T : Any>(val page: PageDef<T>, val route: List<RoutePart>, val dataBuilder: UrlData.() -> T)

expect class RouteInstance<T : Any>(page: PageDef<T>, data: T, url: String){
    val page: PageDef<T>
    val url: String
    fun getData(): T
}

//TODO logging (w/ exception thrown)
private fun <T : Any> parseUrl(url: String, route: Route<T>): RouteInstance<T>? {

    val tailcard = url.substringAfter('?', "").substringBefore("#").split("&").filter { it.isNotBlank() }
        .map { it.split("=").let { it[0] to it[1] } }.toMap()
    val parts = url.substringAfter("://").substringAfter("/").substringBefore('?').split("/").filter { it.isNotBlank() }

    var urlParts = PartedURL(parts, tailcard)

    val urlParams = mutableMapOf<String, Any?>()
    val tailcardParams = mutableMapOf<String, Any?>()

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
        return RouteInstance(route.page, route.dataBuilder(UrlData(urlParams, tailcardParams)), url)
    else
        return null
}

abstract class BaseRoutes internal constructor() {
    private val routes = mutableListOf<Route<*>>()
    internal operator fun plusAssign(route: Route<*>) {
        //TODO check for duplicates w/o tailcard (or optional?) params
        //  more of a conflict check
        if (routes.any { it.route == route.route })
            error("Route already registered: ${route.route}")

        routes += route
    }

    fun tryParse(url: String): RouteInstance<*>? {
        routes.forEach {
            parseUrl(url, it)?.let { return it }
        }

        return null
    }

    @OptIn(ExperimentalContracts::class)
    inner class RoutingBuilder : RouteBuilder() {
        override fun <T> makeParam(name: String, location: ParamLocation, transform: (String) -> T) =
            RoutePart.Param(name, location, transform, null, this@BaseRoutes)

        override fun <T> makeOptionalParam(name: String, location: ParamLocation, transform: (String) -> T) =
            RoutePart.OptionalParam(name, location, transform, null, this@BaseRoutes)

        override fun <T> makeAnonymousParam(name: String, transform: (String) -> T) =
            RoutePart.AnonymousParam(name, transform, null, this@BaseRoutes)

        override fun makeStatic(name: String) = RoutePart.Static(name, null, this@BaseRoutes)
        override fun makeWildcard() = RoutePart.Wildcard(null, this@BaseRoutes)

        private var pageRegistered = false
        @RoutingDSL
        override operator fun <T : Any> PageDef<T>.invoke(dataBuilder: UrlData.() -> T): Route<T> {

            if (pageRegistered)
                error("Page already registered for index")

            val route = Route(this, listOf(), dataBuilder)

            routes += route

            pageRegistered = true
            return route
        }

        @RoutingDSL
        override operator fun PageDef<Unit>.invoke() = invoke { Unit }
    }

    @RoutingDSL
    fun routing(builder: RoutingBuilder.() -> Unit){
        contract {
            callsInPlace(builder, InvocationKind.EXACTLY_ONCE)
        }

        RoutingBuilder().builder()
    }
}

expect abstract class RoutingDefinition() : BaseRoutes

fun <T : Any> RoutingDefinition.pageDef(name: String, dataClass: KClass<T>) = PageDef(name, dataClass, this)
inline fun <reified T : Any> RoutingDefinition.pageDef(name: String) = pageDef(name, T::class)
inline fun <T : Any> RoutingDefinition.pageDef(dataClass: KClass<T>) = PropNameHelper { pageDef(it, dataClass) }
inline fun <reified T : Any> RoutingDefinition.pageDef() = PropNameHelper { pageDef<T>(it) }

fun <T : Any> RoutingDefinition.pageDef(name: String, dataClass: KClass<T>, toURL: (T) -> String) =
    ReactivePageDef(name, dataClass, this, toURL)

inline fun <reified T : Any> RoutingDefinition.pageDef(name: String, noinline toURL: (T) -> String) =
    pageDef(name, T::class, toURL)

inline fun <T : Any> RoutingDefinition.pageDef(dataClass: KClass<T>, noinline toURL: (T) -> String) =
    PropNameHelper { pageDef(it, dataClass, toURL) }

inline fun <reified T : Any> RoutingDefinition.pageDef(noinline toURL: (T) -> String) = PropNameHelper { pageDef<T>(it, toURL) }
