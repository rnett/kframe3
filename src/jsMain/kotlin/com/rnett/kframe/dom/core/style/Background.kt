package com.rnett.kframe.dom.core.style


import com.rnett.kframe.dom.core.JSValue
import com.rnett.kframe.utils.DelegateInterface
import com.rnett.kframe.utils.by
import kotlin.properties.ReadWriteProperty


sealed class BackgroundRepeat(val js: String) : JSValue {
    object RepeatX : BackgroundRepeat("repeat-x")
    object RepeatY : BackgroundRepeat("repeat-y")
    class XY(val horizontal: SingleRepeat, val vertical: SingleRepeat) :
        BackgroundRepeat("${horizontal.js} ${vertical.js}")

    abstract class SingleRepeat(js: String) : BackgroundRepeat(js)

    object DoRepeat : SingleRepeat("repeat")
    object Space : SingleRepeat("space")
    object Round : SingleRepeat("round")
    object NoRepeat : SingleRepeat("no-repeat")

    override fun toJS() = js

    companion object {
        internal fun parseSingle(raw: String): SingleRepeat? =
            when (raw.toLowerCase()) {
                "repeat" -> DoRepeat
                "space" -> Space
                "round" -> Round
                "no-repeat" -> NoRepeat
                else -> null
            }

        fun parse(raw: String): BackgroundRepeat? =
            when (raw.toLowerCase()) {
                "repeat-x" -> RepeatX
                "repeat-y" -> RepeatY
                else -> {
                    raw.toLowerCase().let {
                        if (" " in it) {
                            val parts = it.split(" ")
                            if (parts.size != 2)
                                throw IllegalArgumentException("To many segments in $it, expected 2 (space seperated)")

                            XY(
                                parseSingle(
                                    parts[0]
                                ) ?: error("Could not parse 1st part (${parts[0]}) to a Repeat"),
                                parseSingle(
                                    parts[1]
                                ) ?: error("Could not parse 2nd part (${parts[1]}) to a Repeat")
                            )
                        } else
                            parse(it)
                    }
                }
            }
    }
}

inline fun DelegateInterface.byRepeat(key: String): ReadWriteProperty<Any?, BackgroundRepeat?> = by(key) {
    it?.let {
        BackgroundRepeat.parse(it)
    }
}

enum class BackgroundAttachment : JSValue {
    Scroll, Fixed, Local;

    override fun toJS() = name.toLowerCase()
}

enum class BackgroundBox : JSValue {
    BorderBox, PaddingBox, ContentBox;

    override fun toJS() = name.camelToDash()
}

interface BackgroundPosition : JSValue {
    companion object {
        fun parseSingle(raw: String) = try {
            enumValueOf<BackgroundEdge>(raw.capitalize())
        } catch (e: Exception) {
            Size(raw)
        }
    }
}

interface SingleBackgroundPosition : BackgroundPosition {
    fun isHorizontal(): Boolean? = null
}

enum class BackgroundEdge : SingleBackgroundPosition {
    Top, Right, Bottom, Left;

    override fun toJS() = name.toLowerCase()

    infix fun offsetBy(offset: Size) =
        OffsetBackgroundEdge(this, offset)

    override fun isHorizontal() = this == Right || this == Left
}

class OffsetBackgroundEdge(val edge: BackgroundEdge, val offset: Size) :
    SingleBackgroundPosition {
    override fun toJS() = "${edge.toJS()} ${offset.toJS()}"

    override fun isHorizontal() = edge.isHorizontal()
}

class PairBackgroundPosition(val horizontal: SingleBackgroundPosition, val vertical: SingleBackgroundPosition) :
    BackgroundPosition {
    override fun toJS() = "${horizontal.toJS()} ${vertical.toJS()}"

    companion object {
        fun orderAndMake(part1: SingleBackgroundPosition, part2: SingleBackgroundPosition) =
            if (part1.isHorizontal() == false || part2.isHorizontal() == true)
                PairBackgroundPosition(part2, part1)
            else if (part1.isHorizontal() == true || part2.isHorizontal() == false)
                PairBackgroundPosition(part1, part2)
            else
                PairBackgroundPosition(part1, part2)
    }
}

operator fun SingleBackgroundPosition.plus(other: SingleBackgroundPosition) =
    PairBackgroundPosition(this, other)


inline fun DelegateInterface.byBackgroundPosition(key: String): ReadWriteProperty<Any?, BackgroundPosition?> = by(key) {
    it?.let {
        val parts = it.split(" ").filter { it.isNotBlank() }

        when (parts.size) {
            1 -> {
                BackgroundPosition.parseSingle(parts[0])
            }
            2 -> {
                val part1 =
                    BackgroundPosition.parseSingle(parts[0])
                val part2 =
                    BackgroundPosition.parseSingle(parts[1])
                PairBackgroundPosition.orderAndMake(
                    part1,
                    part2
                )
            }
            3 -> {
                val part1 =
                    BackgroundPosition.parseSingle(parts[0])
                val part2 =
                    BackgroundPosition.parseSingle(parts[1])
                val part3 =
                    BackgroundPosition.parseSingle(parts[2])

                if (part2 is BackgroundEdge)
                    PairBackgroundPosition.orderAndMake(
                        part1,
                        part2 offsetBy (part3 as? Size
                            ?: error("For a 3 value background-position where part 2 is an edge, part 3 must be a size"))
                    )
                else
                    PairBackgroundPosition.orderAndMake(
                        (part1 as? BackgroundEdge
                            ?: error("For 3 value background-position, part 1 must be an edge")) offsetBy (part2 as? Size
                            ?: error("part 2 must be a size")),
                        part3
                    )

            }
            4 -> {
                val part1 = BackgroundPosition.parseSingle(
                    parts[0]
                ) as? BackgroundEdge
                    ?: error("For 4 value background-position, part 1 must be an edge")
                val part2 = BackgroundPosition.parseSingle(
                    parts[1]
                ) as? Size
                    ?: error("For 4 value background-position, part 2 must be an size")
                val part3 = BackgroundPosition.parseSingle(
                    parts[2]
                ) as? BackgroundEdge
                    ?: error("For 4 value background-position, part 3 must be an edge")
                val part4 = BackgroundPosition.parseSingle(
                    parts[3]
                ) as? Size
                    ?: error("For 4 value background-position, part 4 must be an size")

                PairBackgroundPosition.orderAndMake(
                    part1 offsetBy part2,
                    part3 offsetBy part4
                )
            }
            else -> throw IllegalStateException("Must have between 1 and 4 parts in a background-position")
        }

    }
}

interface BackgroundSize : JSValue {
    companion object {
        fun parseSingle(raw: String): SingleBackgroundSize =
            if (raw == "auto")
                Auto
            else
                try {
                    enumValueOf<BackgroundSizePresets>(raw.capitalize())
                } catch (e: Exception) {
                    Size(raw)
                }
    }
}

interface SingleBackgroundSize : BackgroundSize

enum class BackgroundSizePresets : SingleBackgroundSize {
    Cover, Contain;

    override fun toJS() = name.toLowerCase()
}

class PairBackgroundSize(val width: SingleBackgroundSize, val height: SingleBackgroundSize) :
    BackgroundSize {
    override fun toJS() = "${width.toJS()} ${height.toJS()}"
}

operator fun SingleBackgroundSize.plus(other: SingleBackgroundSize) =
    PairBackgroundSize(this, other)

inline fun DelegateInterface.byBackgroundSize(key: String): ReadWriteProperty<Any?, BackgroundSize?> = by(key) {
    it?.let {
        val parts = it.split(" ").filter { it.isNotBlank() }

        when (parts.size) {
            1 -> {
                BackgroundSize.parseSingle(parts[0])
            }
            2 -> {
                PairBackgroundSize(
                    BackgroundSize.parseSingle(
                        parts[0]
                    ), BackgroundSize.parseSingle(parts[1])
                )
            }
            else -> throw IllegalStateException("Must have 1 or 2 parts in a background-size")
        }

    }
}

class Background internal constructor(_delegate: StyleDelegate = MapDelegate()) :
    HasStyleDelegate(_delegate, "background") {

    @StylesheetDSL
    var color by delegate.byColor("color")

    @StylesheetDSL
    var image by delegate.by("image")

    @StylesheetDSL
    var repeat by delegate.byRepeat("repeat")

    @StylesheetDSL
    var attachment by delegate.byEnum<BackgroundAttachment>("attachment")

    @StylesheetDSL
    var clip by delegate.byEnum<BackgroundBox>("clip")

    @StylesheetDSL
    var origin by delegate.byEnum<BackgroundBox>("origin")

    @StylesheetDSL
    var position by delegate.byBackgroundPosition("position")

    @StylesheetDSL
    var size by delegate.byBackgroundSize("size")

    constructor(
        color: Color? = null,
        image: String? = null,
        repeat: BackgroundRepeat? = null,
        attachment: BackgroundAttachment? = null,
        clip: BackgroundBox? = null,
        origin: BackgroundBox? = null,
        position: BackgroundPosition? = null,
        size: BackgroundSize? = null
    ) : this() {
        this.color = color
        this.image = image
        this.repeat = repeat
        this.attachment = attachment
        this.clip = clip
        this.origin = origin
        this.position = position
        this.size = size
    }

    constructor(block: Background.() -> Unit) : this() {
        block()
    }

    inline operator fun invoke(block: Background.() -> Unit) {
        block()
    }
}