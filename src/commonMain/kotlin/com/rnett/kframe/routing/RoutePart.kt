package com.rnett.kframe.routing

@DslMarker
@MustBeDocumented
@Retention(AnnotationRetention.BINARY)
annotation class RoutingDSL

class UrlData(val urlParams: Map<String, Any?>, val tailcardParams: Map<String, Any?>) {

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

data class PartedUrl(val parts: List<String> = listOf(), val tailcard: Map<String, String> = mapOf()) {
    val empty get() = parts.isEmpty()
    val firstPart get() = if (empty) null else parts[0]
    fun dropPart(n: Int) = PartedUrl(parts.drop(n), tailcard)
    fun removeTailcard(key: String) = PartedUrl(parts, tailcard - key)

    fun <T> tryTailcardParam(name: String, transform: (String) -> T): RouterOption {
        return if (name in tailcard) {
            val value = try {
                transform(tailcard.getValue(name))
            } catch (e: Exception) {
                return RouterOption.Fail("Exception in transformation: $e", true)
            }

            RouterOption.AddParam(name, value, tailcard.getValue(name), true, removeTailcard(name))
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

                RouterOption.AddParam(name, value, parts[1], false, dropPart(2))
            } else RouterOption.Fail("Value for param $name not found: ran out of segments")
        } else RouterOption.Fail("Prefix for param $name not found")
    }

    operator fun plus(other: PartedUrl): PartedUrl {
        tailcard.forEach { (key, value) ->
            if (key in other.tailcard && other.tailcard[key] != value)
                error("Mismatched tailcard param $key, got values $value and ${other.tailcard[key]}")
        }

        return PartedUrl(parts + other.parts, tailcard + other.tailcard)
    }

    fun toUrl(): String = buildString {
        append(parts.joinToString("/"))

        if (tailcard.isNotEmpty()) {
            append("?")
            append(tailcard.entries.joinToString("&") { (key, value) -> "$key=$value" })
        }
    }
}

enum class ParamLocation {
    Url, Tailcard, Either;
}

sealed class RouterOption {
    class Fail(val message: String, val transformError: Boolean = false) : RouterOption()
    class Success(val newUrl: PartedUrl) : RouterOption()
    data class AddParam<T>(
        val name: String,
        val value: T,
        val raw: String,
        val tailcard: Boolean,
        val newUrl: PartedUrl
    ) :
        RouterOption()

    fun ifFail(next: () -> RouterOption) =
        if (this is Fail && !transformError) next() else this // fail hard on transform errors
}

interface NamedParam {
    val name: String
}

private fun checkUrlPart(name: String) {
    if ('/' in name)
        error("/s not allowed in url parts")

    if (name.isEmpty())
        error("Empty url parts not allowed")

    Regex("[0-9A-Za-z\\-._~]+").matchEntire(name)
        ?: error("Url part $name contains an illegal character. Allowed: [0-9A-Za-z-._~]")
}

sealed class RoutePart(val parent: RoutePart?, val routes: RoutingDefinition) : RouteBuilder() {

    abstract fun parse(url: PartedUrl): RouterOption
    abstract fun toUrlParts(data: Map<String, String>): PartedUrl

    class Static internal constructor(val name: String, parent: RoutePart?, routes: RoutingDefinition) :
        RoutePart(parent, routes) {
        init {
            checkUrlPart(name)
        }

        override fun parse(url: PartedUrl): RouterOption =
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

        override fun toUrlParts(data: Map<String, String>) = PartedUrl(listOf(name))
    }

//    class Wildcard internal constructor(parent: RoutePart?, routes: RoutingDefinition) : RoutePart(parent, routes) {
//        override fun parse(url: PartedURL): RouterOption =
//            if (url.firstPart != null)
//                RouterOption.Success(url.dropPart(1))
//            else
//                RouterOption.Fail("Required a segment, didn't get any")
//
//        override fun equals(other: Any?): Boolean {
//            if (this === other) return true
//            if (other == null || this::class != other::class) return false
//            return true
//        }
//
//        override fun hashCode(): Int {
//            return this::class.hashCode()
//        }
//
//        override fun toString(): String {
//            return "Wildcard()"
//        }
//
//    }

    class Param<T> internal constructor(
        override val name: String,
        val location: ParamLocation,
        val transform: (String) -> T,
        parent: RoutePart?,
        routes: RoutingDefinition
    ) :
        RoutePart(parent, routes), NamedParam {

        init {
            checkUrlPart(name)

            if (backtrace.any { if (it !== this && it is NamedParam) name == it.name else false })
                error("Already declared param with name \"$name\" for this route: $backtrace")
        }

        inline fun build(builder: Param<T>.(Param<T>) -> Unit) = builder(this)

        override fun parse(url: PartedUrl): RouterOption =
            when (location) {
                ParamLocation.Url -> url.tryUrlParam(name, transform)
                ParamLocation.Tailcard -> url.tryTailcardParam(name, transform)
                ParamLocation.Either -> url.tryUrlParam(name, transform)
                    .ifFail { url.tryTailcardParam(name, transform) }
            }

        override fun toUrlParts(data: Map<String, String>): PartedUrl =
            if (location == ParamLocation.Tailcard)
                PartedUrl(tailcard = mapOf(name to data.getValue(name)))
            else
                PartedUrl(listOf(name, data.getValue(name)))

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
        routes: RoutingDefinition
    ) :
        RoutePart(parent, routes), NamedParam {

        init {
            checkUrlPart(name)


            if (backtrace.any { if (it !== this && it is NamedParam) name == it.name else false })
                error("Already declared param with name \"$name\" for this route: $backtrace")
        }

        inline fun build(builder: OptionalParam<T>.(OptionalParam<T>) -> Unit) = builder(this)

        override fun parse(url: PartedUrl): RouterOption =
            when (location) {
                ParamLocation.Url -> url.tryUrlParam(name, transform).ifFail { RouterOption.Success(url) }
                ParamLocation.Tailcard -> url.tryTailcardParam(name, transform).ifFail { RouterOption.Success(url) }
                ParamLocation.Either -> url.tryUrlParam(name, transform)
                    .ifFail { url.tryTailcardParam(name, transform) }.ifFail { RouterOption.Success(url) }
            }

        override fun toString(): String {
            return "OptionalParam(name='$name', location=$location)"
        }

        override fun toUrlParts(data: Map<String, String>): PartedUrl = if (name in data) {
            if (location == ParamLocation.Tailcard)
                PartedUrl(tailcard = mapOf(name to data.getValue(name)))
            else
                PartedUrl(listOf(name, data.getValue(name)))
        } else PartedUrl()

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
        routes: RoutingDefinition
    ) :
        RoutePart(parent, routes), NamedParam {

        init {
            checkUrlPart(name)


            if (backtrace.any { if (it !== this && it is NamedParam) name == it.name else false })
                error("Already declared param with name \"$name\" for this route: $backtrace")
        }

        inline fun build(builder: AnonymousParam<T>.(AnonymousParam<T>) -> Unit) = builder(this)

        override fun parse(url: PartedUrl): RouterOption {
            return if (url.firstPart != null) {
                val value = try {
                    transform(url.firstPart!!)
                } catch (e: Exception) {
                    return RouterOption.Fail("Exception in transformation: $e", true)
                }
                RouterOption.AddParam(name, value, url.firstPart!!, false, url.dropPart(1))
            } else RouterOption.Fail("No more segments")
        }

        override fun toString(): String {
            return "AnonymousParam(name='$name')"
        }

        override fun toUrlParts(data: Map<String, String>): PartedUrl {
            return PartedUrl(listOf(data.getValue(name)))
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

    val backtrace: List<RoutePart> get() = parent?.backtrace?.plus(this) ?: listOf(this)

    inline fun build(builder: RoutePart.() -> Unit) = builder()


    override fun <T> makeParam(name: String, location: ParamLocation, transform: (String) -> T) =
        Param(name, location, transform, this, routes)

    override fun <T> makeOptionalParam(name: String, location: ParamLocation, transform: (String) -> T) =
        OptionalParam(name, location, transform, this, routes)

    override fun <T> makeAnonymousParam(name: String, transform: (String) -> T) =
        AnonymousParam(name, transform, this, routes)

    override fun makeStatic(name: String) = Static(name, this, routes)
//    override fun makeWildcard() = Wildcard(this, routes)


    private var pageRegistered = false

    internal var handler: (UrlData.() -> Unit)? = null
        private set

    /**
     * For doing all-endpoint things like parameter validation.  Ran each time this endpoint has parse attempted.  Throwing an exception will fail the parse
     */
    @RoutingDSL
    fun handle(handler: UrlData.() -> Unit) {
        if (this.handler != null)
            error("Handler already set")

        this.handler = handler
    }

    @PublishedApi
    internal fun <T> addRoute(route: Route<T>) {
        if (pageRegistered)
            error("Page already registered for this route")

        routes += route

        pageRegistered = true
    }

    @RoutingDSL
    final override inline operator fun <T> PageDef<T>.invoke(crossinline dataBuilder: UrlData.() -> T): Route<T> {
        val route = object : Route<T>(this, backtrace, routes) {
            override fun parseData(urlData: UrlData, rawData: Map<String, String>): T = dataBuilder(urlData)

        }

        addRoute(route)
        return route
    }

    @RoutingDSL
    final override inline operator fun <T> PageDef<T>.invoke(
        crossinline toUrl: (T) -> String,
        crossinline dataBuilder: UrlData.() -> T
    ): ReactiveRoute<T> {
        val route = object : ReactiveRoute<T>(this, backtrace, routes) {
            override fun parseData(urlData: UrlData, rawData: Map<String, String>): T = dataBuilder(urlData)
            override fun toUrl(data: T): String = toUrl(data)
        }

        addRoute(route)
        return route
    }

    @RoutingDSL
    override operator fun PageDef<Unit>.invoke(): ReactiveRoute<Unit> {
        if (backtrace.filterIsInstance<NamedParam>().isNotEmpty())
            error("Can't have Unit page with inferred url for a route with parameters.  Specify url in invoke call")
        val url = backtrace.fold(PartedUrl()) { acc, it -> acc + it.toUrlParts(mapOf()) }.toUrl()
        return invoke(url)
    }

    @RoutingDSL
    override operator fun PageDef<Unit>.invoke(url: String): ReactiveRoute<Unit> {
        return invoke({ url }) { Unit }
    }

    @RoutingDSL
    override operator fun <T> JsonPageDef<T>.invoke(renames: Map<String, String>): JsonRoute<T> {
        val route = JsonRoute(this, backtrace, routes, renames)
        addRoute(route)
        return route
    }
}
