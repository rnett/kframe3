package com.rnett.kframe.dom.providers

import org.w3c.dom.HTMLElement
import org.w3c.dom.Node
import kotlin.browser.document

interface ExistenceProvider {
    val tag: String
    fun addChild(child: Node)
    fun removeChild(child: Node)
    fun remove()
    fun insertChild(before: Node?, child: Node)

    fun existenceProvider(tag: String): ExistenceProvider
    fun attributeProvider(): AttributeProvider
    fun classProvider(): ClassProvider
    fun eventProvider(): EventProvider
    fun styleProvider(): StyleProvider
    fun textProvider(text: String): TextProvider
}

internal class VirtualExistenceProvider(override val tag: String) : ExistenceProvider, VirtualProvider{
    override fun addChild(child: Node) {
    }

    override fun removeChild(child: Node) {
    }

    override fun remove() {
    }

    override fun insertChild(before: Node?, child: Node) {
    }

    override fun existenceProvider(tag: String): ExistenceProvider = VirtualExistenceProvider(tag)
    override fun attributeProvider(): AttributeProvider = VirtualAttributeProvider()
    override fun classProvider(): ClassProvider = VirtualClassProvider()
    override fun eventProvider(): EventProvider = VirtualEventProvider()
    override fun styleProvider(): StyleProvider = VirtualStyleProvider(false)
    override fun textProvider(text: String): TextProvider = VirtualTextProvider(text)
}

class RealizedExistenceProvider(override val tag: String, underlying: HTMLElement = document.createElement(tag) as HTMLElement): ExistenceProvider, RealizedProvider {
    internal var _underlying: HTMLElement? = underlying
    internal inline val underlying get() = _underlying ?: error("Element has been removed")

    override fun addChild(child: Node) {
         underlying.appendChild(child)
    }

    override fun removeChild(child: Node) {
        underlying.removeChild(child)
    }

    override fun remove() {
        _underlying?.remove()
        _underlying = null
    }

    override fun insertChild(before: Node?, child: Node) {
        underlying.insertBefore(child, before)
    }

    override fun existenceProvider(tag: String): ExistenceProvider = RealizedExistenceProvider(tag)
    override fun attributeProvider(): AttributeProvider = RealizedAttributeProvider(this)
    override fun classProvider(): ClassProvider = RealizedClassProvider(this)
    override fun eventProvider(): EventProvider = RealizedEventProvider(this)
    override fun styleProvider(): StyleProvider = RealizedStyleProvider(this)
    override fun textProvider(text: String): TextProvider = RealizedTextProvider(text)
}

internal class ExistenceProviderWrapper(initial: ExistenceProvider): ExistenceProvider, ExistenceAttachable{
    private var provider: ExistenceProvider = initial

    override val tag: String
        get() = provider.tag

    override fun addChild(child: Node) {
        provider.addChild(child)
    }

    override fun removeChild(child: Node) {
        provider.removeChild(child)
    }

    override fun remove() {
        provider.remove()
    }

    override fun insertChild(before: Node?, child: Node) {
        provider.insertChild(before, child)
    }

    override fun attach(provider: RealizedExistenceProvider) {
        this.provider = provider
    }

    override fun detach() {
        provider = VirtualExistenceProvider(tag)
    }

    override fun existenceProvider(tag: String): ExistenceProvider = provider.existenceProvider(tag)
    override fun attributeProvider(): AttributeProvider = provider.attributeProvider()
    override fun classProvider(): ClassProvider = provider.classProvider()
    override fun eventProvider(): EventProvider = provider.eventProvider()
    override fun styleProvider(): StyleProvider = provider.styleProvider()
    override fun textProvider(text: String): TextProvider = provider.textProvider(text)

    val isRealized: Boolean get() = provider is RealizedExistenceProvider
    internal val realizedProviderOrNull get()  = if (isRealized) provider as RealizedExistenceProvider else null
}