package com.rnett.kframe.routing

import com.rnett.kframe.binding.watch.WatcherId
import com.rnett.kframe.binding.watch.watchLateinitWrapper
import com.rnett.kframe.binding.watch.watchWrapper
import kotlin.browser.window
import kotlin.reflect.KProperty


/**
 * @param url Accepts full url or pathname.  Prefixes with window.location.origin if it doesn't already start with it
 */
fun prefixUrl(url: String): String {
    val urlStart = window.location.origin

    return if (url.startsWith(urlStart)) url else urlStart + "/" + url.trimStart('/')
}

actual abstract class RoutingDefinition : BaseRoutes() {
    val currentPageWatcher = watchLateinitWrapper<RouteInstance<*>>()
    var currentPage by currentPageWatcher

    val currentUrl: String
        get() = currentPage.url

    fun init() {
        if (!gotoUrl(window.location.href))
            error("Page not found")
    }

    /**
     * @param url Accepts full url or pathname.  Prefixes with window.location.origin if it doesn't already start with it
     */
    private fun setUrl(url: String) {
        window.history.pushState(null, "", prefixUrl(url))
    }

    fun <T> setUrlIfPageActive(route: ReactiveRoute<T>, data: T): Boolean {
        return if (currentPage.route.page == route.page) {
            setUrl(route.toUrl(data))
            true
        } else
            false
    }

    fun <T> setUrlIfRouteActive(route: ReactiveRoute<T>, data: T): Boolean {
        return if (currentPage.route == route) {
            setUrl(route.toUrl(data))
            true
        } else
            false
    }

    fun <T> setUrl(route: ReactiveRoute<T>, data: T) {
        if (setUrlIfRouteActive(route, data))
            return
        else
            error("Route $this is not active")
    }

    fun isActive(route: Route<*>) = currentPage.route == route
    fun isActive(page: PageDef<*>) = currentPage.route.page == page

    /**
     * @param url Accepts full url or pathname.  Prefixes with window.location.origin if it doesn't already start with it
     */
    fun gotoUrl(url: String): Boolean {
        val fullUrl = prefixUrl(url)
        currentPage = tryParse(fullUrl) ?: return false
        setUrl(fullUrl)
        return true
    }


    /**
     * @param url Accepts full url or pathname.  Prefixes with window.location.origin if it doesn't already start with it
     */
    fun <T> goto(route: Route<T>, data: T, url: String) {
        val fullUrl = prefixUrl(url)
        currentPage = RouteInstance(route, data, fullUrl)
        setUrl(fullUrl)
    }

    fun <T> goto(route: ReactiveRoute<T>, data: T) {
        goto(route, data, route.toUrl(data))
    }
}

fun <T> ReactiveRoute<T>.setUrl(data: T) = routing.setUrl(this, data)
fun <T> ReactiveRoute<T>.setUrlIfRouteActive(data: T) = routing.setUrlIfRouteActive(this, data)
fun <T> ReactiveRoute<T>.setUrlIfPageActive(data: T) = routing.setUrlIfPageActive(this, data)

/**
 * @param url Accepts full url or pathname.  Prefixes with window.location.origin if it doesn't already start with it
 */
fun <T> Route<T>.goto(data: T, url: String) {
    routing.goto(this, data, url)
}

fun <T> ReactiveRoute<T>.goto(data: T) {
    routing.goto(this, data)
}

fun ReactiveRoute<Unit>.goto() {
    routing.goto(this, Unit)
}

actual class RouteInstance<T> actual constructor(
    actual val route: Route<T>,
    data: T,
    actual val url: String
) {
    val dataWatcher by lazy { watchWrapper(data) }

    actual fun getData(): T = dataWatcher.value

    operator fun provideDelegate(thisRef: Any?, property: KProperty<*>) = dataWatcher

    // lifecycle control
    private lateinit var routeListener: WatcherId<T>

    init {
        if (route is ReactiveRoute) {
            routeListener = dataWatcher.onSet {
                route.setUrlIfRouteActive(it)
            }
        }
    }
}