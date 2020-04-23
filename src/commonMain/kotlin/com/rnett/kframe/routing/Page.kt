package com.rnett.kframe.routing

import com.rnett.kframe.utils.urlParamDecode
import com.rnett.kframe.utils.urlParamEncode
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.*
import kotlin.math.absoluteValue
import kotlin.random.Random

/*
TODO reactiveity infered from route
    use serialization style compiler plugin, can set keys, location by annotating data class items.
    validation could be done during init (or in constructor?)
    then check that required params are present and of the right type when used/route is defined
 */

private val usedIds = mutableSetOf<Int>()
private fun newPageId(): Int {
    var id: Int
    do {
        id = Random.nextInt().absoluteValue
    } while (id in usedIds)

    return id
}

open class PageDef<T> internal constructor(val routing: RoutingDefinition) {
    private val id = newPageId()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PageDef<*>) return false

        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        return id
    }
}

abstract class Route<T>(val page: PageDef<T>, val route: List<RoutePart>, val routing: RoutingDefinition) {
    abstract fun parseData(urlData: UrlData, rawData: Map<String, String>): T
}

abstract class ReactiveRoute<T>(page: PageDef<T>, route: List<RoutePart>, routing: RoutingDefinition) :
    Route<T>(page, route, routing) {
    abstract fun toUrl(data: T): String
}

class JsonPageDef<T>(val serializer: KSerializer<T>, routing: RoutingDefinition) : PageDef<T>(routing)

val routeJson = Json {
    encodeDefaults = false
}

/**
 * @param renames rename url parameters to different serialized names.  url name -> serialized name
 */
class JsonRoute<T>(
    val jsonPage: JsonPageDef<T>,
    route: List<RoutePart>,
    routing: RoutingDefinition,
    val renames: Map<String, String>
) : ReactiveRoute<T>(jsonPage, route, routing) {

    private val reverseRenames by lazy { renames.toList().map { it.second to it.first }.toMap() }

    init {
        check(renames.values.toSet().size == renames.keys.size) { "Both sides of renames must be unique (map must be reversible)" }
    }

    private fun doRenames(flat: Map<String, String>): Map<String, String> = flat.mapKeys { (key, _) ->
        renames[key] ?: key
    }

    private fun undoRenames(flat: Map<String, String>): Map<String, String> = flat.mapKeys { (key, _) ->
        reverseRenames[key] ?: key
    }

    //TODO how to handle optional params?   For singletons, can I fill a single mandatory and leave optionals?  I don't think so, as then the optional would never have been used
    //  I think its fine
    override fun toUrl(data: T): String {
        val tree = routeJson.toJson(jsonPage.serializer, data)
        when (tree) {
            is JsonPrimitive -> {
                val params = route.filterIsInstance<NamedParam>()
                if (params.size > 1)
                    error("Multiple params for primitive data.  Params: $params, but data was $data")

                return if (data == Unit) {
                    if (params.isNotEmpty())
                        error("Defined params for Unit data.")

                    route.fold(PartedUrl()) { acc, it -> acc + it.toUrlParts(mapOf()) }.toUrl()
                } else {
                    if (params.isEmpty())
                        error("No params for non-Unit data")

                    val flat = undoRenames(mapOf(params.first().name to tree.content))
                    route.fold(PartedUrl()) { acc, it -> acc + it.toUrlParts(flat) }.toUrl()
                }
            }
            is JsonObject -> {
                val flat = undoRenames(tree.flatten())
                return route.fold(PartedUrl()) { acc, it -> acc + it.toUrlParts(flat) }.toUrl()
            }
            is JsonArray -> {
                val params = route.filterIsInstance<NamedParam>()
                when {
                    params.size > 1 -> error("Can't handle base array type data for multiple params")
                    params.isEmpty() -> error("No params for array data")
                    else -> {
                        val flat = if (tree.all { it is JsonPrimitive })
                            undoRenames(mapOf(params.first().name to tree.joinToString(",") { urlParamEncode(it.content) }))
                        else
                            throw JsonFlatteningError("Can't include non-primitive list in url")
                        return route.fold(PartedUrl()) { acc, it -> acc + it.toUrlParts(flat) }.toUrl()
                    }
                }
            }
        }
    }

    override fun parseData(urlData: UrlData, rawData: Map<String, String>): T {
        val data = doRenames(rawData)
        // try for singletons
        if (data.size == 1) {
            val (key, value) = data.entries.first()
            if ('-' !in key) {
                try {
                    return if (',' in value)
                        routeJson.parse(
                            jsonPage.serializer,
                            value.split(",").joinToString(", ", "[", "]") { urlParamDecode(it) })
                    else
                        routeJson.parse(jsonPage.serializer, urlParamDecode(value))
                } catch (e: SerializationException) {

                }
            }
        }

        return routeJson.fromJson(jsonPage.serializer, unFlatten(data))
    }
}
