package com.rnett.kframe.routing

import kotlin.reflect.KClass
open class PageDef<T : Any> internal constructor(val name: String, val dataClass: KClass<T>, val routing: Routing) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as PageDef<*>

        if (name != other.name) return false
        if (dataClass != other.dataClass) return false
        if (routing != other.routing) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + dataClass.hashCode()
        result = 31 * result + routing.hashCode()
        return result
    }
}

class ReactivePageDef<T : Any>(
    name: String,
    dataClass: KClass<T>,
    routing: Routing,
    val toURL: (T) -> String
) : PageDef<T>(name, dataClass, routing) {
}