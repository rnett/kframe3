package com.rnett.kframe.binding

import kotlin.properties.ReadWriteProperty
import kotlin.random.Random
import kotlin.reflect.KProperty

interface ListenerHost<T, I : ListenerId> : ReadWriteProperty<Any?, T> {

    /**
     * Called after value is set
     */
    fun listen(watcher: (T) -> Unit): I

    fun getValue(): T
    fun setValue(value: T)
    fun removeListener(listenerId: I)
    fun update(transform: (T) -> T)
}

interface ListenerId {
    fun remove()
    fun isLinked(): Boolean
}

class WatcherId internal constructor(val id: Int, watch: ListenerHost<*, WatcherId>) : ListenerId {

    internal var watch: ListenerHost<*, WatcherId>? = watch

    override fun hashCode() = id.hashCode()

    override fun remove() {
        watch?.removeListener(this)
        watch = null
    }

    override fun isLinked() = watch != null

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as WatcherId

        if (id != other.id) return false

        return true
    }
}

//TODO store objects w/ overloaded methods w/ lambdas like Binding?
abstract class BaseWatch<T>() : ListenerHost<T, WatcherId> {
    private val _watchers = mutableMapOf<WatcherId, (T) -> Unit>()
    private val watchers: Map<WatcherId, (T) -> Unit> = _watchers


    /**
     * Called after value is set
     */
    override fun listen(watcher: (T) -> Unit) = onSet(watcher)

    /**
     * Called after value is set
     */
    fun onSet(watcher: (T) -> Unit) = WatcherId(Random.nextInt(), this).also { _watchers[it] = watcher }

    override fun removeListener(listenerId: WatcherId) {
        if (!listenerId.isLinked())
            error("Can't remove an already-removed listener")

        if (listenerId.watch !== this)
            error("Can't remove a listener of a different watch")

        _watchers.remove(listenerId)
    }

    @PublishedApi
    internal fun notify(newValue: T) = watchers.forEach { it.value(newValue) }

    final override inline fun update(transform: (T) -> T) {
        setValue(transform(getValue()))
    }
}

class DelegateWatch<T>(val delegate: ReadWriteProperty<Any?, T>) : BaseWatch<T>() {

    private var _storedProperty: KProperty<*>? = null
    @PublishedApi
    internal val storedProperty
        get() = _storedProperty ?: error("Must initialize DelegateWatch with a property by delegating from it")

    operator fun provideDelegate(thisRef: Any?, property: KProperty<*>): DelegateWatch<T> {
        _storedProperty = property
        return this
    }

    override inline operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
        return delegate.getValue(thisRef, storedProperty)
    }

    override fun getValue() = getValue(null, storedProperty)

    override inline operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        delegate.setValue(thisRef, storedProperty, value)
        notify(value)
    }

    override fun setValue(value: T) = setValue(null, storedProperty, value)
}

class WrapperWatch<T>(initialValue: T) : BaseWatch<T>() {
    var value = initialValue
        get
        private set

    override fun getValue(): T = value

    override inline fun getValue(thisRef: Any?, property: KProperty<*>): T = value

    override fun setValue(value: T) {
        this.value = value
        notify(value)
    }

    override inline fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        setValue(value)
    }
}

class LateinitWrapperWatch<T>() : BaseWatch<T>() {
    private var _value: T? = null

    val value get() = _value ?: error("Must initialize before use")

    override fun getValue(): T = value

    override inline fun getValue(thisRef: Any?, property: KProperty<*>): T = value

    override fun setValue(value: T) {
        this._value = value
        notify(value)
    }

    override inline fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        setValue(value)
    }
}

fun <T> watch(delegate: ReadWriteProperty<Any?, T>) = DelegateWatch(delegate)

fun <T> watchWrapper(value: T) = WrapperWatch(value)

fun <T> watchLateinitWrapper() = LateinitWrapperWatch<T>()
