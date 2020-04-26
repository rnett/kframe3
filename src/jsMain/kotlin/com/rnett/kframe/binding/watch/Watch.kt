package com.rnett.kframe.binding.watch

import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

class WatcherId<T> internal constructor(val id: Int, watch: Watch<T>) {

    internal var watch: Watch<T>? = watch

    fun remove() {
        watch?.removeListener(this)
        watch = null
    }

    fun isLinked() = watch != null

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is WatcherId<*>) return false

        if (id != other.id) return false
        if (watch != other.watch) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id
        result = 31 * result + (watch?.hashCode() ?: 0)
        return result
    }
}

abstract class Watch<T>() : ReadWriteProperty<Any?, T> {
    private val _watchers = mutableMapOf<WatcherId<T>, (T) -> Unit>()

    private var nextWatchId = 0

    internal fun newWatchId() = WatcherId(nextWatchId++, this)

    /**
     * Called after value is set
     */
    fun onSet(watcher: (T) -> Unit) = newWatchId().also { setWatch(it, watcher) }

    internal fun setWatch(id: WatcherId<T>, watcher: (T) -> Unit) {
        check(id.watch === this) { "Can't set a watch for a watch id for another watcher" }
        _watchers[id] = watcher
    }

    fun removeListener(listenerId: WatcherId<T>) {
        if (!listenerId.isLinked())
            error("Can't remove an already-removed listener")

        if (listenerId.watch !== this)
            error("Can't remove a listener of a different watch")

        _watchers.remove(listenerId)
    }

    @PublishedApi
    internal fun notify(newValue: T, except: Set<WatcherId<T>> = setOf()) {
        (_watchers - except).forEach { it.value.invoke(newValue) }
    }

    protected abstract fun doSet(value: T)

    protected abstract fun doGet(): T

    var value: T
        get() = doGet()
        set(value) {
            doSet(value)
            notify(value)
        }

    final override inline operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        this.value = value
    }

    final override inline fun getValue(thisRef: Any?, property: KProperty<*>): T = value

    fun setExcept(newValue: T, except: Set<WatcherId<T>> = setOf()) {
        doSet(newValue)
        notify(newValue, except)
    }

    fun forceUpdate(except: Set<WatcherId<T>> = setOf()) {
        notify(doGet(), except)
    }

    /**
     * Ensure that any bindings using this watch aren't just using equals (as it compares the before-reference and after-reference, which are the same).
     * HashCode should work when using data classes (and is included by default).
     */
    inline fun update(transform: T.() -> Unit) {
        transform(value)
        notify(value)
    }

    inline operator fun invoke(transform: T.() -> Unit) = update(transform)

//
//    /**
//     * Only works when set via the watch, WILL NOT UPDATE IF PROPERTY IS SET DIRECTLY
//     */
//    inner class PropertyWatch<R>(val prop: KMutableProperty1<T, R>): Watch<R>(){
//        override fun doSet(value: R) {
//            prop.set(this@Watch.value, value)
//            this@Watch.notify(this@Watch.value)
//        }
//
//        override fun doGet(): R {
//            return prop.get(this@Watch.value)
//        }
//
//        private val childNotifier = this notifyOn this@Watch
//
//        fun remove(){
//            childNotifier.remove()
//        }
//    }
//
//
//    /**
//     * Only works when set via the watch, WILL NOT UPDATE IF PROPERTY IS SET DIRECTLY.
//     *
//     * May cause memory leaks (child watches won't be removed until the parent is)
//     */
//    fun <R> property(prop: KMutableProperty1<T, R>) = PropertyWatch(prop)
}

//TODO check removal.  Atm, I expect neither to be removed until both are (its a cycle)
/**
 * Sync the values and updates of two watches.  An update to one will result in an update (and listener calls) of the other.
 *
 * Neither watch (or listener) will be removed until both are.
 */
infix fun <T> Watch<T>.sync(other: Watch<T>) {
    val thisWatcherId = this.newWatchId()
    val otherWatcherId = other.newWatchId()

    this.setWatch(thisWatcherId) {
        other.setExcept(it, setOf(otherWatcherId))
    }

    other.setWatch(otherWatcherId) {
        this.setExcept(it, setOf(thisWatcherId))
    }
}

/**
 * Notifies the "parent" watch whenever the "child" updates.
 */
infix fun <T> Watch<*>.notifyOn(child: Watch<T>) =
    child.onSet {
        this.forceUpdate()
    }


