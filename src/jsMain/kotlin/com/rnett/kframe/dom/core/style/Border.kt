package com.rnett.kframe.dom.core.style

import com.rnett.kframe.dom.core.JSValue
import com.rnett.kframe.utils.DelegateInterface
import com.rnett.kframe.utils.Helper
import com.rnett.kframe.utils.by
import kotlin.properties.ReadWriteProperty

enum class LineStyle : JSValue {
    Dotted, Dashed, Solid, Double, Groove, Ridge, Inset, Outset, None, Hidden;

    override fun toJS() = name.toLowerCase()
}

interface LineWidth : JSValue
enum class PresetLineWidth : LineWidth {
    Thin, Medium, Thick;

    override fun toJS() = name.toLowerCase()
}

inline val DelegateInterface.byLineWidth: Helper<LineWidth?>
    inline get() = by {
        it?.let {
            try {
                enumValueOf<PresetLineWidth>(it.capitalize())
            } catch (e: Exception) {
                Size(it)
            }
        }
    }

inline fun DelegateInterface.byLineWidth(key: String): ReadWriteProperty<Any?, LineWidth?> = by(key) {
    it?.let {
        try {
            enumValueOf<PresetLineWidth>(it.capitalize())
        } catch (e: Exception) {
            Size(it)
        }
    }
}

class BorderSide internal constructor(val side: String? = null, _delegate: StyleDelegate = MapDelegate()) :
    HasStyleDelegate(_delegate, side) {
    //TODO change order to match composite CSS  (for all things like this)
    constructor(color: Color? = null, style: LineStyle? = null, width: LineWidth? = null) : this() {
        this.color = color
        this.style = style
        this.width = width
    }

    @StylesheetDSL
    var color by delegate.byColor("color")

    @StylesheetDSL
    var style by delegate.byEnum<LineStyle>("style", true)

    @StylesheetDSL
    var width by delegate.byLineWidth("width")

    constructor(block: BorderSide.() -> Unit) : this() {
        block()
    }

    inline operator fun invoke(block: BorderSide.() -> Unit) {
        block()
    }
}

//TODO elliptical corners w/ double radii https://developer.mozilla.org/en-US/docs/Web/CSS/border-radius
class Radius internal constructor(_delegate: StyleDelegate = MapDelegate()) : HasStyleDelegate(_delegate, null) {

    constructor(
        topLeft: Size? = null,
        topRight: Size? = null,
        bottomLeft: Size? = null,
        bottomRight: Size? = null
    ) : this() {
        this.topLeft = topLeft
        this.topRight = topRight
        this.bottomLeft = bottomLeft
        this.bottomRight = bottomRight
    }

    constructor(topLeftBottomRight: Size? = null, topRightBottomLeft: Size? = null) : this(
        topLeftBottomRight,
        topRightBottomLeft,
        topLeftBottomRight,
        topRightBottomLeft
    )

    constructor(topLeft: Size? = null, topRightBottomLeft: Size? = null, bottomRight: Size? = null) : this(
        topLeft,
        topRightBottomLeft,
        topRightBottomLeft,
        bottomRight
    )

    constructor(all: Size) : this(all, all, all, all)

    var topLeft by delegate.bySize("top-left-radius")
    var topRight by delegate.bySize("top-right-radius")
    var bottomLeft by delegate.bySize("bottom-left-radius")
    var bottomRight by delegate.bySize("bottom-right-radius")
}

class Border internal constructor(_delegate: StyleDelegate = MapDelegate()) : HasStyleDelegate(_delegate, "border") {
    constructor(
        top: BorderSide? = null,
        right: BorderSide? = null,
        bottom: BorderSide? = null,
        left: BorderSide? = null
    ) : this() {
        if (top != null)
            this.top = top

        if (right != null)
            this.right = right

        if (bottom != null)
            this.bottom = bottom

        if (left != null)
            this.left = left
    }

    constructor(top: BorderSide? = null, leftRight: BorderSide? = null, bottom: BorderSide? = null) : this(
        top,
        leftRight,
        bottom,
        leftRight
    )

    constructor(topBottom: BorderSide? = null, leftRight: BorderSide? = null) : this(
        topBottom,
        leftRight,
        topBottom,
        leftRight
    )

    constructor(all: BorderSide?) : this(all, all, all, all)
    constructor(color: Color? = null, style: LineStyle? = null, width: LineWidth? = null) : this(
        BorderSide(
            color,
            style,
            width
        )
    )

    constructor(block: Border.() -> Unit) : this() {
        block()
    }

    inline operator fun invoke(block: Border.() -> Unit) {
        block()
    }

    private val _top by lazy { BorderSide("top", delegate) }

    @StylesheetDSL
    var top
        get() = _top
        set(v) {
            loadFrom(v, "top")
        }

    private val _right by lazy { BorderSide("right", delegate) }

    @StylesheetDSL
    var right
        get() = _right
        set(v) {
            loadFrom(v, "right")
        }

    private val _bottom by lazy { BorderSide("bottom", delegate) }

    @StylesheetDSL
    var bottom
        get() = _bottom
        set(v) {
            loadFrom(v, "bottom")
        }

    private val _left by lazy { BorderSide("left", delegate) }

    @StylesheetDSL
    var left
        get() = _left
        set(v) {
            loadFrom(v, "left")
        }

    private val _radius by lazy { Radius(delegate) }

    @StylesheetDSL
    var radius
        get() = _radius
        set(v) {
            loadFrom(v)
        }

    @StylesheetDSL
    fun style(
        top: LineStyle? = null,
        right: LineStyle? = null,
        bottom: LineStyle? = null,
        left: LineStyle? = null
    ) {
        this.top.style = top
        this.right.style = right
        this.bottom.style = bottom
        this.left.style = left
    }

    @StylesheetDSL
    fun style(top: LineStyle? = null, leftRight: LineStyle? = null, bottom: LineStyle? = null) {
        this.top.style = top
        this.right.style = leftRight
        this.bottom.style = bottom
        this.left.style = leftRight
    }

    @StylesheetDSL
    fun style(topBottom: LineStyle? = null, leftRight: LineStyle? = null) {
        this.top.style = topBottom
        this.right.style = leftRight
        this.bottom.style = topBottom
        this.left.style = leftRight
    }

    @StylesheetDSL
    fun style(all: LineStyle?) {
        this.top.style = all
        this.right.style = all
        this.bottom.style = all
        this.left.style = all
    }

    @StylesheetDSL
    fun width(
        top: LineWidth? = null,
        right: LineWidth? = null,
        bottom: LineWidth? = null,
        left: LineWidth? = null
    ) {
        this.top.width = top
        this.right.width = right
        this.bottom.width = bottom
        this.left.width = left
    }

    @StylesheetDSL
    fun width(top: LineWidth? = null, leftRight: LineWidth? = null, bottom: LineWidth? = null) {
        this.top.width = top
        this.right.width = leftRight
        this.bottom.width = bottom
        this.left.width = leftRight
    }

    @StylesheetDSL
    fun width(topBottom: LineWidth? = null, leftRight: LineWidth? = null) {
        this.top.width = topBottom
        this.right.width = leftRight
        this.bottom.width = topBottom
        this.left.width = leftRight
    }

    @StylesheetDSL
    fun width(all: LineWidth?) {
        this.top.width = all
        this.right.width = all
        this.bottom.width = all
        this.left.width = all
    }

    @StylesheetDSL
    fun color(top: Color? = null, right: Color? = null, bottom: Color? = null, left: Color? = null) {
        this.top.color = top
        this.right.color = right
        this.bottom.color = bottom
        this.left.color = left
    }

    @StylesheetDSL
    fun color(top: Color? = null, leftRight: Color? = null, bottom: Color? = null) {
        this.top.color = top
        this.right.color = leftRight
        this.bottom.color = bottom
        this.left.color = leftRight
    }

    @StylesheetDSL
    fun color(topBottom: Color? = null, leftRight: Color? = null) {
        this.top.color = topBottom
        this.right.color = leftRight
        this.bottom.color = topBottom
        this.left.color = leftRight
    }

    @StylesheetDSL
    fun color(all: Color?) {
        this.top.color = all
        this.right.color = all
        this.bottom.color = all
        this.left.color = all
    }

    @StylesheetDSL
    fun allSides(block: BorderSide.() -> Unit) {
        top.block()
        right.block()
        bottom.block()
        left.block()
    }

    @StylesheetDSL
    var collapse
        get() = delegate["collapse"]?.let { it == "collapse" }
        set(v) {
            when (v) {
                true -> delegate["collapse"] = "collapse"
                false -> delegate["collapse"] = "seperate"
                null -> delegate.remove("collapse")
            }
        }

    @StylesheetDSL
    fun spacing(all: Size?) {
        delegate["border-spacing"] = all?.toJS()
    }

    @StylesheetDSL
    fun spacing(horizontal: Size, vertical: Size) {
        delegate["border-spacing"] = "${horizontal.toJS()} ${vertical.toJS()}"
    }
}