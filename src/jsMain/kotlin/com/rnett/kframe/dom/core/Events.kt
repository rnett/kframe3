package com.rnett.kframe.dom.core

import org.w3c.dom.DragEvent
import org.w3c.dom.clipboard.ClipboardEvent
import org.w3c.dom.events.InputEvent
import org.w3c.dom.events.KeyboardEvent
import org.w3c.dom.events.MouseEvent
import org.w3c.dom.events.WheelEvent
import org.w3c.dom.events.Event as EventData

class Events internal constructor(private val element: Element<*>) {
    operator fun <E: EventData> invoke(eventType: String) = Event<E>(eventType, element)
    operator fun <E: EventData> invoke(eventType: String, useCapture: Boolean = false, handler: (E) -> Unit) =
        invoke<E>(eventType).invoke(useCapture, handler)

    operator fun invoke(block: Events.() -> Unit) {
        block()
    }

    operator fun String.invoke(useCapture: Boolean = false, handler: (EventData) -> Unit) =
        invoke(this, useCapture, handler)
}


//TODO add more event vals
val Events.change get() = this<EventData>("change")

val Events.input get() = this<InputEvent>("input")

// Mouse events

val Events.click get() = this<MouseEvent>("click")

val Events.dblclick get() = this<MouseEvent>("dblclick")

val Events.select get() = this<EventData>("select")

val Events.mouseenter get() = this<MouseEvent>("mouseenter")

val Events.mouseleave get() = this<MouseEvent>("mouseleave")

val Events.scroll get() = this<EventData>("scroll")

val Events.wheel get() = this<WheelEvent>("wheel")

// Keyboard events

val Events.keydown get() = this<KeyboardEvent>("keydown")

@Deprecated("https://developer.mozilla.org/en-US/docs/Web/Events/keypress", replaceWith = ReplaceWith("keydown", "com.rnett.kframe.dom.core.EventsKt.getKeydown"))
val Events.keypress get() = this<KeyboardEvent>("keypress")

val Events.keyup get() = this<KeyboardEvent>("keyup")

// Clipboard events

val Events.cut get() = this<ClipboardEvent>("cut")
val Events.copy get() = this<ClipboardEvent>("copy")
val Events.paste get() = this<ClipboardEvent>("paste")

// Drag & Drop events
val Events.drag get() = this<DragEvent>("drag")
val Events.dragend get() = this<DragEvent>("dragend")
val Events.dragenter get() = this<DragEvent>("dragenter")
val Events.dragstart get() = this<DragEvent>("dragstart")
val Events.dragleave get() = this<DragEvent>("dragleave")
val Events.dragover get() = this<DragEvent>("dragover")
val Events.drop get() = this<DragEvent>("drop")




