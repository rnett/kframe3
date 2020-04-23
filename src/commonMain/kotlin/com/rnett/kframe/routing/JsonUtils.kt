package com.rnett.kframe.routing

import com.rnett.kframe.utils.urlParamDecode
import com.rnett.kframe.utils.urlParamEncode
import kotlinx.serialization.json.*


class JsonFlatteningError(message: String, cause: JsonFlatteningError? = null) : IllegalStateException(message, cause)

fun JsonObject.flatten(): Map<String, String> {
    val flat = mutableMapOf<String, String>()
    this.forEach { (key, value) ->
        if ('-' in key)
            error("Can't have '-' in keys, used to define sub-objects.  Key was $key")

        when (value) {
            is JsonPrimitive -> flat[key] = urlParamEncode(value.toString())

            is JsonObject -> {
                val subFlat = try {
                    value.flatten()
                } catch (e: JsonFlatteningError) {
                    throw JsonFlatteningError("Could not flatten $key", e)
                }

                subFlat.mapKeys { "${key}-${it.key}" }.forEach { (subKey, subValue) ->
                    if (subKey in flat)
                        throw JsonFlatteningError("Key conflict: got key $subKey from $key, but it was already present in the flattened object")
                    else
                        flat[subKey] = subValue
                }
            }

            is JsonArray -> {
                if (value.all { it is JsonPrimitive })
                    flat[key] = value.joinToString(",") { urlParamEncode(it.content) }
                else
                    throw JsonFlatteningError("Can't include non-primitive list in url from key $key")
            }
        }
    }

    return flat
}

fun toPrimitive(value: String): JsonPrimitive {
    return if (value.startsWith("\"") && value.endsWith("\""))
        JsonPrimitive(value.substring(1, value.length - 1))
    else {
        when {
            value == "null" -> JsonNull
            value == "true" -> JsonPrimitive(true)
            value == "false" -> JsonPrimitive(false)
            value.toIntOrNull() != null -> JsonPrimitive(value.toInt())
            else -> JsonPrimitive(value.toDouble())
        }

    }
}

fun unFlatten(flat: Map<String, String>): JsonObject {
    val fields = flat.keys.map { it.substringBefore("-") }.toSet()
    val primitiveFields = flat.keys.filter { '-' !in it }.toSet()

    val primitives = primitiveFields.associateWith { key ->
        val value = flat.getValue(key)
        if (',' in value)
            JsonArray(value.split(',').map { toPrimitive(urlParamDecode(it)) })
        else
            toPrimitive(urlParamDecode(value))
    }
    val objects = (fields - primitiveFields).associateWith { key ->
        val subFlat = flat.filterKeys { it.startsWith("$key-") }.mapKeys { it.key.substringAfter('-') }
        unFlatten(subFlat)
    }

    return JsonObject(primitives + objects)
}