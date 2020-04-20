package com.rnett.kframe.dom.core

import com.rnett.kframe.dom.providers.*

abstract class MetaElement<S: MetaElement<S>>(parent: MetaElementHost?, existenceProvider: ExistenceProvider): Element<S>(parent, existenceProvider),
    MetaElementHost
class BaseMetaElement(parent: MetaElementHost?, existenceProvider: ExistenceProvider): MetaElement<BaseMetaElement>(parent, existenceProvider){
    constructor(parent: MetaElementHost, tag: String): this(parent, parent.existenceProvider(tag))
}

abstract class DisplayElement<S: DisplayElement<S>>(parent: DisplayElementHost?, existenceProvider: ExistenceProvider): Element<S>(parent, existenceProvider),
    DisplayElementHost{
    override fun elementAncestor(): AnyElement = this
}
class BaseDisplayElement(parent: DisplayElementHost?, existenceProvider: ExistenceProvider): DisplayElement<BaseDisplayElement>(parent, existenceProvider) {
    constructor(parent: DisplayElementHost, tag: String): this(parent, parent.existenceProvider(tag))
}

@KFrameDSL
fun MetaElementHost.element(tag: String) =
    BaseMetaElement(this, tag)


@KFrameDSL
fun DisplayElementHost.element(tag: String) =
    BaseDisplayElement(this, tag)

class TextElement internal constructor(val parent: ElementHost, text: String) :
    ElementHost, Attachable<RealizedTextProvider> {
    private val provider = TextProviderWrapper(parent.textProvider(text))
    init {
        parent.addElement(this)
    }

    var text: String
        get() = provider.text
        set(value) {
            provider.text = value
        }

    override fun addElement(element: ElementHost) {
        error("Can't add children to a text element")
    }

    override fun removeChild(element: ElementHost) {
        error("Text elements have no children")
    }

    override fun remove() {
        provider.remove()
        parent.removeChild(this)
    }

    override fun existenceProvider(tag: String): ExistenceProvider {
        error("Can't create an existence provider from a text element")
    }

    override fun textProvider(text: String): TextProvider {
        error("Can't create a text provider from a text element")
    }

    val isRealized: Boolean = provider.isRealized
    internal val realizedProviderOrNull  = provider.realizedProviderOrNull

    override fun attach(provider: RealizedTextProvider){
        this.provider.attach(provider)
    }

    override fun detach(){
        provider.detach()
    }
}