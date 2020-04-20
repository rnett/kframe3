package com.rnett.kframe.dom.core

import com.rnett.kframe.dom.basics.LinkElement
import com.rnett.kframe.dom.basics.TitleElement
import com.rnett.kframe.dom.basics.link
import com.rnett.kframe.dom.core.style.stylesheet
import com.rnett.kframe.dom.providers.RealizedExistenceProvider
import com.rnett.kframe.routing.Routing
import com.rnett.kframe.style.StyleClassHolder
import org.w3c.dom.HTMLElement
import kotlin.browser.document
import kotlin.browser.window


//TODO need to make work with virtual dom
object Document {
    @KFrameDSL
    val head by lazy{ HeadElement() }
    @KFrameDSL
    val body by lazy{ BodyElement() }

    internal val styleClassHolder by lazy {
        StyleClassHolder(head.stylesheet { }.apply { id = "style-class-holder" })
    }

    private val _elements = mutableMapOf<Int, Element<*>>()
    val elements: Map<Int, Element<*>> get() = _elements

    inline fun <reified E: Element<*>> elementById(id: Int): E {
        val element = elements[id] ?: error("No element with id $id")
        if(element is E)
            return element
        else
            error("Element with id $id is not of type ${E::class}, it was of type ${element::class}")
    }

    internal fun addElement(id: Int, element: Element<*>){
        _elements[id] = element
    }
    internal fun removeElement(id: Int){
        _elements.remove(id)
    }
}

@KFrameDSL
inline fun document(router: Routing? = null, crossinline builder: Document.() -> Unit){
    window.onload = {
        router?.init()
        Document.builder()
    }
}

class HeadElement(): MetaElementHost, Element<HeadElement>(null, RealizedExistenceProvider("head", document.head ?: document.createElement("HEAD") as HTMLElement)){
    private var titleElement: TitleElement? = null
    @KFrameDSL
    var title
        get() = titleElement?.titleText
        set(v) {
            if (v != null) {
                if (titleElement == null)
                    titleElement = TitleElement(this, v)
                else
                    titleElement?.titleText = v
            } else if (titleElement != null) {
                titleElement?.remove()
                titleElement = null
            }
        }

    private var faviconElement: LinkElement? = null

    @KFrameDSL
    var favicon
        get() = faviconElement?.attributes?.get("href")
        set(v) {
            if (v != null) {
                if (faviconElement == null)
                    faviconElement = link(v, rel = "favicon")
                else
                    faviconElement?.href = v
            } else if (faviconElement != null) {
                faviconElement?.remove()
                faviconElement = null
            }
        }

    private var viewportElement: BaseMetaElement? = BaseMetaElement(this, "meta")() {
        attributes["content"] = "width=device-width, initial-scale=1, shrink-to-fit=no"
        attributes["name"] = "viewport"
    }

    @KFrameDSL
    var viewport
        get() = viewportElement?.attributes?.get("content")
        set(v) {
            if (v != null) {
                if (viewportElement == null)
                    viewportElement = BaseMetaElement(this, "meta")() {
                        attributes["content"] = v
                        attributes["name"] = "viewport"
                    }
                else
                    viewportElement?.attributes?.set("content", v)
            } else if (viewportElement != null) {
                viewportElement?.remove()
                viewportElement = null
            }
        }
}
class BodyElement(): DisplayElementHost, Element<BodyElement>(null, RealizedExistenceProvider("body", document.body ?: document.createElement("BODY") as HTMLElement)) {
    override fun elementAncestor(): AnyElement {
        return this
    }
}