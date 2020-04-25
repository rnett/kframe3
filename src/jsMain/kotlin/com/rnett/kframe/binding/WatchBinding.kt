package com.rnett.kframe.binding

import com.rnett.kframe.binding.watch.Watch
import com.rnett.kframe.dom.core.DisplayElementHost
import com.rnett.kframe.dom.core.KFrameDSL

class WatchBinding<T>(
    parent: DisplayElementHost,
    val watch: Watch<T>,
    filter: Filter<T>,
    display: WatchBinding<T>.(T) -> Unit,
    detectors: List<ChangeDetector<*>>
) : BaseBinding<WatchBinding<T>, T>(parent, display, watch.value) {

    private val changeDetector = ChangeWatcher(watch.value, detectors, filter)

    override fun getValue(): T = watch.value

    fun resetIfNeeded(value: T = getValue()) {
        if (changeDetector.update(value))
            reset(value)
    }

    private val watcher = watch.onSet { resetIfNeeded(it) }

    override fun remove() {
        watcher.remove()
        super.remove()
    }
}

@KFrameDSL
inline fun <T> DisplayElementHost.watchBinding(
    watch: Watch<T>,
    noinline filter: Filter<T> = { _, _ -> true },
    changeDetectors: List<ChangeDetector<*>> = listOf(ChangeDetector.Equals, ChangeDetector.HashCode),
    noinline display: WatchBinding<T>.(T) -> Unit
) = WatchBinding(this, watch, filter, display, changeDetectors)