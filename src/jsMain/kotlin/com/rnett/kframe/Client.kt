package com.rnett.kframe

import com.rnett.kframe.binding.watchBinding
import com.rnett.kframe.dom.basics.div
import com.rnett.kframe.dom.core.document
import com.rnett.kframe.dom.core.style.Color
import com.rnett.kframe.dom.core.style.Padding
import com.rnett.kframe.dom.core.style.px
import com.rnett.kframe.dom.input.intInput
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
            onPages {
                Routing.mainPage {
                    div {
                        +padded
                        style.background.color = Color.Lightblue
                        +"Hello World!"
                    }
                }

                Routing.incrementPage { incrementWatcher ->
                    var increment by incrementWatcher
                    div {
                        +padded
                        watchBinding(incrementWatcher) {
                            +"Value: ${increment.value}"
                        }
                    }
                    div {
                        +padded
                        +"Step:"
                        //TODO need a way to bind directly to increment.step (probably going to rely on compiler plugin)
                        intInput(increment.step, { it != 0 }) {
                            backing.onSet {
                                incrementWatcher.update { step = it }
                            }
                        }

                        div {
                            +padded
                            watchBinding(incrementWatcher) {
                                +"Step: ${increment.step}"
                            }
                        }
                    }

                    launch(Dispatchers.Default) {
                        while(this.isActive) {
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
