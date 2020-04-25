package com.rnett.kframe.dom.core.providers

import org.w3c.dom.get

interface AttributeProvider {
    fun getValue(key: String): String?
    fun setValue(key: String, value: String?)
    fun map(): Map<String, String>
}

internal class VirtualAttributeProvider : AttributeProvider, SingleProvider<AttributeProvider>, VirtualProvider {
    private val attributes = mutableMapOf<String, String>()

    override fun getValue(key: String): String? = attributes[key]

    override fun setValue(key: String, value: String?) {
        if (value == null)
            attributes.remove(key)
        else
            attributes[key] = value
    }

    override fun map(): Map<String, String> = attributes

    override fun copyFrom(other: AttributeProvider) {
        attributes.clear()
        attributes.putAll(other.map())
    }
}

internal class RealizedAttributeProvider(private val existenceProvider: RealizedExistenceProvider) : AttributeProvider,
    RealizedProvider, SingleProvider<AttributeProvider> {
    private inline val underlying get() = existenceProvider.underlying
    override fun getValue(key: String) = underlying.getAttribute(key)

    override fun setValue(key: String, value: String?) {
        if (value == null)
            underlying.removeAttribute(key)
        else
            underlying.setAttribute(key, value)
    }

    override fun map(): Map<String, String> {
        val map = mutableMapOf<String, String>()
        for (i in 0 until underlying.attributes.length) {
            val attr = underlying.attributes[i]!!
            map[attr.name] = attr.value
        }

        return map
    }

    override fun copyFrom(other: AttributeProvider) {
        val otherMap = other.map()
        otherMap.forEach { (key, value) ->
            if(getValue(key) != value)
                setValue(key, value)
        }

        map().keys.forEach {
            if(it !in otherMap)
                setValue(it, null)
        }
    }
}

internal class AttributeProviderWrapper(initial: AttributeProvider): AttributeProvider, ExistenceAttachable {
    private var provider: AttributeProvider = initial
    override fun getValue(key: String): String? = provider.getValue(key)

    override fun setValue(key: String, value: String?) = provider.setValue(key, value)

    override fun map(): Map<String, String> = provider.map()

    override fun attach(provider: RealizedExistenceProvider) {
        this.provider = RealizedAttributeProvider(provider).also { it.copyFrom(this.provider) }
    }

    override fun detach() {
        provider = VirtualAttributeProvider().also { it.copyFrom(provider) }
    }
}