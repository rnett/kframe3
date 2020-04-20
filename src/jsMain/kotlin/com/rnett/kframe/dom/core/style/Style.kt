package com.rnett.kframe.dom.core.style

import com.rnett.kframe.dom.core.Element
import com.rnett.kframe.dom.providers.*
import com.rnett.kframe.utils.DelegateInterface
import com.rnett.kframe.utils.StringDelegatable
import com.rnett.kframe.utils.by
import kotlin.properties.ReadWriteProperty

@DslMarker
annotation class StylesheetDSL


interface StyleDelegate : DelegateInterface

class PrefixStyleDelegate(val prefix: String, val inner: StyleDelegate) :
    StyleDelegate {
    override fun get(key: String) = inner[prefix + key]

    override fun set(key: String, value: String?) {
        inner[prefix + key] = value
    }

    internal fun hasMap(): Boolean = inner is MapDelegate || (inner is PrefixStyleDelegate && inner.hasMap())

    internal fun getMap(): Map<String, String> {
        if (!hasMap())
            throw IllegalStateException("Is not based on a MapDelegate")

        return if (inner is MapDelegate)
            inner
        else if (inner is PrefixStyleDelegate)
            inner.getMap()
        else
            throw IllegalStateException("Is not based on a MapDelegate")
    }
}

fun StyleDelegate.withPrefixBefore(prefix: String) = if (this is PrefixStyleDelegate)
    PrefixStyleDelegate(prefix + this.prefix, this.inner)
else
    PrefixStyleDelegate(prefix, this)

fun StyleDelegate.withPrefixAfter(prefix: String) = if (this is PrefixStyleDelegate)
    PrefixStyleDelegate(this.prefix + prefix, this.inner)
else
    PrefixStyleDelegate(prefix, this)

abstract class HasStyleDelegate(delegate: StyleDelegate, internal val prefix: String?) {

    internal val delegate = if (prefix != null) delegate.withPrefixAfter("$prefix-") else delegate

    internal fun getMap() =
        (if (delegate is PrefixStyleDelegate && delegate.hasMap()) delegate.getMap() else if (delegate is MapDelegate) delegate else null)?.toMap()

    internal fun loadFrom(other: HasStyleDelegate, defaultPrefix: String? = null) {
        if (other.delegate is PrefixStyleDelegate && other.delegate.hasMap()) {
            other.delegate.getMap().forEach {
                this.delegate[(if (other.prefix == null) defaultPrefix?.let { "$it-" } ?: "" else "") + it.key] =
                    it.value
            }
        } else if (other.delegate is MapDelegate) {
            other.delegate.forEach {
                this.delegate[(if (other.prefix == null) defaultPrefix?.let { "$it-" } ?: "" else "") + it.key] =
                    it.value
            }
        }
    }
}

class MapDelegate(private val internalMap: MutableMap<String, String> = mutableMapOf()) : StyleDelegate,
    Map<String, String> by internalMap {
    override fun get(key: String) = internalMap[key]

    override fun set(key: String, value: String?) {
        if (value == null)
            internalMap.remove(key)
        else
            internalMap[key] = value
    }
}

fun String.camelToDash() = Regex("([A-Z])").replace(this.decapitalize()) { "-" + it.value.toLowerCase() }

class Style internal constructor(internal val provider: StyleProvider) : StringDelegatable(),
    StyleDelegate, Rectifiable<Style>, ExistenceAttachable {
    constructor() : this(VirtualStyleProvider(true))
    internal constructor(element: Element<*>) : this(StyleProviderWrapper(element.provider.styleProvider()))

    override fun attach(provider: RealizedExistenceProvider) {
        if(this.provider is StyleProviderWrapper)
            this.provider.attach(provider)
        else error("Can't attach a saved style")
    }

    override fun detach() {
        if(provider is StyleProviderWrapper)
            provider.detach()
        else error("Can't detach a saved style")
    }

    override fun getValue(key: String) = provider.getStyle(key)

    override fun setValue(key: String, value: String?) {
        if (value != null)
            provider.setValue(key, value)
        else
            provider.removeStyle(key)
    }

    private val capLetter = Regex("([A-Z])")
    override fun transformKey(key: String): String {
        return capLetter.replace(key.removeSuffix("Raw")) { "-" + it.value.toLowerCase() }
    }

    inner class Importance internal constructor() {
        operator fun get(key: String) = provider.isImportant(transformKey(key))
        operator fun set(key: String, important: Boolean) = provider.setImportance(transformKey(key), important)
    }

    val important = Importance()

    operator fun set(key: String, value: Pair<String?, Boolean>) {
        this[key] = value.first
        important[key] = value.second
    }

    fun toCSS(pretty: Boolean = false) = provider.toHTML(pretty)

    operator fun Style.unaryPlus() {
        if (this.provider.save && this.provider is VirtualStyleProvider) {
            this.provider.raw.forEach {
                this@Style[it.key] = it.value
            }
        } else {
            //TODO don't do this?  get keys somehow
            error("Can't use styles from another element.  Create the style independently and use it in both")
        }
    }

    companion object {
        inline operator fun invoke(builder: Style.() -> Unit) = Style()
            .also(builder)
    }

    internal fun loadFrom(other: HasStyleDelegate, defaultPrefix: String? = null) {
        if (other.delegate is PrefixStyleDelegate && other.delegate.hasMap()) {
            other.delegate.getMap().forEach {
                this[(if (other.prefix == null) defaultPrefix?.let { "$it-" } ?: "" else "") + it.key] = it.value
            }
        } else if (other.delegate is MapDelegate) {
            other.delegate.forEach {
                this[(if (other.prefix == null) defaultPrefix?.let { "$it-" } ?: "" else "") + it.key] = it.value
            }
        }
    }

    override fun rectify(source: Style) {
        val thisMap = provider.map()
        val otherMap = source.provider.map()
        otherMap.forEach { (key, value) ->
            if (key !in thisMap || thisMap[key] != value) {

                if (value.first != thisMap[key]?.first)
                    setValue(key, value.first)

                if (value.second != thisMap[key]?.second)
                    important[key] = value.second
            }
        }
        thisMap.keys.forEach {
            if (it !in otherMap)
                this.remove(it)
        }
    }

    inline operator fun invoke(block: Style.() -> Unit) {
        block()
    }

    private val _margin by lazy { Margin(this) }

    @StylesheetDSL
    var margin: Margin
        get() = _margin
        set(v) {
            loadFrom(v)
        }

    private val _padding by lazy { Padding(this) }

    @StylesheetDSL
    var padding: Padding
        get() = _padding
        set(v) {
            loadFrom(v)
        }

    private val _border by lazy { Border(this) }

    @StylesheetDSL
    var border: Border
        get() = _border
        set(v) {
            loadFrom(v)
        }

    private val _outline by lazy { Outline(this) }

    @StylesheetDSL
    var outline: Outline
        get() = _outline
        set(v) {
            loadFrom(v)
        }

    @StylesheetDSL
    var color by byColor("color")

    private val _background by lazy { Background(this) }

    @StylesheetDSL
    var background: Background
        get() = _background
        set(v) {
            loadFrom(v)
        }

    @StylesheetDSL
    var height by bySize("height")

    @StylesheetDSL
    var maxHeight by bySize("max-height")

    @StylesheetDSL
    var minHeight by bySize("min-height")

    @StylesheetDSL
    var width by bySize("width")

    @StylesheetDSL
    var maxWidth by bySize("max-width")

    @StylesheetDSL
    var minWidth by bySize("min-width")

    // Sets both width and height
    @StylesheetDSL
    var size: Size?
        get() = if (width == height) width else null
        set(v) {
            width = v
            height = v
        }

    // Sets both width and height
    @StylesheetDSL
    var maxSize: Size?
        get() = if (maxWidth == maxHeight) maxWidth else null
        set(v) {
            maxWidth = v
            maxHeight = v
        }

    // Sets both width and height
    @StylesheetDSL
    var minSize: Size?
        get() = if (minWidth == minHeight) minWidth else null
        set(v) {
            minWidth = v
            minHeight = v
        }

    @StylesheetDSL
    var boxShadow by byBoxShadow("box-shadow")

    //TODO finish style setters
}
/*
inline fun <reified T : Enum<T>> DelegateInterface.byEnum(ignoreCase: Boolean = true): Helper<T?> = by({
    it?.let { value ->
        enumValues<T>().forEach {
            if (ignoreCase && it.name.toLowerCase() == value.toLowerCase())
                return@let it
            else if (it.name == value)
                return@let it
        }

        return@let null
    }
},
        {
            it?.toString()
        })*/

@Suppress("RemoveExplicitTypeArguments")
inline fun <reified T : Enum<T>> DelegateInterface.byEnum(
    key: String,
    ignoreCase: Boolean = true
): ReadWriteProperty<Any?, T?> = by<T?>(
    key, {
        it?.let { value ->
            enumValues<T>().forEach {
                if (ignoreCase && it.name.toLowerCase() == value.toLowerCase())
                    return@let it
                else if (it.name == value)
                    return@let it
            }

            return@let null
        }
    },
    {
        it?.toString()?.let { if (ignoreCase) it.toLowerCase() else it }
    })