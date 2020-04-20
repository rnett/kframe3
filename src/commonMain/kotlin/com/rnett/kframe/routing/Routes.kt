package com.rnett.kframe.routing

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

@DslMarker
@MustBeDocumented
@Retention(AnnotationRetention.BINARY)
annotation class RoutingDSL

class DataBuilder(val urlParams: Map<String, Any?>, val tailcardParams: Map<String, Any?>) {

    operator fun <T> RoutePart.Param<T>.unaryPlus(): T = when (location) {
        ParamLocation.Url -> urlParams[name] ?: error("Required url parameter not found: $name")
        ParamLocation.Tailcard -> tailcardParams[name]
            ?: error("Required tailcard parameter not found: $name")
        ParamLocation.Either -> urlParams[name] ?: tailcardParams[name]
        ?: error("Required url/tailcard parameter not found: $name")
    } as T

    operator fun <T> RoutePart.OptionalParam<T>.unaryPlus(): T? = when (location) {
        ParamLocation.Url -> urlParams[name]
        ParamLocation.Tailcard -> tailcardParams[name]
        ParamLocation.Either -> urlParams[name] ?: tailcardParams[name]
    } as T?

    operator fun <T> RoutePart.AnonymousParam<T>.unaryPlus(): T =
        (urlParams[name] ?: error("Required anonymous url parameter not found: $name")) as T

    fun <T> RoutePart.Param<T>.resolve() = +this
    fun <T> RoutePart.OptionalParam<T>.resolve() = +this
    fun <T> RoutePart.AnonymousParam<T>.resolve() = +this
}

data class PartedURL(val parts: List<String>, val tailcard: Map<String, String>) {
    val empty get() = parts.isEmpty()
    val firstPart get() = if (empty) null else parts[0]
    fun dropPart(n: Int) = PartedURL(parts.drop(n), tailcard)
    fun removeTailcard(key: String) = PartedURL(parts, tailcard - key)

    fun <T> tryTailcardParam(name: String, transform: (String) -> T): RouterOption {
        return if (name in tailcard) {
            val value = try {
                transform(tailcard.getValue(name))
            } catch (e: Exception) {
                return RouterOption.Fail("Exception in transformation: $e", true)
            }

            RouterOption.AddParam(name, value, true, removeTailcard(name))
        } else RouterOption.Fail("Parameter $name not in tailcard")
    }

    fun <T> tryUrlParam(name: String, transform: (String) -> T): RouterOption {
        return if (firstPart == name) {
            if (parts.size >= 2) {
                val value = try {
                    transform(parts[1])
                } catch (e: Exception) {
                    return RouterOption.Fail("Exception in transformation: $e", true)
                }

                RouterOption.AddParam(name, value, false, dropPart(2))
            } else RouterOption.Fail("Value for param $name not found: ran out of segments")
        } else RouterOption.Fail("Prefix for param $name not found")
    }
}

enum class ParamLocation {
    Url, Tailcard, Either;
}

sealed class RouterOption {
    class Fail(val message: String, val transformError: Boolean = false) : RouterOption()
    class Success(val newUrl: PartedURL) : RouterOption()
    data class AddParam<T>(val name: String, val value: T, val tailcard: Boolean, val newUrl: PartedURL) :
        RouterOption()

    fun ifFail(next: () -> RouterOption) =
        if (this is Fail && !transformError) next() else this // fail hard on transform errors
}

interface NamedParam {
    val name: String
}

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
    protected abstract fun makeWildcard(): RoutePart.Wildcard

    // no builder, transform

    @RoutingDSL
    fun <T> param(name: String, location: ParamLocation, transform: (String) -> T): RoutePart.Param<T> =
        makeParam(name, location, transform)

    @RoutingDSL
    fun <T> urlParam(name: String, transform: (String) -> T) = param(name, ParamLocation.Url, transform)
    @RoutingDSL
    fun <T> tailcardParam(name: String, transform: (String) -> T) = param(name, ParamLocation.Tailcard, transform)
    @RoutingDSL
    fun <T> eitherParam(name: String, transform: (String) -> T) = param(name, ParamLocation.Either, transform)

    @RoutingDSL
    fun <T> optionalParam(name: String, location: ParamLocation, transform: (String) -> T) =
        makeOptionalParam(name, location, transform)

    @RoutingDSL fun <T> urlOptionalParam(name: String, transform: (String) -> T) = optionalParam(name, ParamLocation.Url, transform)
    @RoutingDSL fun <T> tailcardOptionalParam(name: String, transform: (String) -> T) =
        optionalParam(name, ParamLocation.Tailcard, transform)

    @RoutingDSL fun <T> eitherOptionalParam(name: String, transform: (String) -> T) =
        optionalParam(name, ParamLocation.Either, transform)

    @RoutingDSL fun <T> anonymousParam(name: String, transform: (String) -> T) = makeAnonymousParam(name, transform)

    @RoutingDSL fun static(name: String) = makeStatic(name)

    @RoutingDSL fun static(vararg names: String): RoutePart.Static {
        var part = static(names.first())
        names.drop(1).forEach {
            part = part.static(it)
        }
        return part
    }

    @RoutingDSL fun wildcard() = makeWildcard()

    /**
     * Allows statics and wildcards, separated by slashes
     */
    @RoutingDSL fun path(path: String): RoutePart {
        val parts = path.split("/")
        var part = parts.first().let {
            if (it == "*")
                wildcard()
            else
                static(it)
        }

        parts.drop(1).forEach {
            part = if (it == "*")
                part.wildcard()
            else
                part.static(it)
        }
        return part
    }

    // no builder, no transform

    @RoutingDSL fun stringParam(name: String, location: ParamLocation) = param(name, location, { it })
    @RoutingDSL fun urlStringParam(name: String) = stringParam(name, ParamLocation.Url)
    @RoutingDSL fun tailcardStringParam(name: String) = stringParam(name, ParamLocation.Tailcard)
    @RoutingDSL fun eitherStringParam(name: String) = stringParam(name, ParamLocation.Either)

    @RoutingDSL fun optionalStringParam(name: String, location: ParamLocation) = optionalParam(name, location, { it })
    @RoutingDSL fun urlOptionalStringParam(name: String) = optionalStringParam(name, ParamLocation.Url)
    @RoutingDSL fun tailcardOptionalStringParam(name: String) = optionalStringParam(name, ParamLocation.Tailcard)
    @RoutingDSL fun eitherOptionalStringParam(name: String) = optionalStringParam(name, ParamLocation.Either)

    @RoutingDSL fun anonymousStringParam(name: String) = anonymousParam(name, { it })


    // builder, transform

    @RoutingDSL fun <T> param(
        name: String,
        location: ParamLocation,
        transform: (String) -> T,
        builder: RoutePart.Param<T>.(RoutePart.Param<T>) -> Unit
    ): RoutePart.Param<T> {
        contract {
            callsInPlace(builder, InvocationKind.EXACTLY_ONCE)
        }
        return param(name, location, transform).also { it.invoke(builder) }
    }

    @RoutingDSL fun <T> urlParam(
        name: String,
        transform: (String) -> T,
        builder: RoutePart.Param<T>.(RoutePart.Param<T>) -> Unit
    ): RoutePart.Param<T> {
        contract {
            callsInPlace(builder, InvocationKind.EXACTLY_ONCE)
        }
        return param(name, ParamLocation.Url, transform, builder)
    }

    @RoutingDSL fun <T> tailcardParam(
        name: String,
        transform: (String) -> T,
        builder: RoutePart.Param<T>.(RoutePart.Param<T>) -> Unit
    ): RoutePart.Param<T> {

        contract {
            callsInPlace(builder, InvocationKind.EXACTLY_ONCE)
        }
        return param(name, ParamLocation.Tailcard, transform, builder)
    }

    @RoutingDSL fun <T> eitherParam(
        name: String,
        transform: (String) -> T,
        builder: RoutePart.Param<T>.(RoutePart.Param<T>) -> Unit
    ): RoutePart.Param<T> {
        contract {
            callsInPlace(builder, InvocationKind.EXACTLY_ONCE)
        }
        return param(name, ParamLocation.Either, transform, builder)
    }

    @RoutingDSL fun <T> optionalParam(
        name: String,
        location: ParamLocation,
        transform: (String) -> T,
        builder: RoutePart.OptionalParam<T>.(RoutePart.OptionalParam<T>) -> Unit
    ): RoutePart.OptionalParam<T> {
        contract {
            callsInPlace(builder, InvocationKind.EXACTLY_ONCE)
        }
        return optionalParam(name, location, transform).also { it.invoke(builder) }
    }

    @RoutingDSL fun <T> urlOptionalParam(
        name: String,
        transform: (String) -> T,
        builder: RoutePart.OptionalParam<T>.(RoutePart.OptionalParam<T>) -> Unit
    ): RoutePart.OptionalParam<T> {
        contract {
            callsInPlace(builder, InvocationKind.EXACTLY_ONCE)
        }
        return optionalParam(name, ParamLocation.Url, transform, builder)
    }

    @RoutingDSL fun <T> tailcardOptionalParam(
        name: String,
        transform: (String) -> T,
        builder: RoutePart.OptionalParam<T>.(RoutePart.OptionalParam<T>) -> Unit
    ): RoutePart.OptionalParam<T> {
        contract {
            callsInPlace(builder, InvocationKind.EXACTLY_ONCE)
        }
        return optionalParam(name, ParamLocation.Tailcard, transform, builder)
    }

    @RoutingDSL fun <T> eitherOptionalParam(
        name: String,
        transform: (String) -> T,
        builder: RoutePart.OptionalParam<T>.(RoutePart.OptionalParam<T>) -> Unit
    ): RoutePart.OptionalParam<T> {
        contract {
            callsInPlace(builder, InvocationKind.EXACTLY_ONCE)
        }
        return optionalParam(name, ParamLocation.Either, transform, builder)
    }

    @RoutingDSL fun <T> anonymousParam(
        name: String,
        transform: (String) -> T,
        builder: RoutePart.AnonymousParam<T>.(RoutePart.AnonymousParam<T>) -> Unit
    ): RoutePart.AnonymousParam<T> {
        contract {
            callsInPlace(builder, InvocationKind.EXACTLY_ONCE)
        }
        return anonymousParam(name, transform).also { it.invoke(builder) }
            .also { it.invoke(builder) }
    }

    @RoutingDSL fun static(name: String, builder: RoutePart.() -> Unit): RoutePart.Static {
        contract {
            callsInPlace(builder, InvocationKind.EXACTLY_ONCE)
        }
        return static(name).also { it.builder() }
    }

    @RoutingDSL fun static(vararg names: String, builder: RoutePart.() -> Unit): RoutePart.Static {
        contract {
            callsInPlace(builder, InvocationKind.EXACTLY_ONCE)
        }

        return static(*names).also { it.builder() }
    }

    @RoutingDSL fun wildcard(builder: RoutePart.() -> Unit): RoutePart.Wildcard {
        contract {
            callsInPlace(builder, InvocationKind.EXACTLY_ONCE)
        }
        return wildcard().also { it.builder() }
    }

    // builder, no transform

    @RoutingDSL fun stringParam(
        name: String,
        location: ParamLocation,
        builder: RoutePart.Param<String>.(RoutePart.Param<String>) -> Unit
    ): RoutePart.Param<String> {
        contract {
            callsInPlace(builder, InvocationKind.EXACTLY_ONCE)
        }
        return stringParam(name, location).also { it.invoke(builder) }
    }

    @RoutingDSL fun urlStringParam(
        name: String,
        builder: RoutePart.Param<String>.(RoutePart.Param<String>) -> Unit
    ): RoutePart.Param<String> {
        contract {
            callsInPlace(builder, InvocationKind.EXACTLY_ONCE)
        }
        return stringParam(name, ParamLocation.Url, builder)
    }

    @RoutingDSL fun tailcardStringParam(
        name: String,
        builder: RoutePart.Param<String>.(RoutePart.Param<String>) -> Unit
    ): RoutePart.Param<String> {
        contract {
            callsInPlace(builder, InvocationKind.EXACTLY_ONCE)
        }
        return stringParam(name, ParamLocation.Tailcard, builder)
    }

    @RoutingDSL fun eitherStringParam(
        name: String,
        builder: RoutePart.Param<String>.(RoutePart.Param<String>) -> Unit
    ): RoutePart.Param<String> {
        contract {
            callsInPlace(builder, InvocationKind.EXACTLY_ONCE)
        }
        return stringParam(name, ParamLocation.Either, builder)
    }

    @RoutingDSL fun optionalStringParam(
        name: String,
        location: ParamLocation,
        builder: RoutePart.OptionalParam<String>.(RoutePart.OptionalParam<String>) -> Unit
    ): RoutePart.OptionalParam<String> {
        contract {
            callsInPlace(builder, InvocationKind.EXACTLY_ONCE)
        }
        return optionalStringParam(name, location).also { it.invoke(builder) }
    }

    @RoutingDSL fun urlOptionalStringParam(
        name: String,
        builder: RoutePart.OptionalParam<String>.(RoutePart.OptionalParam<String>) -> Unit
    ): RoutePart.OptionalParam<String> {
        contract {
            callsInPlace(builder, InvocationKind.EXACTLY_ONCE)
        }
        return optionalStringParam(name, ParamLocation.Url, builder)
    }

    @RoutingDSL fun tailcardOptionalStringParam(
        name: String,
        builder: RoutePart.OptionalParam<String>.(RoutePart.OptionalParam<String>) -> Unit
    ): RoutePart.OptionalParam<String> {
        contract {
            callsInPlace(builder, InvocationKind.EXACTLY_ONCE)
        }
        return optionalStringParam(name, ParamLocation.Tailcard, builder)
    }

    @RoutingDSL fun eitherOptionalStringParam(
        name: String,
        builder: RoutePart.OptionalParam<String>.(RoutePart.OptionalParam<String>) -> Unit
    ): RoutePart.OptionalParam<String> {
        contract {
            callsInPlace(builder, InvocationKind.EXACTLY_ONCE)
        }
        return optionalStringParam(name, ParamLocation.Either, builder)
    }

    @RoutingDSL fun anonymousStringParam(
        name: String,
        builder: RoutePart.AnonymousParam<String>.(RoutePart.AnonymousParam<String>) -> Unit
    ): RoutePart.AnonymousParam<String> {
        contract {
            callsInPlace(builder, InvocationKind.EXACTLY_ONCE)
        }
        return anonymousStringParam(name).also { it.invoke(builder) }
    }
}

private fun checkUrlPart(name: String) {
    if ('/' in name)
        error("/s not allowed in url parts")

    if (name.isEmpty())
        error("Empty url parts not allowed")

    Regex("[0-9A-Za-z\\-._~]+").matchEntire(name)
        ?: error("Url part $name contains an illegal character. Allowed: [0-9A-Za-z-._~]")
}

sealed class RoutePart(val parent: RoutePart?, val routes: BaseRoutes) : RouteBuilder() {

    abstract fun handle(url: PartedURL): RouterOption

    class Static internal constructor(val name: String, parent: RoutePart?, routes: BaseRoutes) :
        RoutePart(parent, routes) {
        init {
            checkUrlPart(name)
        }

        override fun handle(url: PartedURL): RouterOption =
            if (url.firstPart == name)
                RouterOption.Success(url.dropPart(1))
            else
                RouterOption.Fail("Required segment not found: $name")

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || this::class != other::class) return false

            other as Static

            if (name != other.name) return false

            return true
        }

        override fun hashCode(): Int {
            return name.hashCode()
        }

        override fun toString(): String {
            return "Static(name='$name')"
        }

    }

    class Wildcard internal constructor(parent: RoutePart?, routes: BaseRoutes) : RoutePart(parent, routes) {
        override fun handle(url: PartedURL): RouterOption =
            if (url.firstPart != null)
                RouterOption.Success(url.dropPart(1))
            else
                RouterOption.Fail("Required a segment, didn't get any")

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || this::class != other::class) return false
            return true
        }

        override fun hashCode(): Int {
            return this::class.hashCode()
        }

        override fun toString(): String {
            return "Wildcard()"
        }

    }

    class Param<T> internal constructor(
        override val name: String,
        val location: ParamLocation,
        val transform: (String) -> T,
        parent: RoutePart?,
        routes: BaseRoutes
    ) :
        RoutePart(parent, routes), NamedParam {

        init {
            checkUrlPart(name)

            if (backtrace.any { if (it is NamedParam) name == it.name else false })
                error("Already declared param with name \"$name\" for this route: $backtrace")
        }

        operator fun invoke(builder: Param<T>.(Param<T>) -> Unit) = builder(this)

        override fun handle(url: PartedURL): RouterOption =
            when (location) {
                ParamLocation.Url -> url.tryUrlParam(name, transform)
                ParamLocation.Tailcard -> url.tryTailcardParam(name, transform)
                ParamLocation.Either -> url.tryUrlParam(name, transform)
                    .ifFail { url.tryTailcardParam(name, transform) }
            }

        override fun toString(): String {
            return "Param(name='$name', location=$location)"
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || this::class != other::class) return false

            other as Param<*>

            if (name != other.name) return false
            if (location != other.location) return false

            return true
        }

        override fun hashCode(): Int {
            var result = name.hashCode()
            result = 31 * result + location.hashCode()
            return result
        }
    }

    class OptionalParam<T> internal constructor(
        override val name: String,
        val location: ParamLocation,
        val transform: (String) -> T,
        parent: RoutePart?,
        routes: BaseRoutes
    ) :
        RoutePart(parent, routes), NamedParam {

        init {
            checkUrlPart(name)


            if (backtrace.any { if (it is NamedParam) name == it.name else false })
                error("Already declared param with name \"$name\" for this route: $backtrace")
        }

        operator fun invoke(builder: OptionalParam<T>.(OptionalParam<T>) -> Unit) = builder(this)

        override fun handle(url: PartedURL): RouterOption =
            when (location) {
                ParamLocation.Url -> url.tryUrlParam(name, transform)
                ParamLocation.Tailcard -> url.tryTailcardParam(name, transform)
                ParamLocation.Either -> url.tryUrlParam(name, transform)
                    .ifFail { url.tryTailcardParam(name, transform) }
            }

        override fun toString(): String {
            return "OptionalParam(name='$name', location=$location)"
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || this::class != other::class) return false

            other as OptionalParam<*>

            if (name != other.name) return false
            if (location != other.location) return false

            return true
        }

        override fun hashCode(): Int {
            var result = name.hashCode()
            result = 31 * result + location.hashCode()
            return result
        }
    }

    class AnonymousParam<T> internal constructor(
        override val name: String,
        val transform: (String) -> T,
        parent: RoutePart?,
        routes: BaseRoutes
    ) :
        RoutePart(parent, routes), NamedParam {

        init {
            checkUrlPart(name)


            if (backtrace.any { if (it is NamedParam) name == it.name else false })
                error("Already declared param with name \"$name\" for this route: $backtrace")
        }

        operator fun invoke(builder: AnonymousParam<T>.(AnonymousParam<T>) -> Unit) = builder(this)

        override fun handle(url: PartedURL): RouterOption {
            return if (url.firstPart != null) {
                val value = try {
                    transform(url.firstPart!!)
                } catch (e: Exception) {
                    return RouterOption.Fail("Exception in transformation: $e", true)
                }
                RouterOption.AddParam(name, value, false, url.dropPart(1))
            } else RouterOption.Fail("No more segments")
        }

        override fun toString(): String {
            return "AnonymousParam(name='$name')"
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || this::class != other::class) return false

            other as AnonymousParam<*>

            if (name != other.name) return false

            return true
        }

        override fun hashCode(): Int {
            return name.hashCode()
        }
    }

    val backtrace: List<RoutePart> get() = parent?.backtrace?.plus(this) ?: emptyList()

    operator fun invoke(builder: RoutePart.() -> Unit) = builder()


    override fun <T> makeParam(name: String, location: ParamLocation, transform: (String) -> T) =
        Param(name, location, transform, this, routes)

    override fun <T> makeOptionalParam(name: String, location: ParamLocation, transform: (String) -> T) =
        OptionalParam(name, location, transform, this, routes)

    override fun <T> makeAnonymousParam(name: String, transform: (String) -> T) =
        AnonymousParam(name, transform, this, routes)

    override fun makeStatic(name: String) = Static(name, this, routes)
    override fun makeWildcard() = Wildcard(this, routes)


    private var pageRegistered = false

    operator fun <T : Any> PageDef<T>.invoke(dataBuilder: DataBuilder.() -> T): Route<T> {

        if (pageRegistered)
            error("Page already registered for this route")

        val route = Route(this, backtrace, dataBuilder)

        routes += route

        pageRegistered = true
        return route
    }

    operator fun PageDef<Unit>.invoke() = invoke { Unit }
}
