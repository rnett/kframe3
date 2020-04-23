package com.rnett.kframe

import com.rnett.kframe.binding.watchBinding
import com.rnett.kframe.dom.basics.div
import com.rnett.kframe.dom.core.click
import com.rnett.kframe.dom.core.document
import com.rnett.kframe.dom.core.style.Color
import com.rnett.kframe.dom.core.style.Padding
import com.rnett.kframe.dom.core.style.px
import com.rnett.kframe.dom.input.button
import com.rnett.kframe.routing.goto
import com.rnett.kframe.style.StyleClass
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.seconds

val padded = StyleClass{
    padding = Padding(20.px)
    margin {
        left = 20.px
        top = 20.px
    }
}

//TODO better way of setting title
fun main() {
    document(Routing) {
        body {
            onPages {
                Routing.mainPage {
                    div {
                        +padded
                        style.background.color = Color.Lightblue
                        +"Hello World!"
                    }
                }

                Routing.incrementPage { incrementWatcher ->
                    var incrementValue by incrementWatcher
                    div {
                        +padded
                        watchBinding(incrementWatcher) {
                            +"Value: ${incrementValue.value}"
                        }
                    }
                    div{
                        style.margin.top = 20.px
                        button{
                            +"Go Home"
                            on.click{
                                Routing.mainRoute.goto(Unit, "/")
                            }
                        }
                    }

                    launch(Dispatchers.Default) {
                        while(this.isActive) {
                            delay(5.seconds)
                            console.log("Incrementing")
                            incrementValue = incrementValue.copy(value = incrementValue.value + incrementValue.step)
                        }
                    }
                }
            }
        }
    }
}
