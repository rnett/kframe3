package com.rnett.kframe.binding

import com.rnett.kframe.dom.core.AnyElement
import com.rnett.kframe.dom.core.DisplayElementHost
import com.rnett.kframe.dom.core.ElementHost
import com.rnett.kframe.dom.providers.ExistenceProvider
import com.rnett.kframe.dom.providers.TextProvider
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext

abstract class BaseBinding<S: BaseBinding<S, T>, T>(val parent: DisplayElementHost, private val display: S.(T) -> Unit, initial: T): DisplayElementHost {

    private val children = mutableListOf<ElementHost>()

    private var _supervisorJob: CompletableJob? = null
    private val scope: CoroutineScope by lazy{
        CoroutineScope(Dispatchers.Default + (_supervisorJob ?: SupervisorJob().also { _supervisorJob = it }))
    }

    override val coroutineContext: CoroutineContext
        get() = scope.coroutineContext

    init {
        parent.addElement(this)
    }

    abstract fun getValue(): T

    override fun addElement(element: ElementHost) {
        children.add(element)
        parent.addElement(element)
    }

    override fun removeChild(element: ElementHost) {
        children.remove(element)
        parent.removeChild(element)
    }

    override fun remove() {
        children.forEach { it.remove() }
        parent.removeChild(this)
        _supervisorJob?.cancel()
    }

    open fun reset(value: T = getValue()){
        val elementAncestor = parent.elementAncestor()
        val originalProvider = elementAncestor.realizedProviderOrNull
        if(originalProvider != null)
            elementAncestor.detach()
        children.forEach { it.remove() }
        children.clear()
        _supervisorJob?.cancelChildren()
        display(this as S, value)

        if(originalProvider != null)
            elementAncestor.attach(originalProvider)
    }

    init {
        display(this as S, initial)
    }

    override fun existenceProvider(tag: String): ExistenceProvider = parent.existenceProvider(tag)
    override fun textProvider(text: String): TextProvider = parent.textProvider(text)

    override fun elementAncestor(): AnyElement {
        return parent.elementAncestor()
    }
}
