package com.rnett.kframe.dom.core.style


import com.rnett.kframe.dom.core.JSValue
import com.rnett.kframe.utils.DelegateInterface
import com.rnett.kframe.utils.Helper
import com.rnett.kframe.utils.by
import kotlin.properties.ReadWriteProperty

interface MarginValue : JSValue

inline val DelegateInterface.byMarginValue: Helper<MarginValue?>
    inline get() = by {
        it?.let {
            when (it) {
                "auto" -> Auto
                "inherit" -> Inherit
                else -> Size(it)
            }
        }
    }

inline fun DelegateInterface.byMarginValue(key: String): ReadWriteProperty<Any?, MarginValue?> = by(key) {
    it?.let {
        when (it) {
            "auto" -> Auto
            "inherit" -> Inherit
            else -> Size(it)
        }
    }
}

class Margin internal constructor(_delegate: StyleDelegate = MapDelegate()) : HasStyleDelegate(_delegate, "margin") {
    constructor(
        top: MarginValue? = null,
        right: MarginValue? = null,
        bottom: MarginValue? = null,
        left: MarginValue? = null
    ) : this() {
        this.top = top
        this.right = right
        this.bottom = bottom
        this.left = left
    }

    constructor(top: MarginValue? = null, leftRight: MarginValue? = null, bottom: MarginValue? = null) : this(
        top,
        leftRight,
        bottom,
        leftRight
    )

    constructor(topBottom: MarginValue? = null, leftRight: MarginValue? = null) : this(
        topBottom,
        leftRight,
        topBottom,
        leftRight
    )

    constructor(all: MarginValue?) : this(all, all, all, all)
    constructor(block: Margin.() -> Unit) : this() {
        block()
    }

    var top by delegate.byMarginValue("top")
    var right by delegate.byMarginValue("right")
    var bottom by delegate.byMarginValue("bottom")
    var left by delegate.byMarginValue("left")

    inline operator fun invoke(block: Margin.() -> Unit) {
        block()
    }
}