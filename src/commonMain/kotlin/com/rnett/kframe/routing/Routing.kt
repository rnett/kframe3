package com.rnett.kframe.routing

import com.rnett.kframe.utils.PropNameHelper
import kotlin.reflect.KClass

data class Route<T : Any>(val page: PageDef<T>, val route: List<RoutePart>, val dataBuilder: DataBuilder.() -> T)

data class RouteInstance<T : Any>(val page: PageDef<T>, val data: T, val url: String)

private fun <T : Any> parseUrl(url: String, route: Route<T>): RouteInstance<T>? {

    val tailcard = url.substringAfter('?', "").substringBefore("#").split("&").filter { it.isNotBlank() }
        .map { it.split("=").let { it[0] to it[1] } }.toMap()
    val parts = url.substringAfter("://").substringAfter("/").substringBefore('?').split("/").filter { it.isNotBlank() }

    var urlParts = PartedURL(parts, tailcard)

    val urlParams = mutableMapOf<String, Any?>()
    val tailcardParams = mutableMapOf<String, Any?>()

    route.route.forEach {
        val result = it.handle(urlParts)
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
    }

    return RouteInstance(route.page, route.dataBuilder(DataBuilder(urlParams, tailcardParams)), url)
}

abstract class BaseRoutes internal constructor() : RouteBuilder() {
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


    override fun <T> makeParam(name: String, location: ParamLocation, transform: (String) -> T) =
        RoutePart.Param(name, location, transform, null, this)

    override fun <T> makeOptionalParam(name: String, location: ParamLocation, transform: (String) -> T) =
        RoutePart.OptionalParam(name, location, transform, null, this)

    override fun <T> makeAnonymousParam(name: String, transform: (String) -> T) =
        RoutePart.AnonymousParam(name, transform, null, this)

    override fun makeStatic(name: String) = RoutePart.Static(name, null, this)
    override fun makeWildcard() = RoutePart.Wildcard(null, this)
}

expect abstract class Routing() : BaseRoutes

fun <T : Any> Routing.pageDef(name: String, dataClass: KClass<T>) = PageDef(name, dataClass, this)
inline fun <reified T : Any> Routing.pageDef(name: String) = pageDef(name, T::class)
inline fun <T : Any> Routing.pageDef(dataClass: KClass<T>) = PropNameHelper { pageDef(it, dataClass) }
inline fun <reified T : Any> Routing.pageDef() = PropNameHelper { pageDef<T>(it) }

fun <T : Any> Routing.pageDef(name: String, dataClass: KClass<T>, toURL: (T) -> String) =
    ReactivePageDef(name, dataClass, this, toURL)

inline fun <reified T : Any> Routing.pageDef(name: String, noinline toURL: (T) -> String) =
    pageDef(name, T::class, toURL)

inline fun <T : Any> Routing.pageDef(dataClass: KClass<T>, noinline toURL: (T) -> String) =
    PropNameHelper { pageDef(it, dataClass, toURL) }

inline fun <reified T : Any> Routing.pageDef(noinline toURL: (T) -> String) = PropNameHelper { pageDef<T>(it, toURL) }

object MyRoutes : Routing() {
    val page by pageDef<Int>()
    val home by pageDef<Unit>()

    val pageRoute: Route<Int>
    val homeRoute: Route<Unit>

    init {
        static("home") {
            urlParam("page", { it.toInt() }) { page ->
                pageRoute = this@MyRoutes.page { +page }
            }

            homeRoute = home()
        }
    }
}