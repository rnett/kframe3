package com.rnett.kframe.dom.core.providers

import org.w3c.dom.events.Event

interface EventProvider {
    fun addEventHandler(eventType: String, handler: (Event) -> Unit, useCapture: Boolean)
    fun removeEventHandler(eventType: String, handler: (Event) -> Unit, useCapture: Boolean)
    fun currentHandlers(): Set<EventHandler>
}

data class EventHandler(val eventType: String, val handler: (Event) -> Unit, val useCapture: Boolean)

internal class VirtualEventProvider : EventProvider, SingleProvider<EventProvider>, VirtualProvider {
    val handlers = mutableSetOf<EventHandler>()

    override fun addEventHandler(eventType: String, handler: (Event) -> Unit, useCapture: Boolean) {
        handlers.add(EventHandler(eventType, handler, useCapture))
    }

    override fun removeEventHandler(eventType: String, handler: (Event) -> Unit, useCapture: Boolean) {
        handlers.remove(EventHandler(eventType, handler, useCapture))
    }

    override fun currentHandlers(): Set<EventHandler> = handlers

    override fun copyFrom(other: EventProvider) {
        handlers.clear()
        handlers.addAll(other.currentHandlers())
    }
}

internal class RealizedEventProvider(private val existenceProvider: RealizedExistenceProvider) : EventProvider, SingleProvider<EventProvider>,
    RealizedProvider {
    private inline val underlying get() = existenceProvider.underlying
    val handlers = mutableSetOf<EventHandler>()

    override fun addEventHandler(eventType: String, handler: (Event) -> Unit, useCapture: Boolean) {
        handlers.add(EventHandler(eventType, handler, useCapture))
        underlying.addEventListener(eventType, handler, useCapture)
    }

    override fun removeEventHandler(eventType: String, handler: (Event) -> Unit, useCapture: Boolean) {
        handlers.remove(EventHandler(eventType, handler, useCapture))
        underlying.removeEventListener(eventType, handler, useCapture)
    }

    override fun currentHandlers(): Set<EventHandler> = handlers

    override fun copyFrom(other: EventProvider) {
        handlers.forEach {
            //TODO can I ensure that handler replacement works properly?  If so I don't need to clear
            removeEventHandler(it.eventType, it.handler, it.useCapture)
        }

        handlers.clear()
        handlers.addAll(other.currentHandlers())

        handlers.forEach {
            addEventHandler(it.eventType, it.handler, it.useCapture)
        }
    }
}

internal class EventProviderWrapper(initial: EventProvider): EventProvider, ExistenceAttachable {
    private var provider: EventProvider = initial

    override fun attach(provider: RealizedExistenceProvider) {
        this.provider = RealizedEventProvider(provider).also { it.copyFrom(this.provider) }
    }

    override fun detach() {
        provider = VirtualEventProvider().also { it.copyFrom(provider) }
    }

    override fun addEventHandler(eventType: String, handler: (Event) -> Unit, useCapture: Boolean) = provider.addEventHandler(eventType, handler, useCapture)

    override fun removeEventHandler(eventType: String, handler: (Event) -> Unit, useCapture: Boolean) = provider.removeEventHandler(eventType, handler, useCapture)

    override fun currentHandlers(): Set<EventHandler> = provider.currentHandlers()
}