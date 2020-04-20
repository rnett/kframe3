package com.rnett.kframe.dom.providers

import org.w3c.dom.asList


interface StyleProvider {
    fun setStyle(key: String, value: String, important: Boolean)
    fun getStyle(key: String): String?
    fun removeStyle(key: String)
    fun hasStyle(key: String): Boolean

    fun setImportance(key: String, important: Boolean)
    fun setValue(key: String, value: String)
    fun isImportant(key: String): Boolean?
    fun toHTML(pretty: Boolean = true): String
    fun map(): Map<String, Pair<String, Boolean>>
    val save: Boolean
}

internal class VirtualStyleProvider(override val save: Boolean) : StyleProvider, SingleProvider<StyleProvider>, VirtualProvider {
    val raw = mutableMapOf<String, Pair<String, Boolean>>()

    override fun setStyle(key: String, value: String, important: Boolean) {
        raw[key] = Pair(value, important)
    }

    override fun getStyle(key: String) = raw[key]?.first

    override fun removeStyle(key: String) {
        raw.remove(key)
    }

    override fun hasStyle(key: String) = key in raw
    override fun setImportance(key: String, important: Boolean) {
        val p = raw.remove(key)
        if (p != null)
            raw[key] = Pair(p.first, important)
        else
            error("Style $key is not set")
    }

    override fun setValue(key: String, value: String) {
        val p = raw.remove(key)
        if (p != null)
            raw[key] = Pair(value, p.second)
        else
            raw[key] = Pair(value, false)
    }

    override fun isImportant(key: String) = raw[key]?.second

    override fun toHTML(pretty: Boolean) =
        raw.entries.joinToString(if (pretty) ";\n" else "; ") { "${it.key}: ${it.value.first}${if (it.value.second) " !important" else ""}" }
            .let {
                if (it.isNotBlank())
                    "$it;"
                else
                    it
            }

    override fun map(): Map<String, Pair<String, Boolean>> {
        return raw
    }

    override fun copyFrom(other: StyleProvider) {
        raw.clear()
        raw.putAll(other.map())
    }
}

internal class RealizedStyleProvider(private val existenceProvider: RealizedExistenceProvider) : StyleProvider, SingleProvider<StyleProvider>,
    RealizedProvider {
    private inline val underlying get() = existenceProvider.underlying

    override val save = false

    override fun setStyle(key: String, value: String, important: Boolean) {
        underlying.style.setProperty(value, value, if (important) "important" else "")
    }

    override fun getStyle(key: String) =
        underlying.style.getPropertyValue(key).let { if (it.isBlank()) null else it }

    override fun removeStyle(key: String) {
        underlying.style.removeProperty(key)
    }

    override fun hasStyle(key: String) = !underlying.style.getPropertyValue(key).isBlank()

    override fun setImportance(key: String, important: Boolean) {
        underlying.style.let {
            it.setProperty(key, it.getPropertyValue(key), if (important) "important" else "")
        }
    }

    override fun setValue(key: String, value: String) {
        underlying.style.setProperty(key, value = value)
    }

    override fun isImportant(key: String) = underlying.style.getPropertyPriority(key) == "important"

    override fun toHTML(pretty: Boolean): String {
        val text = underlying.style.cssText
        return if (pretty)
            text.replace("; ", ";\n")
        else text
    }

    override fun map(): Map<String, Pair<String, Boolean>> {
        return underlying.style.asList().associateWith {
            underlying.style.getPropertyValue(it) to isImportant(it)
        }.filterValues { it.first.isNotBlank() }
    }

    override fun copyFrom(other: StyleProvider) {
        val thisMap = map()
        val otherMap = other.map()
        otherMap.forEach { (key, value) ->
            if (key !in thisMap || thisMap[key] != value) {

                if (value.first != thisMap[key]?.first)
                    setValue(key, value.first)

                if (value.second != thisMap[key]?.second)
                    setImportance(key, value.second)
            }
        }
        thisMap.keys.forEach {
            if (it !in otherMap)
                this.removeStyle(it)
        }
    }
}

internal class StyleProviderWrapper(initial: StyleProvider): ExistenceAttachable, StyleProvider{
    private var provider: StyleProvider = initial

    override fun attach(provider: RealizedExistenceProvider) {
        this.provider = RealizedStyleProvider(provider).also { it.copyFrom(this.provider) }
    }

    override fun detach() {
        provider = VirtualStyleProvider(false).also { it.copyFrom(provider) }
    }

    override fun setStyle(key: String, value: String, important: Boolean) {
        provider.setStyle(key, value, important)
    }

    override fun getStyle(key: String): String? {
        return provider.getStyle(key)
    }

    override fun removeStyle(key: String) {
        provider.removeStyle(key)
    }

    override fun hasStyle(key: String): Boolean {
        return provider.hasStyle(key)
    }

    override fun setImportance(key: String, important: Boolean) {
        provider.setImportance(key, important)
    }

    override fun setValue(key: String, value: String) {
        provider.setValue(key, value)
    }

    override fun isImportant(key: String): Boolean? {
        return provider.isImportant(key)
    }

    override fun toHTML(pretty: Boolean): String {
        return provider.toHTML(pretty)
    }

    override fun map(): Map<String, Pair<String, Boolean>> {
        return provider.map()
    }

    override val save: Boolean
        get() = provider.save
}