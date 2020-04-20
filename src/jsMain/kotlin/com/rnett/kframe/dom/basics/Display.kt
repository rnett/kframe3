package com.rnett.kframe.dom.basics

import com.rnett.kframe.dom.core.DisplayElement
import com.rnett.kframe.dom.core.DisplayElementHost
import com.rnett.kframe.dom.core.KFrameDSL
import com.rnett.kframe.dom.core.element
import com.rnett.kframe.dom.providers.DomProvider
import com.rnett.kframe.utils.byInt

@KFrameDSL
inline val DisplayElementHost.div inline get() = element("div")

@KFrameDSL
inline val DisplayElementHost.span inline get() = element("span")

@KFrameDSL
inline val DisplayElementHost.p inline get() = element("p")

class AElement(parent: DisplayElementHost?, domProvider: DomProvider) : DisplayElement<AElement>(parent, domProvider, "a") {
    var href by attributes
    var download by attributes
    var type by attributes

    override fun makeNew(parent: DisplayElementHost?, domProvider: DomProvider): AElement {
        return AElement(parent, domProvider)
    }
}

@KFrameDSL
inline val DisplayElementHost.a inline get() = AElement(this, domProvider)

class ImageElement(parent: DisplayElementHost?, domProvider: DomProvider) : DisplayElement<ImageElement>(parent, domProvider, "img") {
    var src by attributes

    var height by attributes.byInt
    var width by attributes.byInt

    override fun makeNew(parent: DisplayElementHost?, domProvider: DomProvider): ImageElement {
        return ImageElement(parent, domProvider)
    }
}

@KFrameDSL
inline val DisplayElementHost.img inline get() = ImageElement(this, domProvider)

@KFrameDSL
inline fun DisplayElementHost.img(src: String, klass: String? = null, id: String? = null, builder: ImageElement.() -> Unit = {}) =
    img(klass, id) {
        this.src = src
        builder()
    }

@KFrameDSL
inline fun DisplayElementHost.img(
    src: String,
    height: Int,
    width: Int,
    klass: String? = null,
    id: String? = null,
    builder: ImageElement.() -> Unit = {}
) =
    img(klass, id) {
        this.src = src
        this.height = height
        this.width = width
        builder()
    }

@KFrameDSL
inline val DisplayElementHost.br
    inline get() = element("br")
