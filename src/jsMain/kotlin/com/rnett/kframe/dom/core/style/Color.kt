package com.rnett.kframe.dom.core.style

import com.rnett.kframe.dom.core.JSValue
import com.rnett.kframe.utils.DelegateInterface
import com.rnett.kframe.utils.by
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.properties.ReadWriteProperty

inline fun DelegateInterface.byColor(key: String): ReadWriteProperty<Any?, Color?> = by(key) {
    it?.let {
        Color(it)
    }
}

sealed class Color : JSValue {
    abstract fun getRGB(): Triple<Int, Int, Int>
    abstract val alpha: Float

    override fun toString(): String = toJS() ?: "null"

    fun toHsl(): HslColor {
        if (this is HslColor)
            return this

        val rgb = getRGB()
        val hsl = rgbToHSL(
            rgb.first,
            rgb.second,
            rgb.third
        )
        return HslColor(hsl.first, hsl.second, hsl.third, alpha)
    }

    fun toRgb(): RgbColor {
        if (this is RgbColor)
            return this

        val rgb = getRGB()
        return RgbColor(rgb.first, rgb.second, rgb.third, alpha)
    }

    fun toNamed(): NamedColor? {
        if (this is NamedColor)
            return this

        val rgb = getRGB()
        return NamedColor.colors.values.find { it.red == rgb.first && it.green == rgb.second && it.blue == rgb.third }
    }

    fun toHex(): HexColor {
        if (this is HexColor)
            return this

        val rgb = getRGB()
        return HexColor(
            "#${rgb.first.formatHex()}${rgb.second.formatHex()}${rgb.third.formatHex()}${(alpha * 255).toInt()
                .formatHex()}"
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Color) return false

        if (alpha != other.alpha) return false

        if (getRGB() != other.getRGB()) return false

        return true
    }

    override fun hashCode(): Int {
        return alpha.hashCode() * 331 + getRGB().hashCode()
    }


    companion object {

        fun rbg(red: Int, green: Int, blue: Int) =
            RgbColor(red, green, blue)
        fun hex(hex: String) = HexColor(hex)
        fun hsl(hue: Int, sat: Int, lightness: Int) =
            HslColor(hue, sat, lightness)
        fun rbga(red: Int, green: Int, blue: Int, transparency: Float) =
            RgbColor(red, green, blue, transparency)
        fun hsla(hue: Int, sat: Int, lightness: Int, transparency: Float) =
            HslColor(hue, sat, lightness, transparency)

        //RED'S
        val Indianred = NamedColor("indianred", 205, 92, 92)
        val Lightcoral = NamedColor("lightcoral", 240, 128, 128)
        val Salmon = NamedColor("salmon", 250, 128, 114)
        val Darksalmon = NamedColor("darksalmon", 233, 150, 122)
        val Lightsalmon = NamedColor("lightsalmon", 255, 160, 122)
        val Crimson = NamedColor("crimson", 220, 20, 60)
        val Red = NamedColor("red", 255, 0, 0)
        val Firebrick = NamedColor("firebrick", 178, 34, 34)
        val Darkred = NamedColor("darkred", 139, 0, 0)

        //PINK'S
        val Pink = NamedColor("pink", 255, 192, 203)
        val Lightpink = NamedColor("lightpink", 255, 182, 193)
        val Hotpink = NamedColor("hotpink", 255, 105, 180)
        val Deeppink = NamedColor("deeppink", 255, 20, 147)
        val Mediumvioletred =
            NamedColor("mediumvioletred", 199, 21, 133)
        val Palevioletred =
            NamedColor("palevioletred", 219, 112, 147)

        //ORANGE'S
        val Coral = NamedColor("coral", 255, 127, 80)
        val Tomato = NamedColor("tomato", 255, 99, 71)
        val Orangered = NamedColor("orangered", 255, 69, 0)
        val Darkorange = NamedColor("darkorange", 255, 140, 0)
        val Orange = NamedColor("orange", 255, 165, 0)

        //YELLOW'S
        val Gold = NamedColor("gold", 255, 215, 0)
        val Yellow = NamedColor("yellow", 255, 255, 0)
        val Lightyellow = NamedColor("lightyellow", 255, 255, 224)
        val Lemonchiffon = NamedColor("lemonchiffon", 255, 250, 205)
        val Lightgoldenrodyellow =
            NamedColor("lightgoldenrodyellow", 250, 250, 210)
        val Papayawhip = NamedColor("papayawhip", 255, 239, 213)
        val Moccasin = NamedColor("moccasin", 255, 228, 181)
        val Peachpuff = NamedColor("peachpuff", 255, 218, 185)
        val Palegoldenrod =
            NamedColor("palegoldenrod", 238, 232, 170)
        val Khaki = NamedColor("khaki", 240, 230, 140)
        val Darkkhaki = NamedColor("darkkhaki", 189, 183, 107)

        //PURPLE'S
        val Lavender = NamedColor("lavender", 230, 230, 250)
        val Thistle = NamedColor("thistle", 216, 191, 216)
        val Plum = NamedColor("plum", 221, 160, 221)
        val Violet = NamedColor("violet", 238, 130, 238)
        val Orchid = NamedColor("orchid", 218, 112, 214)
        val Fuchsia = NamedColor("fuchsia", 255, 0, 255)
        val Magenta = NamedColor("magenta", 255, 0, 255)
        val Mediumorchid = NamedColor("mediumorchid", 186, 85, 211)
        val Mediumpurple = NamedColor("mediumpurple", 147, 112, 219)
        val Rebeccapurple = NamedColor("rebeccapurple", 102, 51, 153)
        val Blueviolet = NamedColor("blueviolet", 138, 43, 226)
        val Darkviolet = NamedColor("darkviolet", 148, 0, 211)
        val Darkorchid = NamedColor("darkorchid", 153, 50, 204)
        val Darkmagenta = NamedColor("darkmagenta", 139, 0, 139)
        val Purple = NamedColor("purple", 128, 0, 128)
        val Indigo = NamedColor("indigo", 75, 0, 130)
        val Slateblue = NamedColor("slateblue", 106, 90, 205)
        val Darkslateblue = NamedColor("darkslateblue", 72, 61, 139)
        val Mediumslateblue =
            NamedColor("mediumslateblue", 123, 104, 238)

        //GREEN'S
        val Greenyellow = NamedColor("greenyellow", 173, 255, 47)
        val Chartreuse = NamedColor("chartreuse", 127, 255, 0)
        val Lawngreen = NamedColor("lawngreen", 124, 252, 0)
        val Lime = NamedColor("lime", 0, 255, 0)
        val Limegreen = NamedColor("limegreen", 50, 205, 50)
        val Palegreen = NamedColor("palegreen", 152, 251, 152)
        val Lightgreen = NamedColor("lightgreen", 144, 238, 144)
        val Mediumspringgreen =
            NamedColor("mediumspringgreen", 0, 250, 154)
        val Springgreen = NamedColor("springgreen", 0, 255, 127)
        val Mediumseagreen =
            NamedColor("mediumseagreen", 60, 179, 113)
        val Seagreen = NamedColor("seagreen", 46, 139, 87)
        val Forestgreen = NamedColor("forestgreen", 34, 139, 34)
        val Green = NamedColor("green", 0, 128, 0)
        val Darkgreen = NamedColor("darkgreen", 0, 100, 0)
        val Yellowgreen = NamedColor("yellowgreen", 154, 205, 50)
        val Olivedrab = NamedColor("olivedrab", 107, 142, 35)
        val Olive = NamedColor("olive", 128, 128, 0)
        val Darkolivegreen =
            NamedColor("darkolivegreen", 85, 107, 47)
        val Mediumaquamarine =
            NamedColor("mediumaquamarine", 102, 205, 170)
        val Darkseagreen = NamedColor("darkseagreen", 143, 188, 139)
        val Lightseagreen = NamedColor("lightseagreen", 32, 178, 170)
        val Darkcyan = NamedColor("darkcyan", 0, 139, 139)
        val Teal = NamedColor("teal", 0, 128, 128)

        //BLUE'S
        val Aqua = NamedColor("Aqua", 0, 255, 255)
        val Cyan = NamedColor("cyan", 0, 255, 255)
        val Lightcyan = NamedColor("lightcyan", 224, 255, 255)
        val Paleturquoise =
            NamedColor("paleturquoise", 175, 238, 238)
        val Aquamarine = NamedColor("Aquamarine", 127, 255, 212)
        val Turquoise = NamedColor("turquoise", 64, 224, 208)
        val Mediumturquoise =
            NamedColor("mediumturquoise", 72, 209, 204)
        val Darkturquoise = NamedColor("darkturquoise", 0, 206, 209)
        val Cadetblue = NamedColor("cadetblue", 95, 158, 160)
        val Steelblue = NamedColor("steelblue", 70, 130, 180)
        val Lightsteelblue =
            NamedColor("lightsteelblue", 176, 196, 222)
        val Powderblue = NamedColor("powderblue", 176, 224, 230)
        val Lightblue = NamedColor("lightblue", 173, 216, 230)
        val Skyblue = NamedColor("skyblue", 135, 206, 235)
        val Lightskyblue = NamedColor("lightskyblue", 135, 206, 250)
        val Deepskyblue = NamedColor("deepskyblue", 0, 191, 255)
        val Dodgerblue = NamedColor("dodgerblue", 30, 144, 255)
        val Cornflowerblue =
            NamedColor("cornflowerblue", 100, 149, 237)
        val Royalblue = NamedColor("royalblue", 65, 105, 225)
        val Blue = NamedColor("blue", 0, 0, 255)
        val Mediumblue = NamedColor("mediumblue", 0, 0, 205)
        val Darkblue = NamedColor("darkblue", 0, 0, 139)
        val Navy = NamedColor("navy", 0, 0, 128)
        val Midnightblue = NamedColor("midnightblue", 25, 25, 112)

        //BROWN'S
        val Cornsilk = NamedColor("cornsilk", 255, 248, 220)
        val Blanchedalmond =
            NamedColor("blanchedalmond", 255, 235, 205)
        val Bisque = NamedColor("bisque", 255, 228, 196)
        val Navajowhite = NamedColor("navajowhite", 255, 222, 173)
        val Wheat = NamedColor("wheat", 245, 222, 179)
        val Burlywood = NamedColor("burlywood", 222, 184, 135)
        val Tan = NamedColor("tan", 210, 180, 140)
        val Rosybrown = NamedColor("rosybrown", 188, 143, 143)
        val Sandybrown = NamedColor("sandybrown", 244, 164, 96)
        val Goldenrod = NamedColor("goldenrod", 218, 165, 32)
        val Darkgoldenrod = NamedColor("darkgoldenrod", 184, 134, 11)
        val Peru = NamedColor("peru", 205, 133, 63)
        val Chocolate = NamedColor("chocolate", 210, 105, 30)
        val Saddlebrown = NamedColor("saddlebrown", 139, 69, 19)
        val Sienna = NamedColor("sienna", 160, 82, 45)
        val Brown = NamedColor("brown", 165, 42, 42)
        val Maroon = NamedColor("maroon", 128, 0, 0)

        //WHITE'S
        val White = NamedColor("white", 255, 255, 255)
        val Snow = NamedColor("snow", 255, 250, 250)
        val Honeydew = NamedColor("honeydew", 240, 255, 240)
        val Mintcream = NamedColor("mintcream", 245, 255, 250)
        val Azure = NamedColor("Azure", 240, 255, 255)
        val Aliceblue = NamedColor("Aliceblue", 240, 248, 255)
        val Ghostwhite = NamedColor("ghostwhite", 248, 248, 255)
        val Whitesmoke = NamedColor("whitesmoke", 245, 245, 245)
        val Seashell = NamedColor("seashell", 255, 245, 238)
        val Beige = NamedColor("beige", 245, 245, 220)
        val Oldlace = NamedColor("oldlace", 253, 245, 230)
        val Floralwhite = NamedColor("floralwhite", 255, 250, 240)
        val Ivory = NamedColor("ivory", 255, 255, 240)
        val Antiquewhite = NamedColor("Antiquewhite", 250, 235, 215)
        val Linen = NamedColor("linen", 250, 240, 230)
        val Lavenderblush =
            NamedColor("lavenderblush", 255, 240, 245)
        val Mistyrose = NamedColor("mistyrose", 255, 228, 225)

        //GRAY'S
        val Gainsboro = NamedColor("gainsboro", 220, 220, 220)
        val Lightgray = NamedColor("lightgray", 211, 211, 211)
        val Silver = NamedColor("silver", 192, 192, 192)
        val Darkgray = NamedColor("darkgray", 169, 169, 169)
        val Gray = NamedColor("gray", 128, 128, 128)
        val Dimgray = NamedColor("dimgray", 105, 105, 105)
        val Lightslategray =
            NamedColor("lightslategray", 119, 136, 153)
        val Slategray = NamedColor("slategray", 112, 128, 144)
        val Darkslategray = NamedColor("darkslategray", 47, 79, 79)
        val Black = NamedColor("black", 0, 0, 0)

        val Transparent = NamedColor("transparent", -1, -1, -1)
        val None = NamedColor("none", -2, -2, -2)

        fun fromString(raw: String): Color {
            return when {
                NamedColor.colors.containsKey(raw.toLowerCase()) -> NamedColor.colors.getValue(raw.toLowerCase())
                raw.startsWith("#") -> HexColor(raw)
                raw.startsWith("rgb") -> RgbColor(raw)
                raw.startsWith("hsl") -> HslColor(raw)
                else -> RawColor(raw)
            }
        }

        operator fun invoke(raw: String) =
            fromString(raw)

        /**
         * Convert a RGB Color to it corresponding HSL values.
         *
         */
        fun rgbToHSL(r: Int, g: Int, b: Int): Triple<Int, Int, Int> {
            //  Get RGB values in the range 0 - 1

            val r = r / 255F
            val g = g / 255F
            val b = b / 255F

            //	Minimum and Maximum RGB values are used in the HSL calculations

            val min = min(r, min(g, b))
            val max = max(r, max(g, b))

            //  Calculate the Hue

            var h = 0f

            when (max) {
                min -> h = 0f
                r -> h = (60 * (g - b) / (max - min) + 360) % 360
                g -> h = 60 * (b - r) / (max - min) + 120
                b -> h = 60 * (r - g) / (max - min) + 240

                //  Calculate the Luminance

                //  Calculate the Saturation
            }

            //  Calculate the Luminance

            val l = (max + min) / 2

            //  Calculate the Saturation

            var s = 0f

            s = when {
                max == min -> 0f
                l <= .5f -> (max - min) / (max + min)
                else -> (max - min) / (2f - max - min)
            }

            return Triple(h.roundToInt(), (s * 100).roundToInt(), (l * 100).roundToInt())
        }

        /**
         * Convert HSL values to a RGB Color.
         *
         * @param h Hue is specified as degrees in the range 0 - 360.
         * @param s Saturation is specified as a percentage in the range 1 - 100.
         * @param l Luminance is specified as a percentage in the range 1 - 100.
         */
        fun hslToRGB(hue: Int, sat: Int, lum: Int): Triple<Int, Int, Int> {
            if (sat < 0.0f || sat > 100.0f) {
                val message = "Color parameter outside of expected range - Saturation"
                throw IllegalArgumentException(message)
            }

            if (lum < 0.0f || lum > 100.0f) {
                val message = "Color parameter outside of expected range - Luminance"
                throw IllegalArgumentException(message)
            }

            //  Formula needs all values between 0 - 1.

            val h = (hue % 360.0f) / 360f
            val s = sat / 100f
            val l = lum / 100f

            var q = 0f

            q = if (l < 0.5)
                l * (1 + s)
            else
                l + s - s * l

            val p = 2 * l - q

            var r = max(0.0f,
                hueToRGB(p, q, h + 1.0f / 3.0f)
            )
            var g = max(0.0f, hueToRGB(p, q, h))
            var b = max(0.0f,
                hueToRGB(p, q, h - 1.0f / 3.0f)
            )

            r = min(r, 1.0f)
            g = min(g, 1.0f)
            b = min(b, 1.0f)

            return Triple(r.roundToInt(), g.roundToInt(), b.roundToInt())
        }

        private fun hueToRGB(p: Float, q: Float, h: Float): Float {
            var h = h
            if (h < 0) h += 1f

            if (h > 1) h -= 1f

            if (6 * h < 1) {
                return p + (q - p) * 6f * h
            }

            if (2 * h < 1) {
                return q
            }

            return if (3 * h < 2) {
                p + (q - p) * 6f * (2.0f / 3.0f - h)
            } else p

        }

        fun hexToRGB(hex: String): Pair<Triple<Int, Int, Int>, Float> {
            val withAlpha = Regex("^#?([a-f0-9]{2})([a-f0-9]{2})([a-fd]{2})([a-f0-9]{2})", RegexOption.IGNORE_CASE)
            val withoutAlpha = Regex("^#?([a-f0-9]{2})([a-fd]{2})([a-f0-9]{2})", RegexOption.IGNORE_CASE)
            val c = (withAlpha.matchEntire(hex) ?: withoutAlpha.matchEntire(hex))!!.groupValues.drop(1)


            return Pair(Triple(c[0].toInt(16), c[1].toInt(16), c[2].toInt(16)), c.getOrElse(3) { "0" }.toInt(16) / 255f)
        }
    }
}

val String.color get() = Color(this)

class NamedColor internal constructor(val name: String, val red: Int, val green: Int, val blue: Int) : Color() {

    override fun getRGB(): Triple<Int, Int, Int> = Triple(red, green, blue)

    override fun toString(): String = toJS()

    override val alpha: Float
        get() = if (this == Transparent) 0f else 1f

    override fun toJS(): String = if (name == "none") "" else name

    companion object {

        @Suppress("ObjectPropertyName")
        private val _colors = mutableMapOf<String, NamedColor>()
        val colors: Map<String, NamedColor> get() = _colors

    }

    override fun hashCode(): Int {
        return name.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        return if (other is NamedColor)
            name == other.name
        else super.equals(other)
    }

    init {
        _colors[this.name] = this
    }

}

class HexColor(val hexString: String) : Color() {
    override fun getRGB(): Triple<Int, Int, Int> = hexToRGB(
        hexString
    ).first

    override val alpha: Float
        get() = hexToRGB(hexString).second

    override fun toJS(): String = hexString

    override fun toString(): String = toJS()

}

class HslColor(val hue: Int, val saturation: Int, val lightness: Int, override val alpha: Float = 0f) : Color() {

    override fun getRGB(): Triple<Int, Int, Int> =
        hslToRGB(hue, saturation, lightness)

    override fun toJS(): String =
        if (alpha >= 1f) "hsl($hue, $saturation%, $lightness%)" else "hsla($hue, $saturation%, $lightness%, $alpha)"

    companion object {
        operator fun invoke(raw: String): HslColor {
            val str = raw.removePrefix("hsl").removePrefix("a").trim('(', ')').replace(" ", "")
            val parts = str.split(",").map { it.trim().trim('%') }

            return if (parts.count() > 3)
                HslColor(
                    parts[0].toInt(),
                    parts[1].toInt(),
                    parts[2].toInt(),
                    parts[3].toFloat()
                )
            else
                HslColor(
                    parts[0].toInt(),
                    parts[1].toInt(),
                    parts[2].toInt()
                )
        }
    }

    override fun toString(): String = toJS()

}

class RgbColor(val red: Int, val green: Int, val blue: Int, override val alpha: Float = 0f) : Color() {

    override fun getRGB(): Triple<Int, Int, Int> = Triple(red, green, blue)

    override fun toJS(): String = if (alpha >= 1f) "rgb($red, $green, $blue)" else "rgba($red, $green, $blue, $alpha)"

    companion object {
        operator fun invoke(raw: String): RgbColor {
            val str = raw.removePrefix("rgb").removePrefix("a").trim('(', ')').replace(" ", "")
            val parts = str.split(",").map { it.trim() }

            return if (parts.count() > 3)
                RgbColor(
                    parts[0].toInt(),
                    parts[1].toInt(),
                    parts[2].toInt(),
                    parts[3].toFloat()
                )
            else
                RgbColor(
                    parts[0].toInt(),
                    parts[1].toInt(),
                    parts[2].toInt()
                )
        }
    }

    override fun toString(): String = toJS()

}

class RawColor(val raw: String) : Color() {
    override fun getRGB(): Triple<Int, Int, Int> = Triple(-1, -1, -1)

    override val alpha: Float = -1f

    override fun toJS(): String = raw


    override fun toString(): String = toJS()

}

fun Int.formatHex(): String = this.toString(16).padStart(2, '0').takeLast(2)