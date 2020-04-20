package com.rnett.kframe.routing

import com.rnett.kframe.binding.watchLateinitWrapper
import kotlin.browser.window

actual abstract class Routing : BaseRoutes() {
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
    fun gotoUrl(url: String): Boolean {

        val urlStart = window.location.origin

        val fullUrl = if (url.startsWith(urlStart)) url else urlStart + url

        currentPage = tryParse(fullUrl) ?: return false
        return true
    }


    /**
     * @param url Accepts full url or pathname.  Prefixes with window.location.origin if it doesn't already start with it
     */
    fun <T : Any> goto(pageDef: PageDef<T>, data: T, url: String) {

        val urlStart = window.location.origin

        val fullUrl = if (url.startsWith(urlStart)) url else urlStart + url

        currentPage = RouteInstance(pageDef, data, fullUrl)
    }

    fun <T : Any> goto(pageDef: ReactivePageDef<T>, data: T) {
        goto(pageDef, data, pageDef.toUrl(data))
    }
}


/**
 * @param url Accepts full url or pathname.  Prefixes with window.location.origin if it doesn't already start with it
 */
fun <T : Any> PageDef<T>.goto(data: T, url: String) {
    routing.goto(this, data, url)
}

fun <T : Any> ReactivePageDef<T>.goto(data: T) {
    routing.goto(this, data)
}