package com.rnett.kframe.dom.basics

import com.rnett.kframe.dom.core.KFrameDSL
import com.rnett.kframe.dom.core.MetaElement
import com.rnett.kframe.dom.core.MetaElementHost
import com.rnett.kframe.dom.providers.DomProvider

@KFrameDSL
inline val MetaElementHost.script
    inline get() = ScriptElement(this, domProvider)

class ScriptElement(parent: MetaElementHost?, domProvider: DomProvider) : MetaElement<ScriptElement>(parent, domProvider, "script") {
    var src by attributes
    var type by attributes
    var integrity by attributes
    var crossorigin by attributes

    override fun makeNew(parent: MetaElementHost?, domProvider: DomProvider): ScriptElement {
        return ScriptElement(parent, domProvider)
    }
}

@KFrameDSL
inline fun MetaElementHost.script(
    src: String,
    type: String? = "text/javascript",
    builder: ScriptElement.() -> Unit = {}
) = ScriptElement(this, domProvider)() {
    this.src = src
    this.type = type
    builder()
}

class TitleElement(parent: MetaElementHost?, domProvider: DomProvider) : MetaElement<TitleElement>(parent, domProvider, "title") {

    private val titleElement = +""

    var titleText: String
        get() = titleElement.text
        set(v) {
            titleElement.text = v
        }

    constructor(parent: MetaElementHost, title: String) : this(parent, parent.domProvider) {
        this.titleText = title
    }

    override fun makeNew(parent: MetaElementHost?, domProvider: DomProvider): TitleElement {
        return TitleElement(parent, domProvider)
    }
}

class LinkElement(parent: MetaElementHost?, domProvider: DomProvider) : MetaElement<LinkElement>(parent, domProvider, "link") {
    var rel by attributes
    var href by attributes
    var integrity by attributes
    var crossorigin by attributes

    override fun makeNew(parent: MetaElementHost?, domProvider: DomProvider): LinkElement {
        return LinkElement(parent, domProvider)
    }
}

@KFrameDSL
inline val MetaElementHost.link
    inline get() = LinkElement(this, domProvider)

@KFrameDSL
inline fun MetaElementHost.link(href: String, rel: String, builder: LinkElement.() -> Unit = {}) = LinkElement(this, domProvider)() {
    this.href = href
    this.rel = rel
    builder()
}

@KFrameDSL
inline fun MetaElementHost.remoteStylesheet(
    href: String,
    rel: String = "text/css",
    builder: LinkElement.() -> Unit = {}
) = link(href, rel, builder)