package com.rnett.kframe.binding

import com.rnett.kframe.dom.core.DisplayElementHost
import com.rnett.kframe.dom.core.KFrameDSL

class WatchBinding<T>(
    parent: DisplayElementHost,
    val watch: BaseWatch<T>,
    filter: Filter<T>,
    display: WatchBinding<T>.(T) -> Unit,
    detectors: List<ChangeDetector<*>>
) : BaseBinding<WatchBinding<T>, T>(parent, display, watch.getValue()) {

    private val changeDetector = ChangeWatcher(watch.getValue(), detectors, filter)

    override fun getValue(): T = watch.getValue()

    fun resetIfNeeded(value: T = getValue()) {
        if (changeDetector.update(value))
            reset()
    }

    private val watcher = watch.onSet { resetIfNeeded(it) }

    override fun remove() {
        watch.removeListener(watcher)
        super.remove()
    }
}

@KFrameDSL
inline fun <T> DisplayElementHost.watchBinding(
    watch: BaseWatch<T>,
    noinline filter: Filter<T> = {_, _, -> true},
    changeDetectors: List<ChangeDetector<*>> = listOf(ChangeDetector.Equals),
    noinline display: WatchBinding<T>.(T) -> Unit
) = WatchBinding(this, watch, filter, display, changeDetectors)