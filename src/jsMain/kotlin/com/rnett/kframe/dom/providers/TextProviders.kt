package com.rnett.kframe.dom.providers

import org.w3c.dom.Text
import kotlin.browser.document

interface TextProvider {
    var text: String
    fun remove()
}

class VirtualTextProvider(override var text: String): TextProvider, VirtualProvider, SingleProvider<TextProvider> {
    override fun remove() {
    }

    override fun copyFrom(other: TextProvider) {
        text = other.text
    }
}

class RealizedTextProvider(internal val underlying: Text): TextProvider, RealizedProvider, SingleProvider<TextProvider> {
    constructor(text: String): this(document.createTextNode(text))

    override var text: String
        get() = underlying.wholeText
        set(value) {
            underlying.textContent = value
        }

    override fun remove() {
        underlying.remove()
    }

    init {
        this.text = text
    }

    override fun copyFrom(other: TextProvider) {
        text = other.text
    }
}

//TODO probably needs fixes.  Especially creation / appending to parent
internal class TextProviderWrapper(initial: TextProvider): TextProvider, Attachable<RealizedTextProvider>{
    private var provider: TextProvider = initial

    override fun attach(provider: RealizedTextProvider) {
        provider.text = text
        this.provider = provider
    }

    override fun detach() {
        provider = VirtualTextProvider(text)
    }

    override var text: String
        get() = provider.text
        set(value) {
            provider.text = value
        }

    override fun remove() {
        provider.remove()
    }

    val isRealized: Boolean get() = provider is RealizedTextProvider
    internal val realizedProviderOrNull get()  = if (isRealized) provider as RealizedTextProvider else null
}