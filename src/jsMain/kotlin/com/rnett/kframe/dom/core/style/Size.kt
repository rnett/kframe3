package com.rnett.kframe.dom.core.style

import com.rnett.kframe.utils.DelegateInterface
import com.rnett.kframe.utils.by

data class Size(val value: Double, val units: String) : MarginValue,
    PaddingValue,
    LineWidth,
    SingleBackgroundPosition,
    SingleBackgroundSize {
    override fun toJS() = if (isIntValue) "${value.toInt()}$units" else "$value$units"

    val isIntValue = value.toInt().toDouble() == value

    companion object {

        fun tryParse(raw: String?) = if (raw == null) null else Regex("([0-9.]+)([a-zA-Z%]+)").matchEntire(raw)?.let {
            Size(it.groupValues[1].toDouble(), it.groupValues[2])
        }

        operator fun invoke(raw: String) = tryParse(
            raw
        ) ?: Size(raw.toDoubleOrNull() ?: 0.0, "")
    }
}

val Number.px inline get() = Size(this.toDouble(), "px")
val Number.rem inline get() = Size(this.toDouble(), "rem")
val Number.percent inline get() = Size(this.toDouble(), "%")
val Number.cm inline get() = Size(this.toDouble(), "cm")
val Number.em inline get() = Size(this.toDouble(), "em")
val Number.ex inline get() = Size(this.toDouble(), "ex")
val Number.inch inline get() = Size(this.toDouble(), "inch")
val Number.mm inline get() = Size(this.toDouble(), "mm")
val Number.pc inline get() = Size(this.toDouble(), "pc")
val Number.pt inline get() = Size(this.toDouble(), "pt")
val Number.vh inline get() = Size(this.toDouble(), "vh")
val Number.vw inline get() = Size(this.toDouble(), "vw")
val Number.vmin inline get() = Size(this.toDouble(), "vmin")

inline fun DelegateInterface.bySize(key: String) = by(key) { it?.let {
    Size(
        it
    )
} }
inline val DelegateInterface.bySize inline get() = by { it?.let {
    Size(
        it
    )
} }


object Auto : MarginValue,
    SingleBackgroundSize {
    override fun toJS() = "auto"
}

object Inherit : MarginValue,
    PaddingValue {
    override fun toJS() = "inherit"
}