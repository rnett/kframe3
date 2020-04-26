package com.rnett.kframe

import com.rnett.kframe.routing.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

//TODO optional param where key is required but value is optional

//TODO check serial compatibility when routes are created (including optionals, etc)

@Serializable
data class IncrementData(@SerialName("increment") var value: Int, var step: Int = 1)


object Routing : RoutingDefinition() {
    val mainPage = unitPageDef()
    val incrementPage = jsonPageDef(IncrementData.serializer())

    val mainRoute: ReactiveRoute<Unit>
    val incrementRoute: JsonRoute<IncrementData>
    val incrementRoute2: ReactiveRoute<IncrementData>

    //TODO assigning routes doesn't work.  Kotlin bug?
    init {
        routing {
            mainRoute = mainPage()

            urlParam("increment", { it.toInt() }) { increment ->
                urlOptionalParam("step", { it.toInt() }) { step ->
                    handle {
                        if (+step == 0)
                            error("Can't use 0 step")
                    }
                    incrementRoute = incrementPage()
                }
            }

            urlParam("increment2", { it.toInt() }) { increment ->
                urlOptionalParam("step", { it.toInt() }) { step ->
                    handle {
                        if (+step == 0)
                            error("Can't use 0 step")
                    }
                    incrementRoute2 = incrementPage { IncrementData(+increment, +step ?: 1) }
                        .asReactive { "/increment2/${it.value}" + if (it.step != 1) "/step/${it.step}" else "" }
                }
            }
        }
    }
}