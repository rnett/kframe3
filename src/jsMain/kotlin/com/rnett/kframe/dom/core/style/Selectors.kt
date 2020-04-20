package com.rnett.kframe.dom.core.style

enum class AttributeComparisonOperator(val css: String) {
    Equals("="),
    ContainsWord("~="),
    EqualsOrFirstPart("|="),
    StartsWith("^="),
    EndsWith("$="),
    ValueContains("*=")
}

interface CSSSelectorPart {
    val css: String
}

sealed class Selector(override val css: String) : CSSSelectorPart {

    internal class Class internal constructor(klass: String) : Selector(".$klass")

    internal class Id internal constructor(id: String) : Selector("#$id")

    internal object Universal : Selector("*")

    internal class Type internal constructor(type: String) : Selector(type)

    internal class AttributeExists internal constructor(attr: String) : Selector("[$attr]")

    internal class AttributeOperator internal constructor(
        attr: String,
        operator: AttributeComparisonOperator,
        val value: String,
        caseSensitive: Boolean = true
    ) : Selector("[$attr${operator.css}$value${if (!caseSensitive) " i" else ""}]")

    internal class PseudoClass internal constructor(pseudoClass: String) : Selector(":$pseudoClass")

    internal class PseudoElement internal constructor(pseudoElement: String) : Selector("::$pseudoElement")

    internal class Multiple internal constructor(val selectors: List<Selector>) : Selector("") {
        override val css = selectors.joinToString("") { it.css }
        override fun toString() = css

        internal fun addPart(part: Selector): Multiple =
            if (part is Multiple) Multiple(
                selectors + part.selectors
            ) else Multiple(selectors + part)
    }

    override fun toString() = css
}

internal fun Selector.asMultiple() =
    Selector.Multiple(listOf(this))

internal fun Selector.combine(newPart: Selector) =
    if (this is Selector.Multiple)
        this.addPart(newPart)
    else if (newPart is Selector.Multiple)
        Selector.Multiple(listOf(this) + newPart.selectors)
    else
        Selector.Multiple(listOf(this, newPart))

sealed class Combinator(first: CSSSelectorPart, second: Selector, cssSeperator: String) :
    CSSSelectorPart {

    override val css = "$first$cssSeperator$second"

    internal class Descendant(first: CSSSelectorPart, second: Selector) : Combinator(first, second, " ")
    internal class Child(first: CSSSelectorPart, second: Selector) : Combinator(first, second, " > ")
    internal class AdjacentSibling(first: CSSSelectorPart, second: Selector) : Combinator(first, second, " + ")
    internal class GeneralSibling(first: CSSSelectorPart, second: Selector) : Combinator(first, second, " ~ ")

    override fun toString() = css
}

@DslMarker
annotation class SelectorDSL

@DslMarker
annotation class CombinatorDSL

object SelectorBuilderContext {
    @SelectorDSL
    val universal: Selector =
        Selector.Universal

    @SelectorDSL
    fun klass(klass: String): Selector =
        Selector.Class(klass)

    @SelectorDSL
    fun id(id: String): Selector =
        Selector.Id(id)

    @SelectorDSL
    fun type(type: String): Selector =
        Selector.Type(type)

    @SelectorDSL
    fun attributeExists(attr: String): Selector =
        Selector.AttributeExists(attr)

    @SelectorDSL
    fun attributeEquals(attr: String, value: String, caseSensitive: Boolean = true): Selector =
        Selector.AttributeOperator(
            attr,
            AttributeComparisonOperator.Equals,
            value,
            caseSensitive
        )

    @SelectorDSL
    fun attributeContainsWord(attr: String, value: String, caseSensitive: Boolean = true): Selector =
        Selector.AttributeOperator(
            attr,
            AttributeComparisonOperator.ContainsWord,
            value,
            caseSensitive
        )

    @SelectorDSL
    fun attributeFirstPart(attr: String, value: String, caseSensitive: Boolean = true): Selector =
        Selector.AttributeOperator(
            attr,
            AttributeComparisonOperator.EqualsOrFirstPart,
            value,
            caseSensitive
        )

    @SelectorDSL
    fun attributeStartsWith(attr: String, value: String, caseSensitive: Boolean = true): Selector =
        Selector.AttributeOperator(
            attr,
            AttributeComparisonOperator.StartsWith,
            value,
            caseSensitive
        )

    @SelectorDSL
    fun attributeEndsWith(attr: String, value: String, caseSensitive: Boolean = true): Selector =
        Selector.AttributeOperator(
            attr,
            AttributeComparisonOperator.EndsWith,
            value,
            caseSensitive
        )

    @SelectorDSL
    fun attributeValueContains(attr: String, value: String, caseSensitive: Boolean = true): Selector =
        Selector.AttributeOperator(
            attr,
            AttributeComparisonOperator.ValueContains,
            value,
            caseSensitive
        )

    @SelectorDSL
    fun pseudoClass(pseudoClass: String): Selector =
        Selector.PseudoClass(pseudoClass)

    @SelectorDSL
    val active = pseudoClass("active")

    @SelectorDSL
    val checked = pseudoClass("checked")

    @SelectorDSL
    val disabled = pseudoClass("disabled")

    @SelectorDSL
    val enabled = pseudoClass("enabled")

    @SelectorDSL
    val first = pseudoClass("first")

    @SelectorDSL
    val firstChild =
        pseudoClass("first-child")

    @SelectorDSL
    val firstOfType =
        pseudoClass("first-of-type")

    @SelectorDSL
    val focus = pseudoClass("focus")

    @SelectorDSL
    val focusWithin =
        pseudoClass("focus-within")

    @SelectorDSL
    val hover = pseudoClass("hover")

    @SelectorDSL
    val inRange = pseudoClass("in-range")

    @SelectorDSL
    val invalid = pseudoClass("invalid")

    @SelectorDSL
    val lastChild = pseudoClass("last-child")

    @SelectorDSL
    val lastOfType =
        pseudoClass("last-of-type")

    @SelectorDSL
    val link = pseudoClass("link")

    @SelectorDSL
    fun nthChild(n: Int) =
        pseudoClass("nth-child($n)")

    @SelectorDSL
    fun nthChild(child: String) =
        pseudoClass("nth-child($child)")

    @SelectorDSL
    fun nthLastChild(n: Int) =
        pseudoClass("nth-last-child($n)")

    @SelectorDSL
    fun nthLastChild(child: String) =
        pseudoClass("nth-last-child($child)")

    @SelectorDSL
    fun nthOfType(n: Int) =
        pseudoClass("nth-of-type($n)")

    @SelectorDSL
    fun nthOfType(child: String) =
        pseudoClass("nth-of-type($child)")

    @SelectorDSL
    fun nthLastOfType(n: Int) =
        pseudoClass("nth-last-of-type($n)")

    @SelectorDSL
    fun nthLastOfType(child: String) =
        pseudoClass("nth-last-of-type($child)")

    @SelectorDSL
    val onlyChild = pseudoClass("only-child")

    @SelectorDSL
    val onlyChildOfType =
        pseudoClass("only-child-of-type")

    @SelectorDSL
    val optional = pseudoClass("optional")

    @SelectorDSL
    val outOfRange =
        pseudoClass("out-of-range")

    @SelectorDSL
    val readOnly = pseudoClass("read-only")

    @SelectorDSL
    val readWrite = pseudoClass("read-write")

    @SelectorDSL
    val required = pseudoClass("required")

    @SelectorDSL
    val target = pseudoClass("target")

    @SelectorDSL
    val valid = pseudoClass("valid")

    @SelectorDSL
    val visited = pseudoClass("visited")

    @SelectorDSL
    fun pseudoElement(pseudoElement: String): Selector =
        Selector.PseudoElement(pseudoElement)

    @SelectorDSL
    val after = pseudoElement("after")

    @SelectorDSL
    val before = pseudoElement("before")

    @SelectorDSL
    val cue = pseudoElement("cue")

    @SelectorDSL
    val cueRegion =
        pseudoElement("cue-region")

    @SelectorDSL
    val firstLetter =
        pseudoElement("first-letter")

    @SelectorDSL
    val firstLine =
        pseudoElement("first-line")

    @SelectorDSL
    val selection = pseudoElement("selection")

    // add to existing selector methods

    @SelectorDSL
    fun Selector.klass(klass: String): Selector = combine(
        SelectorBuilderContext.klass(klass)
    )

    @SelectorDSL
    fun Selector.id(id: String): Selector = combine(
        SelectorBuilderContext.id(id)
    )

    @SelectorDSL
    fun Selector.type(type: String): Selector = combine(
        SelectorBuilderContext.type(type)
    )

    @SelectorDSL
    fun Selector.attributeExists(attr: String): Selector = combine(
        SelectorBuilderContext.attributeExists(attr)
    )

    @SelectorDSL
    fun Selector.attributeEquals(attr: String, value: String, caseSensitive: Boolean = true): Selector =
        combine(
            SelectorBuilderContext.attributeEquals(
                attr,
                value,
                caseSensitive
            )
        )

    @SelectorDSL
    fun Selector.attributeContainsWord(attr: String, value: String, caseSensitive: Boolean = true): Selector =
        combine(
            SelectorBuilderContext.attributeContainsWord(
                attr,
                value,
                caseSensitive
            )
        )

    @SelectorDSL
    fun Selector.attributeFirstPart(attr: String, value: String, caseSensitive: Boolean = true): Selector =
        combine(
            SelectorBuilderContext.attributeFirstPart(
                attr,
                value,
                caseSensitive
            )
        )

    @SelectorDSL
    fun Selector.attributeStartsWith(attr: String, value: String, caseSensitive: Boolean = true): Selector =
        combine(
            SelectorBuilderContext.attributeStartsWith(
                attr,
                value,
                caseSensitive
            )
        )

    @SelectorDSL
    fun Selector.attributeEndsWith(attr: String, value: String, caseSensitive: Boolean = true): Selector =
        combine(
            SelectorBuilderContext.attributeEndsWith(
                attr,
                value,
                caseSensitive
            )
        )

    @SelectorDSL
    fun Selector.attributeValueContains(attr: String, value: String, caseSensitive: Boolean = true): Selector =
        combine(
            SelectorBuilderContext.attributeValueContains(
                attr,
                value,
                caseSensitive
            )
        )

    @SelectorDSL
    fun Selector.pseudoClass(pseudoClass: String): Selector = combine(
        SelectorBuilderContext.pseudoClass(pseudoClass)
    )

    @SelectorDSL
    val Selector.active
        inline get() = pseudoClass("active")

    @SelectorDSL
    val Selector.checked
        inline get() = pseudoClass("checked")

    @SelectorDSL
    val Selector.disabled
        inline get() = pseudoClass("disabled")

    @SelectorDSL
    val Selector.enabled
        inline get() = pseudoClass("enabled")

    @SelectorDSL
    val Selector.first
        inline get() = pseudoClass("first")

    @SelectorDSL
    val Selector.firstChild
        inline get() = pseudoClass("first-child")

    @SelectorDSL
    val Selector.firstOfType
        inline get() = pseudoClass("first-of-type")

    @SelectorDSL
    val Selector.focus
        inline get() = pseudoClass("focus")

    @SelectorDSL
    val Selector.focusWithin
        inline get() = pseudoClass("focus-within")

    @SelectorDSL
    val Selector.hover
        inline get() = pseudoClass("hover")

    @SelectorDSL
    val Selector.inRange
        inline get() = pseudoClass("in-range")

    @SelectorDSL
    val Selector.invalid
        inline get() = pseudoClass("invalid")

    @SelectorDSL
    val Selector.lastChild
        inline get() = pseudoClass("last-child")

    @SelectorDSL
    val Selector.lastOfType
        inline get() = pseudoClass("last-of-type")

    @SelectorDSL
    val Selector.link
        inline get() = pseudoClass("link")

    @SelectorDSL
    fun Selector.nthChild(n: Int) = pseudoClass("nth-child($n)")

    @SelectorDSL
    fun Selector.nthChild(child: String) = pseudoClass("nth-child($child)")

    @SelectorDSL
    fun Selector.nthLastChild(n: Int) = pseudoClass("nth-last-child($n)")

    @SelectorDSL
    fun Selector.nthLastChild(child: String) = pseudoClass("nth-last-child($child)")

    @SelectorDSL
    fun Selector.nthOfType(n: Int) = pseudoClass("nth-of-type($n)")

    @SelectorDSL
    fun Selector.nthOfType(child: String) = pseudoClass("nth-of-type($child)")

    @SelectorDSL
    fun Selector.nthLastOfType(n: Int) = pseudoClass("nth-last-of-type($n)")

    @SelectorDSL
    fun Selector.nthLastOfType(child: String) = pseudoClass("nth-last-of-type($child)")

    @SelectorDSL
    val Selector.onlyChild
        inline get() = pseudoClass("only-child")

    @SelectorDSL
    val Selector.onlyChildOfType
        inline get() = pseudoClass("only-child-of-type")

    @SelectorDSL
    val Selector.optional
        inline get() = pseudoClass("optional")

    @SelectorDSL
    val Selector.outOfRange
        inline get() = pseudoClass("out-of-range")

    @SelectorDSL
    val Selector.readOnly
        inline get() = pseudoClass("read-only")

    @SelectorDSL
    val Selector.readWrite
        inline get() = pseudoClass("read-write")

    @SelectorDSL
    val Selector.required
        inline get() = pseudoClass("required")

    @SelectorDSL
    val Selector.target
        inline get() = pseudoClass("target")

    @SelectorDSL
    val Selector.valid
        inline get() = pseudoClass("valid")

    @SelectorDSL
    val Selector.visited
        inline get() = pseudoClass("visited")

    @SelectorDSL
    fun Selector.pseudoElement(pseudoElement: String): Selector =
        combine(SelectorBuilderContext.pseudoElement(pseudoElement))

    @SelectorDSL
    val Selector.after
        inline get() = pseudoElement("after")

    @SelectorDSL
    val Selector.before
        inline get() = pseudoElement("before")

    @SelectorDSL
    val Selector.cue
        inline get() = pseudoElement("cue")

    @SelectorDSL
    val Selector.cueRegion
        inline get() = pseudoElement("cue-region")

    @SelectorDSL
    val Selector.firstLetter
        inline get() = pseudoElement("first-letter")

    @SelectorDSL
    val Selector.firstLine
        inline get() = pseudoElement("first-line")

    @SelectorDSL
    val Selector.selection
        inline get() = pseudoElement("selection")

    //TODO do I actually want the operators?

    @CombinatorDSL
    infix fun CSSSelectorPart.descendant(selector: Selector): Combinator =
        Combinator.Descendant(this, selector)

    @CombinatorDSL
    operator fun CSSSelectorPart.times(selector: Selector): Combinator = this descendant selector

    @CombinatorDSL
    infix fun CSSSelectorPart.child(selector: Selector): Combinator =
        Combinator.Child(this, selector)

    @CombinatorDSL
    operator fun CSSSelectorPart.div(selector: Selector): Combinator = this child selector

    @CombinatorDSL
    infix fun CSSSelectorPart.adjacentSibling(selector: Selector): Combinator =
        Combinator.AdjacentSibling(this, selector)

    @SelectorDSL
    operator fun CSSSelectorPart.plus(selector: Selector): Combinator = this adjacentSibling selector

    @CombinatorDSL
    infix fun CSSSelectorPart.sibling(selector: Selector): Combinator =
        Combinator.GeneralSibling(this, selector)

    @CombinatorDSL
    operator fun CSSSelectorPart.minus(selector: Selector): Combinator = this sibling selector
}
typealias SelectorBuilder = SelectorBuilderContext.() -> CSSSelectorPart

@SelectorDSL
inline fun selector(builder: SelectorBuilder) =
    CSSSelector(
        SelectorBuilderContext.run(builder)
    )


class CSSSelector(val selectors: List<CSSSelectorPart>) {
    constructor(vararg selectors: CSSSelectorPart) : this(selectors.toList())

    val css = selectors.joinToString(", ")

    override fun toString() = css

    operator fun plus(other: CSSSelector) =
        CSSSelector(selectors + other.selectors)
}

//TODO query document elements by CSS selector (document.query methods)
