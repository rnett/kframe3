package com.rnett.kframe.dom.core

import org.w3c.dom.events.EventTarget
import org.w3c.dom.events.FocusEvent
import org.w3c.dom.events.MouseEvent
import org.w3c.dom.events.Event as EventData

class EventHandlerKey<E:  EventData> internal constructor(event: Event<E>, val handler: (E) -> Unit, val underlyingHandler: (EventData) -> Unit, val useCapture: Boolean) {
    val eventType = event.eventType
    internal var event: Event<E>? = event

    fun isActive() = event != null

    fun remove() {
        event?.removeHandler(this)
        event = null
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class.js != other::class.js) return false

        other as EventHandlerKey<*>

        if (handler != other.handler) return false
        if (underlyingHandler != other.underlyingHandler) return false
        if (useCapture != other.useCapture) return false
        if (eventType != other.eventType) return false
        if (event != other.event) return false

        return true
    }

    override fun hashCode(): Int {
        var result = handler.hashCode()
        result = 31 * result + underlyingHandler.hashCode()
        result = 31 * result + useCapture.hashCode()
        result = 31 * result + eventType.hashCode()
        result = 31 * result + (event?.hashCode() ?: 0)
        return result
    }
}

/**
 * @param name should NOT start with on.  Should be from https://developer.mozilla.org/en-US/docs/Web/Events
 */
data class Event<E: EventData> internal constructor(val eventType: String, val element: Element<*>) {
    fun addHandler(
        useCapture: Boolean = false,
        handler: (E) -> Unit
    ): EventHandlerKey<E> {
        val handle: (EventData) -> Unit = { handler(it as E) }
        element.eventProvider.addEventHandler(eventType, handle, useCapture)
        return EventHandlerKey(this, handler, handle, useCapture)
    }

    fun removeHandler(listener: EventHandlerKey<E>) {
        if (!listener.isActive())
            error("Can't remove an already-removed listener")

        if (listener.event !== this)
            error("Can't remove a listener for a different event/element")

        element.eventProvider.removeEventHandler(listener.eventType, listener.underlyingHandler, listener.useCapture)
    }

    operator fun invoke(useCapture: Boolean = false, handler: (E) -> Unit) = addHandler(useCapture, handler)
}

inline fun <reified E: Element<*>> EventTarget.asKFrameElement(): E? =
    if(this is org.w3c.dom.Element)
        this.asKFrameElementOrNull()
    else
        null

inline fun <reified E: Element<*>> EventData.kframeTarget() = target?.asKFrameElement<E>()
inline fun <reified E: Element<*>> EventData.kframeCurrentTarget() = currentTarget?.asKFrameElement<E>()
inline fun <reified E: Element<*>> MouseEvent.kframeRelatedTarget() = relatedTarget?.asKFrameElement<E>()
inline fun <reified E: Element<*>> FocusEvent.kframeRelatedTarget() = relatedTarget?.asKFrameElement<E>()
