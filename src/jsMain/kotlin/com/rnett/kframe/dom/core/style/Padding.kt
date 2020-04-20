package com.rnett.kframe.dom.core.style

import com.rnett.kframe.dom.core.JSValue
import com.rnett.kframe.utils.DelegateInterface
import com.rnett.kframe.utils.Helper
import com.rnett.kframe.utils.by
import kotlin.properties.ReadWriteProperty

interface PaddingValue : JSValue

inline val DelegateInterface.byPaddingValue: Helper<PaddingValue?>
    inline get() = by {
        it?.let {
            @Suppress("USELESS_CAST")
            when (it) {
                "inherit" -> Inherit as PaddingValue
                else -> Size(it)
            }
        }
    }

inline fun DelegateInterface.byPaddingValue(key: String): ReadWriteProperty<Any?, PaddingValue?> = by(key) {
    it?.let {
        @Suppress("USELESS_CAST")
        when (it) {
            "inherit" -> Inherit as PaddingValue
            else -> Size(it)
        }
    }
}

class Padding internal constructor(_delegate: StyleDelegate = MapDelegate()) : HasStyleDelegate(_delegate, "padding") {
    constructor(
        top: PaddingValue? = null,
        right: PaddingValue? = null,
        bottom: PaddingValue? = null,
        left: PaddingValue? = null
    ) : this() {
        this.top = top
        this.right = right
        this.bottom = bottom
        this.left = left
    }

    constructor(top: PaddingValue? = null, leftRight: PaddingValue? = null, bottom: PaddingValue? = null) : this(
        top,
        leftRight,
        bottom,
        leftRight
    )

    constructor(topBottom: PaddingValue? = null, leftRight: PaddingValue? = null) : this(
        topBottom,
        leftRight,
        topBottom,
        leftRight
    )

    constructor(all: PaddingValue?) : this(all, all, all, all)
    constructor(block: Padding.() -> Unit) : this() {
        block()
    }

    var top by delegate.byPaddingValue("top")
    var right by delegate.byPaddingValue("right")
    var bottom by delegate.byPaddingValue("bottom")
    var left by delegate.byPaddingValue("left")

    inline operator fun invoke(block: Padding.() -> Unit) {
        block()
    }
}