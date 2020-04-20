package com.rnett.kframe.dom.core.style

class Outline internal constructor(_delegate: StyleDelegate = MapDelegate()) : HasStyleDelegate(_delegate, "outline") {
    var style by delegate.byEnum<LineStyle>("style")
    var width by delegate.byLineWidth("width")
    var color by delegate.byColor("color")
    var offset by delegate.bySize("offset")

    constructor(
        style: LineStyle? = null,
        width: LineWidth? = null,
        color: Color? = null,
        offset: Size? = null
    ) : this() {
        this.style = style
        this.width = width
        this.color = color
        this.offset = offset
    }

    constructor(block: Outline.() -> Unit) : this() {
        block()
    }

    inline operator fun invoke(block: Outline.() -> Unit) {
        block()
    }
}