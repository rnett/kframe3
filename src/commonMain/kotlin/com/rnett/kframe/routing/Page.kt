package com.rnett.kframe.routing

import kotlin.reflect.KClass

/*
TODO reactiveity infered from route
    use serialization style compiler plugin, can set keys, location by annotating data class items.
    validation could be done during init (or in constructor?)
    then check that required params are present and of the right type when used/route is defined
 */

open class PageDef<T : Any> internal constructor(val name: String, val dataClass: KClass<T>, val routing: RoutingDefinition) {
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
    routing: RoutingDefinition,
    val toURL: (T) -> String
) : PageDef<T>(name, dataClass, routing) {
}