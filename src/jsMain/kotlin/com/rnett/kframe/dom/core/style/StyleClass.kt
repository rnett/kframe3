package com.rnett.kframe.style

import com.rnett.kframe.dom.core.style.Style
import com.rnett.kframe.dom.core.style.StyleElement
import kotlin.math.absoluteValue
import kotlin.random.Random

internal class StyleClassHolder internal constructor(val stylesheetElement: StyleElement) {
    private val usedClasses = mutableSetOf<String>()
    private val styleClasses = mutableMapOf<Int, String>()

    private fun newClass(): String {
        var klass = "style-$Random.nextInt().absoluteValue}"
        while (klass in usedClasses)
            klass = "style-${Random.nextInt().absoluteValue}"

        return klass
    }

    internal fun addStyleClass(styleClass: StyleClass): String {
        if (styleClass.id in styleClasses) // already added
            return styleClasses[styleClass.id]!!

        if (styleClass.klass != null && styleClass.klass in usedClasses)
            error("StyleClass with class ${styleClass.klass} already is used")

        val klass = styleClass.klass ?: newClass()
        styleClasses[styleClass.id] = klass

        stylesheetElement.styles {
            style({ klass(klass) }, styleClass.style)
        }

        return klass
    }

    fun update() {
        stylesheetElement.updateCSS()
    }

    internal fun classFor(styleClass: StyleClass) =
        styleClasses[styleClass.id] ?: error("No StyleClass with id ${styleClass.id} used in this document")
}

private object UsedStyleClasses : MutableSet<Int> by mutableSetOf<Int>()

private fun newId(): Int {
    var klass = Random.nextInt().absoluteValue
    while (klass in UsedStyleClasses)
        klass = Random.nextInt().absoluteValue

    return klass
}

class StyleClass private constructor(internal val style: Style, val klass: String?, internal val id: Int = newId()) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is StyleClass) return false

        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        return id
    }

    companion object {
        operator fun invoke(klass: String? = null, style: Style): StyleClass = StyleClass(style, klass)
        inline operator fun invoke(klass: String? = null, block: Style.() -> Unit): StyleClass =
            StyleClass(klass, Style(block))
    }
}
