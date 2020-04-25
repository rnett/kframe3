package com.rnett.kframe.dom.core.providers

import kotlin.dom.addClass
import kotlin.dom.hasClass
import kotlin.dom.removeClass

interface ClassProvider {
    fun list(): List<String>
    fun contains(klass: String): Boolean
    fun addClasses(vararg klasses: String)
    fun removeClasses(vararg klasses: String)
    fun set(klasses: List<String>)
    fun clear()
}

internal class VirtualClassProvider() : ClassProvider, SingleProvider<ClassProvider>, VirtualProvider {
    private val classes = mutableSetOf<String>()
    override fun list(): List<String> = classes.toList()

    override fun contains(klass: String): Boolean  = klass in classes

    override fun addClasses(vararg klasses: String) {
        classes.addAll(klasses)
    }

    override fun removeClasses(vararg klasses: String) {
        classes.removeAll(klasses)
    }

    override fun set(klasses: List<String>) {
        classes.clear()
        classes.addAll(klasses)
    }

    override fun clear() {
        classes.clear()
    }

    override fun copyFrom(other: ClassProvider) {
        classes.clear()
        classes.addAll(other.list())
    }
}

internal class RealizedClassProvider(private val existenceProvider: RealizedExistenceProvider) : ClassProvider, SingleProvider<ClassProvider>,
    RealizedProvider {
    private inline val underlying get() = existenceProvider.underlying

    override fun list(): List<String> = underlying.classList.value.split(" ").filter { it.isNotBlank() }

    override fun contains(klass: String): Boolean = underlying.hasClass(klass)

    override fun addClasses(vararg klasses: String) {
        underlying.addClass(*klasses)
    }

    override fun removeClasses(vararg klasses: String) {
        underlying.removeClass(*klasses)
    }

    override fun set(klasses: List<String>) {
        underlying.classList.value = klasses.joinToString(" ")
    }

    override fun clear() {
        underlying.removeAttribute("class")
    }

    override fun copyFrom(other: ClassProvider) {
        underlying.classList.value = other.list().joinToString(" ")
    }
}

internal class ClassProviderWrapper(initial: ClassProvider): ClassProvider, ExistenceAttachable {
    private var provider: ClassProvider = initial

    override fun attach(provider: RealizedExistenceProvider) {
        this.provider = RealizedClassProvider(provider).also { it.copyFrom(this.provider) }
    }

    override fun detach() {
        provider = VirtualClassProvider().also { it.copyFrom(provider) }
    }

    override fun list(): List<String> = provider.list()

    override fun contains(klass: String): Boolean = provider.contains(klass)

    override fun addClasses(vararg klasses: String) = provider.addClasses(*klasses)

    override fun removeClasses(vararg klasses: String) = provider.removeClasses(*klasses)

    override fun set(klasses: List<String>) = provider.set(klasses)

    override fun clear() = provider.clear()
}
