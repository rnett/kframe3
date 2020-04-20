package com.rnett.kframe.binding

import com.rnett.kframe.dom.core.DisplayElementHost
import com.rnett.kframe.dom.core.Event
import com.rnett.kframe.dom.core.EventHandlerKey
import com.rnett.kframe.dom.core.KFrameDSL
import kotlin.reflect.KProperty0
import kotlin.time.Duration
import kotlin.time.TimeMark
import kotlin.time.TimeSource
import kotlin.time.milliseconds

interface DataSource<T> {
    val value: T
}

data class LambdaDataSource<T>(val getter: () -> T) : DataSource<T> {
    override val value get() = getter()
}

data class PropertyDataSource<T>(val property: KProperty0<T>) : DataSource<T> {
    override val value get() = property.get()
}

class EventBinding<T>(
    parent: DisplayElementHost,
    val dataSource: DataSource<T>,
    filter: Filter<T>,
    display: EventBinding<T>.(T) -> Unit,
    events: Set<Event<*>>,
    val lockoutTime: Duration,
    detectors: List<ChangeDetector<*>>
) : BaseBinding<EventBinding<T>, T>(parent, display, dataSource.value) {

    private val changeDetector = ChangeWatcher(dataSource.value, detectors, filter)

    override fun getValue(): T = dataSource.value

    fun resetIfNeeded() {
        if (changeDetector.update(getValue()))
            reset()
    }

    private var lastUpdate: TimeMark = TimeSource.Monotonic.markNow()

    private fun eventUpdate() {
        if ((lastUpdate + lockoutTime).hasPassedNow()) {
            resetIfNeeded()
            lastUpdate = TimeSource.Monotonic.markNow()
        }
    }

    private val events = Events(events.associateWith { it.addHandler { eventUpdate() } }.toMutableMap())

    inner class Events internal constructor(private val handlers: MutableMap<Event<*>, EventHandlerKey<*>>) :
        Set<Event<*>> by handlers.keys {

        fun add(event: Event<*>) {
            if (event in handlers)
                return
            handlers[event] = event.addHandler { eventUpdate() }
        }

        operator fun plusAssign(event: Event<*>) = add(event)

        fun remove(event: Event<*>) {
            if (event !in events)
                return

            val handler = handlers[event]
            handler?.remove()
            handlers.remove(event)
        }

        operator fun minusAssign(event: Event<*>) = remove(event)

        internal fun removeOld() {
            handlers.keys.filter { it.element.isRemoved }.forEach { handlers.remove(it) }
        }

        internal fun removeHandlers() {
            handlers.forEach { it.value.remove() }
        }
    }

    override fun remove() {
        events.removeHandlers()
        super.remove()
    }

    override fun reset(value: T) {
        super.reset(value)
        events.removeOld()
    }
}

@KFrameDSL
inline fun <T> DisplayElementHost.eventBinding(
    dataSource: DataSource<T>,
    noinline filter: Filter<T> = {_, _, -> true},
    changeDetectors: List<ChangeDetector<*>> = listOf(ChangeDetector.Equals),
    lockoutTime: Duration = 5.milliseconds,
    events: Set<Event<*>> = setOf(),
    noinline display: EventBinding<T>.(T) -> Unit
) = EventBinding(this, dataSource, filter, display, events, lockoutTime, changeDetectors)

@KFrameDSL
inline fun <T> DisplayElementHost.eventBinding(
    noinline dataSource: () -> T,
    noinline filter: Filter<T> = {_, _, -> true},
    changeDetectors: List<ChangeDetector<*>> = listOf(ChangeDetector.Equals),
    lockoutTime: Duration = 5.milliseconds,
    events: Set<Event<*>> = setOf(),
    noinline display: EventBinding<T>.(T) -> Unit
) = EventBinding(
    this,
    LambdaDataSource(dataSource),
    filter,
    display,
    events,
    lockoutTime,
    changeDetectors
)

@KFrameDSL
inline fun <T> DisplayElementHost.eventBinding(
    dataSource: KProperty0<T>,
    noinline filter: Filter<T> = {_, _, -> true},
    changeDetectors: List<ChangeDetector<*>> = listOf(ChangeDetector.Equals),
    lockoutTime: Duration = 5.milliseconds,
    events: Set<Event<*>> = setOf(),
    noinline display: EventBinding<T>.(T) -> Unit
) = EventBinding(
    this,
    PropertyDataSource(dataSource),
    filter,
    display,
    events,
    lockoutTime,
    changeDetectors
)