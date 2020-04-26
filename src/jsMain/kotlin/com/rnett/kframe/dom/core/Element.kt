package com.rnett.kframe.dom.core

import com.rnett.kframe.binding.WatchBinding
import com.rnett.kframe.binding.watch.WrapperWatch
import com.rnett.kframe.binding.watchBinding
import com.rnett.kframe.dom.core.providers.*
import com.rnett.kframe.dom.core.style.Style
import com.rnett.kframe.routing.PageDef
import com.rnett.kframe.routing.RouteInstance
import com.rnett.kframe.routing.RoutingDefinition
import com.rnett.kframe.style.StyleClass
import com.rnett.kframe.utils.byInt
import kotlinx.coroutines.CompletableJob
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.w3c.dom.HTMLElement
import org.w3c.dom.Node
import org.w3c.dom.Text
import org.w3c.dom.asList
import kotlin.collections.List
import kotlin.collections.MutableMap
import kotlin.collections.contains
import kotlin.collections.filterIsInstance
import kotlin.collections.forEach
import kotlin.collections.mapNotNull
import kotlin.collections.minusAssign
import kotlin.collections.mutableListOf
import kotlin.collections.mutableMapOf
import kotlin.collections.mutableSetOf
import kotlin.collections.plusAssign
import kotlin.collections.set
import kotlin.collections.toList
import kotlin.collections.toSet
import kotlin.collections.toTypedArray
import kotlin.coroutines.CoroutineContext
import kotlin.math.absoluteValue
import kotlin.random.Random
import org.w3c.dom.Element as W3Element

@DslMarker
@MustBeDocumented
@Retention(AnnotationRetention.BINARY)
annotation class KFrameDSL

interface HasNode {
    val realizedNodeOrNull: Node?
    fun createAndAttach()
}

interface ElementHost : CoroutineScope {
    fun addElement(element: ElementHost)
    fun removeChild(element: ElementHost, removeUnderlying: Boolean = true)
    fun remove()

    operator fun String.unaryPlus() = TextElement(this@ElementHost, this)
    fun existenceProvider(tag: String): ExistenceProvider
    fun textProvider(text: String): TextProvider
}

const val KFRAME_ELEMENT_ID_NAME = "kframe-element-id"
private val usedIds = mutableSetOf<Int>()
internal fun newElementId(): Int {
    var id = Random.nextInt().absoluteValue
    while (id in usedIds)
        id = Random.nextInt().absoluteValue
    return id
}

typealias AnyElement = Element<*>

//TODO implement rectify.  Check on benifits of virtual DOMs
abstract class Element<S : Element<S>> internal constructor(val parent: ElementHost?, provider: ExistenceProvider) :
    ElementHost, ExistenceAttachable, HasNode {

    private val _provider = ExistenceProviderWrapper(provider)
    val provider: ExistenceProvider = _provider

    private var _supervisorJob: CompletableJob? = null
    private val scope: CoroutineScope by lazy {
        CoroutineScope(Dispatchers.Default + (_supervisorJob ?: SupervisorJob().also { _supervisorJob = it }))
    }

    override val coroutineContext: CoroutineContext
        get() = scope.coroutineContext

    override fun existenceProvider(tag: String): ExistenceProvider = provider.existenceProvider(tag)
    override fun textProvider(text: String): TextProvider = provider.textProvider(text)

    init {
        parent?.addElement(this)
    }

    val tag = provider.tag
    internal val eventProvider = EventProviderWrapper(provider.eventProvider())

    private val _children = mutableListOf<ElementHost>()

    val children: List<ElementHost> get() = _children

    override fun addElement(element: ElementHost) {
        if (element is HasNode)
            element.realizedNodeOrNull?.let(provider::addChild)
        _children += element
    }

    override fun removeChild(element: ElementHost, removeUnderlying: Boolean) {
        _children -= element
        if (element is HasNode && removeUnderlying)
            element.realizedNodeOrNull?.let(provider::removeChild)
    }

    private fun insertChildNode(before: Node, element: ElementHost) {
        if (element is HasNode) {
            val node = element.realizedNodeOrNull
            if (node != null) {
                provider.insertChild(before, node)
            }
        }
    }

    val isRealized: Boolean get() = this._provider.isRealized
    internal val realizedProviderOrNull get() = _provider.realizedProviderOrNull
    override val realizedNodeOrNull: Node?
        get() = realizedProviderOrNull?.underlying

    val properties = Properties(this)

    @Deprecated("You probably want to use properties", replaceWith = ReplaceWith("properties"))
    val attributes = Attributes(this)

    val data by lazy { attributes.data }

    val style = Style(this)

    var id by properties

    val classes = Classes(this)

    val on by lazy { Events(this) }

    inline operator fun invoke(klass: String? = null, id: String? = null, block: S.() -> Unit = {}): S {
        if (id != null)
            this.id = id
        if (klass != null)
            this.classes.add(*klass.split(" ").toTypedArray())
        return (this as S).also(block)
    }

    operator fun StyleClass.unaryPlus() {
        classes += Document.styleClassHolder.addStyleClass(this)
    }

    private var _elementId by properties.byInt(KFRAME_ELEMENT_ID_NAME)
    val elementId
        get() = _elementId
            ?: error("Element ID got deleted somehow in the DOM (attribute $KFRAME_ELEMENT_ID_NAME).  This is a bug.")

    init {
        _elementId = newElementId()
        Document.addElement(elementId, this)
    }

    internal fun changeId(newId: Int) {
        Document.removeElement(elementId)
        _elementId = newId
        Document.addElement(elementId, this)
    }

    var isRemoved = false
        private set

    override fun remove() {
        children.toList().forEach { it.remove() }
        parent?.removeChild(this)
        provider.remove()
        _supervisorJob?.cancel()
        isRemoved = true
        Document.removeElement(elementId)
    }

    open fun initUnderlying(provider: RealizedExistenceProvider){

    }

    final override fun attach(provider: RealizedExistenceProvider) {
        _provider.attach(provider)
        properties.attach(provider)
        classes.attach(provider)
        style.attach(provider)
        eventProvider.attach(provider)

        var idx = 0

        //TODO some sort of edit distance algorithm.  needs delete/add cost, use but add children cost, and use but only edit attributes cost (the lowest)
        children.forEach {
            val underlyingChildren = provider.underlying.childNodes.asList()
            val child = it
            if (idx < underlyingChildren.size) {
                val elementAt = underlyingChildren[idx]



                if (child is Element<*> && elementAt is HTMLElement && elementAt.tagName.toLowerCase() == child.tag) {
                    child.attach(RealizedExistenceProvider(child.tag, elementAt))
                } else if (elementAt is Text && child is TextElement) {
                    child.attach(RealizedTextProvider(elementAt))
                } else {
                    if (child is HasNode) {

                        // make a new node, then insert
                        child.createAndAttach()

                        insertChildNode(elementAt, child)
                    } else
                        return@forEach
                }
                idx++
            } else {
                if (child is HasNode) {
                    child.createAndAttach()
                    child.realizedNodeOrNull?.let(this.provider::addChild)
                    idx++
                }
            }
        }

        val childNodes = children.filterIsInstance<HasNode>().mapNotNull { it.realizedNodeOrNull }.toSet()
        val oldChildNodes = provider.underlying.childNodes.asList().toList()

        oldChildNodes.forEach {
            if(it !in childNodes){
                provider.underlying.removeChild(it)
            }
        }

        initUnderlying(provider)
    }

    override fun createAndAttach() {
        attach(RealizedExistenceProvider(this.tag))
    }

    final override fun detach() {
        _provider.detach()
        properties.detach()
        classes.detach()
        style.detach()
        eventProvider.detach()

        children.forEach { if (it is Attachable<*>) it.detach() }
    }

    init {
        realizedProviderOrNull?.let { initUnderlying(it) }
    }

}

interface MetaElementHost : ElementHost
interface DisplayElementHost : ElementHost {
    fun elementAncestor(): AnyElement

    @KFrameDSL
    operator fun <T> PageDef<T>.invoke(display: WatchBinding<RouteInstance<*>>.(WrapperWatch<T>) -> Unit): WatchBinding<RouteInstance<*>> =
        watchBinding(this.routing.currentPageWatcher, filter = { old, new ->
            old == this || new == this
        }) { page ->
            if (page.route.page == this@invoke) {
                page as RouteInstance<T>
                display(page.dataWatcher)
            }
        }

    @KFrameDSL
    fun <T> onPage(pageDef: PageDef<T>, display: WatchBinding<RouteInstance<*>>.(WrapperWatch<T>) -> Unit) =
        pageDef(display)

    @KFrameDSL
    fun onPages(displays: PagedBinding.() -> Unit): WatchBinding<RouteInstance<*>> {
        val bindings = PagedBinding().also(displays)
        val routing = bindings.routing ?: error("Must bind at least one page")
        return watchBinding(
            routing.currentPageWatcher,
            { old, new -> old.route.page in bindings || new.route.page in bindings }) {
            it as RouteInstance<Any>
            val display = bindings[it.route.page]
            display?.invoke(this, it.dataWatcher)
        }
    }
}

typealias PageBuilder<T> = WatchBinding<RouteInstance<*>>.(WrapperWatch<T>) -> Unit

class PagedBinding {
    private val displayers: MutableMap<PageDef<*>, PageBuilder<Any>> = mutableMapOf()

    internal var routing: RoutingDefinition? = null
        private set

    @KFrameDSL
    operator fun <T> PageDef<T>.invoke(display: PageBuilder<T>) {
        if (this in displayers)
            error("Display already set for page $this")

        if (this@PagedBinding.routing == null)
            this@PagedBinding.routing = this.routing
        else if (this@PagedBinding.routing != this.routing)
            error("Can't bind PageDefs from different Routings")

        displayers[this] = display as PageBuilder<Any>
    }

    operator fun <T> contains(page: PageDef<T>) = page in displayers

    operator fun <T> get(page: PageDef<T>): (WatchBinding<RouteInstance<*>>.(WrapperWatch<T>) -> Unit)? {
        return displayers[page] as PageBuilder<T>?
    }

}

inline fun <reified E : Element<*>> W3Element.asKFrameElement(): E {
    val idStr = getAttribute(KFRAME_ELEMENT_ID_NAME)
        ?: error("Element ID not found (attribute $KFRAME_ELEMENT_ID_NAME).")
    val id = idStr.toIntOrNull()
        ?: error("Element ID was not an int (attribute $KFRAME_ELEMENT_ID_NAME).")
    return Document.elementById(id)
}

inline fun <reified E : Element<*>> W3Element.asKFrameElementOrNull(): E? {
    val idStr = getAttribute(KFRAME_ELEMENT_ID_NAME) ?: return null
    val id = idStr.toIntOrNull()
        ?: error("Element ID was present, but not an int (attribute $KFRAME_ELEMENT_ID_NAME).")

    return if (id in Document.elements)
        Document.elementById(id)
    else null
}


class TextElement internal constructor(val parent: ElementHost, text: String) :
        ElementHost, HasNode, Attachable<RealizedTextProvider> {
    private val provider = TextProviderWrapper(parent.textProvider(text))

    private var _supervisorJob: CompletableJob? = null
    private val scope: CoroutineScope by lazy{
        CoroutineScope(Dispatchers.Default + (_supervisorJob ?: SupervisorJob().also { _supervisorJob = it }))
    }

    override val coroutineContext: CoroutineContext
        get() = scope.coroutineContext

    init {
        parent.addElement(this)
    }

    var text: String
        get() = provider.text
        set(value) {
            provider.text = value
        }

    override fun addElement(element: ElementHost) {
        error("Can't add children to a text element")
    }

    override fun removeChild(element: ElementHost, removeUnderlying: Boolean) {
        error("Text elements have no children")
    }

    override fun remove() {
        provider.remove()
        parent.removeChild(this)
        _supervisorJob?.cancel()
    }

    override fun existenceProvider(tag: String): ExistenceProvider {
        error("Can't create an existence provider from a text element")
    }

    override fun textProvider(text: String): TextProvider {
        error("Can't create a text provider from a text element")
    }

    val isRealized: Boolean get() = provider.isRealized
    internal val realizedProviderOrNull get() = provider.realizedProviderOrNull
    override val realizedNodeOrNull: Node?
        get() = realizedProviderOrNull?.underlying

    override fun attach(provider: RealizedTextProvider) {
        this.provider.attach(provider)
    }

    override fun detach() {
        provider.detach()
    }

    override fun createAndAttach() {
        attach(RealizedTextProvider(text))
    }
}