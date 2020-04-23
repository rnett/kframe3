package com.rnett.kframe.routing

actual abstract class RoutingDefinition : BaseRoutes()
actual class RouteInstance<T : Any> actual constructor(
    actual val page: PageDef<T>,
    val data: T,
    actual val url: String
) {

    @JvmName("dataGetter")
    actual fun getData(): T = data
}