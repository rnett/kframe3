package com.rnett.kframe.dom.basics

import com.rnett.kframe.dom.core.DisplayElement
import com.rnett.kframe.dom.core.DisplayElementHost
import com.rnett.kframe.dom.core.KFrameDSL
import com.rnett.kframe.dom.core.element
import com.rnett.kframe.utils.byInt

@KFrameDSL
inline val DisplayElementHost.div inline get() = element("div")

@KFrameDSL
inline val DisplayElementHost.span inline get() = element("span")

@KFrameDSL
inline val DisplayElementHost.p inline get() = element("p")

class AElement(parent: DisplayElementHost) : DisplayElement<AElement>(parent, "a") {
    var href by properties
    var download by properties
    var type by properties
}

@KFrameDSL
inline val DisplayElementHost.a inline get() = AElement(this)

class ImageElement(parent: DisplayElementHost) : DisplayElement<ImageElement>(parent, "img") {
    var src by properties

    var height by properties.byInt
    var width by properties.byInt
}

@KFrameDSL
inline val DisplayElementHost.img inline get() = ImageElement(this)

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
