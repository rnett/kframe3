package com.rnett.kframe.dom.core

import com.rnett.kframe.dom.core.providers.AttributeProviderWrapper
import com.rnett.kframe.dom.core.providers.ClassProviderWrapper
import com.rnett.kframe.dom.core.providers.ExistenceAttachable
import com.rnett.kframe.dom.core.providers.RealizedExistenceProvider
import com.rnett.kframe.utils.StringDelegatable
import com.rnett.kframe.utils.StringDelegatePassthrough

interface JSValue {
    /**
     * Returns the value in JS, or null to remove the attribute/style
     */
    fun toJS(): String?
}

//TODO aria attributes
class Attributes internal constructor(element: Element<*>) : StringDelegatable(), ExistenceAttachable {

    internal val provider = AttributeProviderWrapper(element.provider.attributeProvider())

    override fun attach(provider: RealizedExistenceProvider) {
        this.provider.attach(provider)
    }

    override fun detach() {
        provider.detach()
    }

    private val blacklist = setOf("class", "style")

    override fun getValue(key: String) =
        if (key in blacklist) error("Can't get $key from attributes, use accessor") else provider.getValue(key)

    override fun setValue(key: String, value: String?) {
        if (key in blacklist)
            error("Can't set $key from attributes, use accessor")

        provider.setValue(key, value)
    }

    inner class Data internal constructor() : StringDelegatePassthrough(this, "data-") {
        var target by this
        var dismiss by this
        var toggle by this
        var content by this
        var placement by this
        var container by this
        var trigger by this
    }

    val data = Data()
}

class Classes internal constructor(element: Element<*>) : ExistenceAttachable {

    internal val provider = ClassProviderWrapper(element.provider.classProvider())

    override fun attach(provider: RealizedExistenceProvider) {
        this.provider.attach(provider)
    }

    override fun detach() {
        provider.detach()
    }

    val list get() = provider.list()

    operator fun contains(klass: String) = provider.contains(klass)

    fun add(vararg klasses: String) {
        provider.addClasses(*klasses)
    }

    fun remove(vararg klasses: String) {
        provider.removeClasses(*klasses)
    }

    operator fun plusAssign(klass: String) = add(klass)
    operator fun plusAssign(klasses: Iterable<String>) = add(*klasses.toList().toTypedArray())

    operator fun minusAssign(klass: String) = remove(klass)
    operator fun minusAssign(klasses: Iterable<String>) = remove(*klasses.toList().toTypedArray())

    fun set(klasses: List<String>?) = if (klasses == null) clear() else provider.set(klasses)

    fun clear() = provider.clear()
}