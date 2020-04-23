package com.rnett.kframe

import com.rnett.kframe.routing.RoutingDefinition
import com.rnett.kframe.routing.pageDef

//TODO limit route creator methods to here (don't clutter page accesses)
//TODO optional param where key is required but value is optional
//TODO would like to be able to make class based pages (with a display method?)

data class IncrementData(val value: Int, val step: Int)
// 7.7 MB when left
object Routing : RoutingDefinition() {
    val mainPage by pageDef<Unit>()
    val incrementPage by pageDef<IncrementData> { "/increment/${it.value}" + if(it.step != 1) "/step/${it.step}" else "" } //TODO should be able to make single use pages that infer reactivity from routes.  Would need a reverse data builder

//    val mainRoute: Route<Unit>
//    val incrementRoute: Route<Int>

    //TODO assigning routes doesn't work.  Kotlin bug?
    init {
        routing {
            mainPage()
            urlParam("increment", { it.toInt() }) { increment ->
                urlOptionalParam("step", { it.toInt() }) { step ->
                    handle {
                        if (+step == 0)
                            error("Can't use 0 step")
                    }
                    incrementPage { IncrementData(+increment, +step ?: 1) }
                }
            }
        }
    }
}