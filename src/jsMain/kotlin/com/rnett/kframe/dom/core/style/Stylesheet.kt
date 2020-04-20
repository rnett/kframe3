package com.rnett.kframe.dom.core.style

import com.rnett.kframe.dom.core.ElementHost
import com.rnett.kframe.dom.core.KFrameDSL
import com.rnett.kframe.dom.core.MetaElement
import com.rnett.kframe.dom.core.MetaElementHost


// scoped and global stylesheet elements
class Stylesheet {
    private val styles = mutableMapOf<CSSSelector, Style>()

    @StylesheetDSL
    fun style(selector: CSSSelector, style: Style) {
        styles[selector] = style
    }

    @StylesheetDSL
    inline operator fun set(selector: CSSSelector, style: Style) = style(selector, style)

    @StylesheetDSL
    inline fun style(selector: SelectorBuilder, style: Style) = style(
        selector(selector), style
    )

    @StylesheetDSL
    inline operator fun set(selector: SelectorBuilder, style: Style) = style(selector, style)

    @StylesheetDSL
    inline fun style(selector: CSSSelector, style: Style.() -> Unit) = style(
        selector,
        Style(style)
    )

    @StylesheetDSL
    inline operator fun set(selector: CSSSelector, style: Style.() -> Unit) = style(selector, style)

    @StylesheetDSL
    inline fun style(selector: SelectorBuilder, style: Style.() -> Unit) = style(
        selector(selector), style
    )

    @StylesheetDSL
    inline operator fun set(selector: SelectorBuilder, style: Style.() -> Unit) = style(selector, style)

    fun toCSS() = styles.entries.joinToString("\n\n") {
        "${it.key} {\n${it.value.toCSS(pretty = true).prependIndent("\t")}\n}"
    }

    override fun toString() = toCSS()

    companion object {
        inline operator fun invoke(builder: Stylesheet.() -> Unit) = Stylesheet()
            .also(builder)
    }
}

//TODO won't work for scoped if I use meta element
class StyleElement(parent: MetaElementHost) : MetaElement<StyleElement>(parent, "style") {
    val internalStylesheet = Stylesheet()

    inline operator fun invoke(block: Stylesheet.() -> Unit): StyleElement {
        internalStylesheet.block()
        updateCSS()
        return this
    }

    inline fun styles(block: Stylesheet.() -> Unit) = invoke(block)

    fun updateCSS() {
        underlying.innerHTML = "\n" + internalStylesheet.toCSS() + "\n"
    }

    override fun addElement(element: ElementHost) {
        error("Can't add children to Style elements")
    }
}

@KFrameDSL
inline fun MetaElementHost.stylesheet(block: Stylesheet.() -> Unit) = StyleElement(
    this
)(block)
