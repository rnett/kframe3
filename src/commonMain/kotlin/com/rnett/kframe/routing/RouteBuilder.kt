package com.rnett.kframe.routing

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

@OptIn(ExperimentalContracts::class)
abstract class RouteBuilder {

    protected abstract fun <T> makeParam(
        name: String,
        location: ParamLocation,
        transform: (String) -> T
    ): RoutePart.Param<T>

    protected abstract fun <T> makeOptionalParam(
        name: String,
        location: ParamLocation,
        transform: (String) -> T
    ): RoutePart.OptionalParam<T>

    protected abstract fun <T> makeAnonymousParam(name: String, transform: (String) -> T): RoutePart.AnonymousParam<T>
    protected abstract fun makeStatic(name: String): RoutePart.Static
//    protected abstract fun makeWildcard(): RoutePart.Wildcard

    // no builder, transform

    @RoutingDSL
    fun <T> param(name: String, location: ParamLocation, transform: (String) -> T): RoutePart.Param<T> =
        makeParam(name, location, transform)

    @RoutingDSL
    fun <T> urlParam(name: String, transform: (String) -> T) = param(
        name,
        ParamLocation.Url, transform
    )

    @RoutingDSL
    fun <T> tailcardParam(name: String, transform: (String) -> T) = param(
        name,
        ParamLocation.Tailcard, transform
    )

    @RoutingDSL
    fun <T> eitherParam(name: String, transform: (String) -> T) = param(
        name,
        ParamLocation.Either, transform
    )

    @RoutingDSL
    fun <T> optionalParam(name: String, location: ParamLocation, transform: (String) -> T) =
        makeOptionalParam(name, location, transform)

    @RoutingDSL
    fun <T> urlOptionalParam(name: String, transform: (String) -> T) = optionalParam(
        name,
        ParamLocation.Url, transform
    )

    @RoutingDSL
    fun <T> tailcardOptionalParam(name: String, transform: (String) -> T) =
        optionalParam(name, ParamLocation.Tailcard, transform)

    @RoutingDSL
    fun <T> eitherOptionalParam(name: String, transform: (String) -> T) =
        optionalParam(name, ParamLocation.Either, transform)

    @RoutingDSL
    fun <T> anonymousParam(name: String, transform: (String) -> T) = makeAnonymousParam(name, transform)

    @RoutingDSL
    fun static(name: String) = makeStatic(name)

    @RoutingDSL
    fun static(vararg names: String): RoutePart.Static {
        var part = static(names.first())
        names.drop(1).forEach {
            part = part.static(it)
        }
        return part
    }

//    @RoutingDSL
//    fun wildcard() = makeWildcard()

    /**
     * Allows statics and wildcards, separated by slashes
     */
    @RoutingDSL
    fun path(path: String): RoutePart {
        val parts = path.split("/")
        var part = parts.first().let {
//            if (it == "*")
//                wildcard()
//            else
            static(it)
        }

        parts.drop(1).forEach {
//            part = if (it == "*")
//                part.wildcard()
//            else
            part.static(it)
        }
        return part
    }

    // no builder, no transform

    @RoutingDSL
    fun stringParam(name: String, location: ParamLocation) = param(name, location, { it })

    @RoutingDSL
    fun urlStringParam(name: String) = stringParam(name, ParamLocation.Url)

    @RoutingDSL
    fun tailcardStringParam(name: String) = stringParam(
        name,
        ParamLocation.Tailcard
    )

    @RoutingDSL
    fun eitherStringParam(name: String) = stringParam(
        name,
        ParamLocation.Either
    )

    @RoutingDSL
    fun optionalStringParam(name: String, location: ParamLocation) = optionalParam(name, location, { it })

    @RoutingDSL
    fun urlOptionalStringParam(name: String) = optionalStringParam(
        name,
        ParamLocation.Url
    )

    @RoutingDSL
    fun tailcardOptionalStringParam(name: String) = optionalStringParam(
        name,
        ParamLocation.Tailcard
    )

    @RoutingDSL
    fun eitherOptionalStringParam(name: String) = optionalStringParam(
        name,
        ParamLocation.Either
    )

    @RoutingDSL
    fun anonymousStringParam(name: String) = anonymousParam(name, { it })


    // builder, transform

    @RoutingDSL
    inline fun <T> param(
        name: String,
        location: ParamLocation,
        noinline transform: (String) -> T,
        builder: RoutePart.Param<T>.(RoutePart.Param<T>) -> Unit
    ): RoutePart.Param<T> {
        contract {
            callsInPlace(builder, InvocationKind.EXACTLY_ONCE)
        }
        return param(name, location, transform).also { it.build(builder) }
    }

    @RoutingDSL
    inline fun <T> urlParam(
        name: String,
        noinline transform: (String) -> T,
        builder: RoutePart.Param<T>.(RoutePart.Param<T>) -> Unit
    ): RoutePart.Param<T> {
        contract {
            callsInPlace(builder, InvocationKind.EXACTLY_ONCE)
        }
        return param(name, ParamLocation.Url, transform, builder)
    }

    @RoutingDSL
    inline fun <T> tailcardParam(
        name: String,
        noinline transform: (String) -> T,
        builder: RoutePart.Param<T>.(RoutePart.Param<T>) -> Unit
    ): RoutePart.Param<T> {

        contract {
            callsInPlace(builder, InvocationKind.EXACTLY_ONCE)
        }
        return param(name, ParamLocation.Tailcard, transform, builder)
    }

    @RoutingDSL
    inline fun <T> eitherParam(
        name: String,
        noinline transform: (String) -> T,
        builder: RoutePart.Param<T>.(RoutePart.Param<T>) -> Unit
    ): RoutePart.Param<T> {
        contract {
            callsInPlace(builder, InvocationKind.EXACTLY_ONCE)
        }
        return param(name, ParamLocation.Either, transform, builder)
    }

    @RoutingDSL
    inline fun <T> optionalParam(
        name: String,
        location: ParamLocation,
        noinline transform: (String) -> T,
        builder: RoutePart.OptionalParam<T>.(RoutePart.OptionalParam<T>) -> Unit
    ): RoutePart.OptionalParam<T> {
        contract {
            callsInPlace(builder, InvocationKind.EXACTLY_ONCE)
        }
        return optionalParam(name, location, transform).also { it.build(builder) }
    }

    @RoutingDSL
    inline fun <T> urlOptionalParam(
        name: String,
        noinline transform: (String) -> T,
        builder: RoutePart.OptionalParam<T>.(RoutePart.OptionalParam<T>) -> Unit
    ): RoutePart.OptionalParam<T> {
        contract {
            callsInPlace(builder, InvocationKind.EXACTLY_ONCE)
        }
        return optionalParam(name, ParamLocation.Url, transform, builder)
    }

    @RoutingDSL
    inline fun <T> tailcardOptionalParam(
        name: String,
        noinline transform: (String) -> T,
        builder: RoutePart.OptionalParam<T>.(RoutePart.OptionalParam<T>) -> Unit
    ): RoutePart.OptionalParam<T> {
        contract {
            callsInPlace(builder, InvocationKind.EXACTLY_ONCE)
        }
        return optionalParam(
            name,
            ParamLocation.Tailcard, transform, builder
        )
    }

    @RoutingDSL
    inline fun <T> eitherOptionalParam(
        name: String,
        noinline transform: (String) -> T,
        builder: RoutePart.OptionalParam<T>.(RoutePart.OptionalParam<T>) -> Unit
    ): RoutePart.OptionalParam<T> {
        contract {
            callsInPlace(builder, InvocationKind.EXACTLY_ONCE)
        }
        return optionalParam(name, ParamLocation.Either, transform, builder)
    }

    @RoutingDSL
    inline fun <T> anonymousParam(
        name: String,
        noinline transform: (String) -> T,
        builder: RoutePart.AnonymousParam<T>.(RoutePart.AnonymousParam<T>) -> Unit
    ): RoutePart.AnonymousParam<T> {
        contract {
            callsInPlace(builder, InvocationKind.EXACTLY_ONCE)
        }
        return anonymousParam(name, transform).also { it.build(builder) }
    }

    // builder, no transform

    @RoutingDSL
    inline fun stringParam(
        name: String,
        location: ParamLocation,
        builder: RoutePart.Param<String>.(RoutePart.Param<String>) -> Unit
    ): RoutePart.Param<String> {
        contract {
            callsInPlace(builder, InvocationKind.EXACTLY_ONCE)
        }
        return stringParam(name, location).also { it.build(builder) }
    }

    @RoutingDSL
    inline fun urlStringParam(
        name: String,
        builder: RoutePart.Param<String>.(RoutePart.Param<String>) -> Unit
    ): RoutePart.Param<String> {
        contract {
            callsInPlace(builder, InvocationKind.EXACTLY_ONCE)
        }
        return stringParam(name, ParamLocation.Url, builder)
    }

    @RoutingDSL
    inline fun tailcardStringParam(
        name: String,
        builder: RoutePart.Param<String>.(RoutePart.Param<String>) -> Unit
    ): RoutePart.Param<String> {
        contract {
            callsInPlace(builder, InvocationKind.EXACTLY_ONCE)
        }
        return stringParam(name, ParamLocation.Tailcard, builder)
    }

    @RoutingDSL
    inline fun eitherStringParam(
        name: String,
        builder: RoutePart.Param<String>.(RoutePart.Param<String>) -> Unit
    ): RoutePart.Param<String> {
        contract {
            callsInPlace(builder, InvocationKind.EXACTLY_ONCE)
        }
        return stringParam(name, ParamLocation.Either, builder)
    }

    @RoutingDSL
    inline fun optionalStringParam(
        name: String,
        location: ParamLocation,
        builder: RoutePart.OptionalParam<String>.(RoutePart.OptionalParam<String>) -> Unit
    ): RoutePart.OptionalParam<String> {
        contract {
            callsInPlace(builder, InvocationKind.EXACTLY_ONCE)
        }
        return optionalStringParam(name, location).also { it.build(builder) }
    }

    @RoutingDSL
    inline fun urlOptionalStringParam(
        name: String,
        builder: RoutePart.OptionalParam<String>.(RoutePart.OptionalParam<String>) -> Unit
    ): RoutePart.OptionalParam<String> {
        contract {
            callsInPlace(builder, InvocationKind.EXACTLY_ONCE)
        }
        return optionalStringParam(name, ParamLocation.Url, builder)
    }

    @RoutingDSL
    inline fun tailcardOptionalStringParam(
        name: String,
        builder: RoutePart.OptionalParam<String>.(RoutePart.OptionalParam<String>) -> Unit
    ): RoutePart.OptionalParam<String> {
        contract {
            callsInPlace(builder, InvocationKind.EXACTLY_ONCE)
        }
        return optionalStringParam(name, ParamLocation.Tailcard, builder)
    }

    @RoutingDSL
    inline fun eitherOptionalStringParam(
        name: String,
        builder: RoutePart.OptionalParam<String>.(RoutePart.OptionalParam<String>) -> Unit
    ): RoutePart.OptionalParam<String> {
        contract {
            callsInPlace(builder, InvocationKind.EXACTLY_ONCE)
        }
        return optionalStringParam(name, ParamLocation.Either, builder)
    }

    @RoutingDSL
    inline fun anonymousStringParam(
        name: String,
        builder: RoutePart.AnonymousParam<String>.(RoutePart.AnonymousParam<String>) -> Unit
    ): RoutePart.AnonymousParam<String> {
        contract {
            callsInPlace(builder, InvocationKind.EXACTLY_ONCE)
        }
        return anonymousStringParam(name).also { it.build(builder) }
    }

    @RoutingDSL
    inline fun static(name: String, builder: RoutePart.() -> Unit): RoutePart.Static {
        contract {
            callsInPlace(builder, InvocationKind.EXACTLY_ONCE)
        }
        return static(name).also { it.builder() }
    }

    @RoutingDSL
    inline fun static(vararg names: String, builder: RoutePart.() -> Unit): RoutePart.Static {
        contract {
            callsInPlace(builder, InvocationKind.EXACTLY_ONCE)
        }

        return static(*names).also { it.builder() }
    }

    //    @RoutingDSL
//    fun wildcard(builder: RoutePart.() -> Unit): RoutePart.Wildcard {
//        contract {
//            callsInPlace(builder, InvocationKind.EXACTLY_ONCE)
//        }
//        return wildcard().also { it.builder() }
//    }
    @RoutingDSL
    inline fun path(path: String, builder: RoutePart.() -> Unit): RoutePart {
        contract {
            callsInPlace(builder, InvocationKind.EXACTLY_ONCE)
        }
        val part = path(path)
        return part.also { it.builder() }
    }

    @RoutingDSL
    abstract operator fun <T> PageDef<T>.invoke(dataBuilder: UrlData.() -> T): Route<T>

    @RoutingDSL
    abstract operator fun <T> PageDef<T>.invoke(toUrl: (T) -> String, dataBuilder: UrlData.() -> T): ReactiveRoute<T>

    @RoutingDSL
    abstract operator fun PageDef<Unit>.invoke(): ReactiveRoute<Unit>

    @RoutingDSL
    abstract operator fun PageDef<Unit>.invoke(url: String): ReactiveRoute<Unit>

    @RoutingDSL
    abstract operator fun <T> JsonPageDef<T>.invoke(renames: Map<String, String> = mapOf()): JsonRoute<T>
}