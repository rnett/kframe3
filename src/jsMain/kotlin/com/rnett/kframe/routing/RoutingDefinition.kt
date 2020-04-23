package com.rnett.kframe.routing

import com.rnett.kframe.binding.watchLateinitWrapper
import com.rnett.kframe.binding.watchWrapper
import kotlin.browser.window
import kotlin.reflect.KProperty


/**
 * @param url Accepts full url or pathname.  Prefixes with window.location.origin if it doesn't already start with it
 */
fun prefixUrl(url: String): String {
    val urlStart = window.location.origin

    return if (url.startsWith(urlStart)) url else urlStart + url
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
    private fun setUrl(url: String){

        window.history.pushState(null, "", prefixUrl(url))
    }

    fun <T: Any> setUrlIfActive(pageDef: ReactivePageDef<T>, data: T): Boolean {
        return if(currentPage.page == pageDef) {
            setUrl(pageDef.toURL(data))
            true
        } else
            false
    }

    fun <T: Any> setUrl(pageDef: ReactivePageDef<T>, data: T){
        if(currentPage.page == pageDef)
            setUrl(pageDef.toURL(data))
        else
            error("Current page is not $pageDef, was ${currentPage.page}")
    }

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
    fun <T : Any> goto(pageDef: PageDef<T>, data: T, url: String) {
        val fullUrl = prefixUrl(url)
        currentPage = RouteInstance(pageDef, data, fullUrl)
        setUrl(fullUrl)
    }

    fun <T : Any> goto(pageDef: ReactivePageDef<T>, data: T) {
        goto(pageDef, data, pageDef.toURL(data))
    }
}

//TODO delegates for this (from watch)
fun <T: Any> ReactivePageDef<T>.setUrl(data: T) = routing.setUrl(this, data)
fun <T: Any> ReactivePageDef<T>.setUrlIfActive(data: T) = routing.setUrlIfActive(this, data)


/**
 * @param url Accepts full url or pathname.  Prefixes with window.location.origin if it doesn't already start with it
 */
fun <T : Any> PageDef<T>.goto(data: T, url: String) {
    routing.goto(this, data, url)
}

fun <T : Any> ReactivePageDef<T>.goto(data: T) {
    routing.goto(this, data)
}

actual class RouteInstance<T : Any> actual constructor(
    actual val page: PageDef<T>,
    data: T,
    actual val url: String
) {
    val dataWatcher by lazy{ watchWrapper(data) }

    actual fun getData(): T = dataWatcher.getValue()

    operator fun provideDelegate(thisRef: Any?, property: KProperty<*>) = dataWatcher

    init {
        if(page is ReactivePageDef){
            dataWatcher.onSet {
                page.setUrlIfActive(it)
            }
        }
    }
}