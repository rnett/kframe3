package com.rnett.kframe.routing

actual abstract class RoutingDefinition : BaseRoutes()
actual class RouteInstance<T> actual constructor(
    actual val route: Route<T>,
    val data: T,
    actual val url: String
) {

    @JvmName("dataGetter")
    actual fun getData(): T = data
}