package com.rnett.kframe.dom.basics

import com.rnett.kframe.dom.core.KFrameDSL
import com.rnett.kframe.dom.core.MetaElement
import com.rnett.kframe.dom.core.MetaElementHost

@KFrameDSL
inline val MetaElementHost.script
    inline get() = ScriptElement(this)

class ScriptElement(parent: MetaElementHost) : MetaElement<ScriptElement>(parent, "script") {
    var src by attributes
    var type by attributes
    var integrity by attributes
    var crossorigin by attributes
}

@KFrameDSL
inline fun MetaElementHost.script(
    src: String,
    type: String? = "text/javascript",
    builder: ScriptElement.() -> Unit = {}
) = ScriptElement(this)() {
    this.src = src
    this.type = type
    builder()
}

class TitleElement(parent: MetaElementHost) : MetaElement<TitleElement>(parent, "title") {

    private val titleElement = +""

    var titleText: String
        get() = titleElement.text
        set(v) {
            titleElement.text = v
        }

    constructor(parent: MetaElementHost, title: String) : this(parent) {
        this.titleText = title
    }
}

class LinkElement(parent: MetaElementHost) : MetaElement<LinkElement>(parent, "link") {
    var rel by attributes
    var href by attributes
    var integrity by attributes
    var crossorigin by attributes
}

@KFrameDSL
inline val MetaElementHost.link
    inline get() = LinkElement(this)

@KFrameDSL
inline fun MetaElementHost.link(href: String, rel: String, builder: LinkElement.() -> Unit = {}) = LinkElement(this)() {
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