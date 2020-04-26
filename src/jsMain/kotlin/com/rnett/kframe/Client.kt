package com.rnett.kframe

import com.rnett.kframe.binding.watchBinding
import com.rnett.kframe.dom.basics.div
import com.rnett.kframe.dom.core.click
import com.rnett.kframe.dom.core.document
import com.rnett.kframe.dom.core.element
import com.rnett.kframe.dom.core.style.Color
import com.rnett.kframe.dom.core.style.Padding
import com.rnett.kframe.dom.core.style.px
import com.rnett.kframe.dom.input.intInput
import com.rnett.kframe.dom.material.Ripple
import com.rnett.kframe.routing.goto
import com.rnett.kframe.style.StyleClass
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.seconds

val padded = StyleClass {
    padding = Padding(20.px)
    margin {
        left = 20.px
        top = 20.px
    }
}

data class Tester(val t: Int = 10)

//TODO better way of setting title
fun main() {

    document(Routing) {
        body {

            val t = Ripple

//            element("button")(){ classes += "mdc-button"}.realizedProviderOrNull?.let { MDCRipple(it.underlying) }

            //TODO goto pages not working

            //TODO material components, using https://material.io/develop/web/components/buttons/

            onPages {
                Routing.mainPage {
                    div {
                        +padded
                        style.background.color = Color.Lightblue
                        +"Hello World!"
                    }

                    div {
                        +padded
                        element("button")() {
//                            outlined = true
                            +"Goto Increment"
                            on.click {
                                Routing.incrementRoute.goto(IncrementData(10))
                            }
                        }
                    }
                }

                Routing.incrementPage { incrementWatcher ->
                    val increment by incrementWatcher
                    div {
                        +padded
                        watchBinding(incrementWatcher) {
                            +"Value: ${increment.value}"
                        }
                    }
                    div {
                        +padded
                        +"Step:"
                        //TODO need a way to bind directly to increment.step (probably going to need a compiler plugin)
                        //  for now, pass watcher, property to input element, make property-watcher like thing to update watcher, bind to watcher.onSet to update element
                        intInput(increment.step, { it != 0 }) {
                            backing.onSet {
                                incrementWatcher.update { step = it }
                            }
                        }
                    }

                    div {
                        +padded
                        watchBinding(incrementWatcher) {
                            +"Step: ${increment.step}"
                        }
                    }

                    div {
                        +padded
                        element("button")() {
//                                raised = true
                            +"Go Home"
                            on.click {
                                Routing.mainRoute.goto()
                            }
                        }
                    }

                    launch(Dispatchers.Default) {
                        while (this.isActive) {
                            delay(5.seconds)
                            console.log("Incrementing")
                            incrementWatcher.update { value += step }
                        }
                    }
                }
            }
        }
    }
}
