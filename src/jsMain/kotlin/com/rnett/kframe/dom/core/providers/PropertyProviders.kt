package com.rnett.kframe.dom.core.providers

import org.w3c.dom.HTMLElement

interface PropertyProvider {
    fun getValue(key: String): String?
    fun setValue(key: String, value: String?)
    val original: HTMLElement?
}

private fun getJsProperty(obj: Any, key: String): String? {
    val dyn = obj.asDynamic()[key]

    return dyn as? String? ?: (dyn as? Int?)?.toString() ?: (dyn as? Double)?.toString() ?: (dyn as? Number)?.toString()
}

private fun setJsProperty(obj: Any, key: String, value: Any?) {
    if (value == null) {
        js("delete obj[key]")
    } else
        obj.asDynamic()[key] = value
}

internal class VirtualPropertyProvider(override val original: HTMLElement?) : PropertyProvider,
    SingleProvider<VirtualPropertyProvider>, VirtualProvider {
    internal val properties = mutableMapOf<String, String>()
    internal val updatedProperties = mutableSetOf<String>()

    override fun getValue(key: String): String? {
        properties[key]?.let { return it }

        if (key !in updatedProperties && original != null)
            return getJsProperty(original, key)

        return null
    }

    override fun setValue(key: String, value: String?) {
        updatedProperties += key
        if (value == null) {
            properties.remove(key)
        } else
            properties[key] = value
    }

    fun map(): Map<String, String> = properties

    override fun copyFrom(other: VirtualPropertyProvider) {
        properties.clear()
        properties.putAll(other.map())
    }
}

internal class RealizedPropertyProvider(private val existenceProvider: RealizedExistenceProvider) : PropertyProvider,
    RealizedProvider, SingleProvider<VirtualPropertyProvider> {
    private inline val underlying get() = existenceProvider.underlying

    override val original: HTMLElement = underlying

    override fun getValue(key: String) = getJsProperty(underlying, key)

    override fun setValue(key: String, value: String?) =
        setJsProperty(underlying, key, value)

    override fun copyFrom(other: VirtualPropertyProvider) {
        other.properties.forEach { (key, value) ->
            if (getValue(key) != value)
                setValue(key, value)
        }

        other.updatedProperties.forEach {
            if (other.properties[it] == null)
                setValue(it, null)
        }
    }
}

internal class PropertyProviderWrapper(initial: PropertyProvider) : PropertyProvider, ExistenceAttachable {
    private var provider: PropertyProvider = initial
    override fun getValue(key: String): String? = provider.getValue(key)

    override fun setValue(key: String, value: String?) = provider.setValue(key, value)

    override val original: HTMLElement?
        get() = provider.original

    override fun attach(provider: RealizedExistenceProvider) {
        val thisProvider = this.provider
        if (thisProvider !is VirtualPropertyProvider)
            error("Can't attach a non-virtual provider")

        this.provider = RealizedPropertyProvider(provider).also { it.copyFrom(thisProvider) }
    }

    override fun detach() {
        if (provider !is VirtualPropertyProvider)
            provider = VirtualPropertyProvider(original)
        else
            provider = VirtualPropertyProvider(null).also { it.copyFrom(provider as VirtualPropertyProvider) }
    }
}