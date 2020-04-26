package com.rnett.kframe.dom.material

import com.rnett.kframe.dom.basics.span
import com.rnett.kframe.dom.core.DisplayElementHost
import com.rnett.kframe.dom.core.KFrameDSL
import com.rnett.kframe.dom.core.TextElement
import com.rnett.kframe.dom.core.element
import com.rnett.kframe.utils.byBoolFlag
import org.w3c.dom.HTMLElement

@JsModule("@material/ripple")
external object Ripple{
    object MDCRipple{
        fun attachTo(element: HTMLElement)
    }
}

class MaterialButton(parent: DisplayElementHost): MaterialElement<MaterialButton>(parent, "button", "mdc-button", Ripple.MDCRipple::attachTo){
    init {
        span("mdc-button__ripple")
    }
    val label by lazy{ span("mdc-button__label") }

    override operator fun String.unaryPlus(): TextElement {
        label.apply{ return +this@unaryPlus }
    }

    var raised by byMaterialFlag
    var unelevated by byMaterialFlag

    var outlined by byMaterialFlag
    var dense by byMaterialFlag
    //TODO icons

    @KFrameDSL
    fun icon(icon: String) = element("i")("material-icons mdc-button__icon"){
        attributes["aria-hidden"] = "true"
        +icon
    }

    var disabled by attributes.byBoolFlag
}

@KFrameDSL
inline val DisplayElementHost.button get() = MaterialButton(this)
