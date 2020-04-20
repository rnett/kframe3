package com.rnett.kframe.dom.core.style

import com.rnett.kframe.dom.core.JSValue
import com.rnett.kframe.utils.DelegateInterface
import com.rnett.kframe.utils.by
import kotlin.properties.ReadWriteProperty

sealed class BoxShadows : JSValue {
    abstract operator fun plus(other: BoxShadows): MultipleBoxShadows

    infix fun eq(other: BoxShadows) =
        if (this is BoxShadow && other is BoxShadow)
            this == other
        else if (this is BoxShadow && other is MultipleBoxShadows)
            other.shadows.size == 1 && this == other.shadows.first()
        else if (this is MultipleBoxShadows && other is BoxShadow)
            this.shadows.size == 1 && other == this.shadows.first()
        else
            this == other

    companion object {
        fun parse(raw: String): BoxShadows {
            val shadows = MultipleBoxShadows.parse(raw)

            return if (shadows.size == 1)
                shadows.first()
            else
                shadows
        }
    }
}

class BoxShadow(
    val offsetX: Size,
    val offsetY: Size,
    val blurRadius: Size? = null,
    val spreadRadius: Size? = null,
    val inset: Boolean = false,
    val color: Color? = null
) : BoxShadows() {
    override fun toJS() = buildString {
        append(offsetX.toJS() + "  ")
        append(offsetY.toJS() + "  ")

        if (blurRadius != null) {
            append(blurRadius.toJS() + "  ")
            if (spreadRadius != null)
                append(spreadRadius.toJS() + " ")
        } else
            if (spreadRadius != null)
                error("Can't have a set spread-radius without a set blur-radius")

        if (inset)
            append("inset ")

        if (color != null)
            append(color.toJS() + " ")
    }.trim()

    override operator fun plus(other: BoxShadows) =
        when (other) {
            is BoxShadow -> MultipleBoxShadows(
                this,
                other
            )
            is MultipleBoxShadows -> MultipleBoxShadows(
                listOf(this) + other.shadows
            )
        }

    companion object {
        fun parse(raw: String): BoxShadow {
            val parts = raw.split(" ").filter { it.isNotBlank() }

            val offX = Size(parts[0])
            val offY = Size(parts[1])

            val numSizes: Int
            val spread: Size?
            val blur = Size.tryParse(parts.getOrNull(2))

            if (blur != null) {
                spread = Size.tryParse(parts.getOrNull(3))
                if (spread != null)
                    numSizes = 4
                else
                    numSizes = 3
            } else {
                spread = null
                numSizes = 2
            }

            val inset: Boolean
            val color: Color?
            if (parts.getOrNull(numSizes) == "inset") {
                inset = true
                color = parts.getOrNull(numSizes + 1)?.let { Color(it) }
            } else {
                color = parts.getOrNull(numSizes)?.let { Color(it) }
                inset = parts.getOrNull(numSizes + 1) == "inset"
            }


            return BoxShadow(offX, offY, blur, spread, inset, color)
        }
    }
}

class MultipleBoxShadows(val shadows: List<BoxShadow>) : BoxShadows(), List<BoxShadow> by shadows {
    constructor(vararg shadows: BoxShadow) : this(shadows.toList())

    override fun toJS() = shadows.joinToString { it.toJS() }

    override operator fun plus(other: BoxShadows) =
        when (other) {
            is BoxShadow -> MultipleBoxShadows(
                shadows + other
            )
            is MultipleBoxShadows -> MultipleBoxShadows(
                shadows + other.shadows
            )
        }

    companion object {
        fun parse(raw: String): MultipleBoxShadows =
            MultipleBoxShadows(
                raw.split(",").filter { it.isNotBlank() }
                    .map { BoxShadow.parse(it.trim()) })
    }
}

inline fun DelegateInterface.byBoxShadow(key: String): ReadWriteProperty<Any?, BoxShadows?> = by(key) {
    it?.let {
        BoxShadows.parse(it)
    }
}