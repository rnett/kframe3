package com.rnett.kframe.dom.core

import com.rnett.kframe.dom.core.providers.ExistenceProvider

abstract class MetaElement<S : MetaElement<S>>(parent: MetaElementHost?, existenceProvider: ExistenceProvider) :
    Element<S>(parent, existenceProvider),
    MetaElementHost {
    constructor(parent: MetaElementHost, tag: String) : this(parent, parent.existenceProvider(tag))
}

class BaseMetaElement(parent: MetaElementHost?, existenceProvider: ExistenceProvider) :
    MetaElement<BaseMetaElement>(parent, existenceProvider) {
    constructor(parent: MetaElementHost, tag: String) : this(parent, parent.existenceProvider(tag))
}

abstract class DisplayElement<S : DisplayElement<S>>(
    parent: DisplayElementHost?,
    existenceProvider: ExistenceProvider
) : Element<S>(parent, existenceProvider),
    DisplayElementHost {
    constructor(parent: DisplayElementHost, tag: String) : this(parent, parent.existenceProvider(tag))

    override fun elementAncestor(): AnyElement = this
}

class BaseDisplayElement(parent: DisplayElementHost?, existenceProvider: ExistenceProvider) :
    DisplayElement<BaseDisplayElement>(parent, existenceProvider) {
    constructor(parent: DisplayElementHost, tag: String) : this(parent, parent.existenceProvider(tag))
}

@KFrameDSL
fun MetaElementHost.element(tag: String) =
    BaseMetaElement(this, tag)


@KFrameDSL
fun DisplayElementHost.element(tag: String) =
    BaseDisplayElement(this, tag)
