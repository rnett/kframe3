package com.rnett.kframe.dom.input

import com.rnett.kframe.binding.watch.Watch
import com.rnett.kframe.binding.watch.watchWrapper
import com.rnett.kframe.dom.core.DisplayElement
import com.rnett.kframe.dom.core.DisplayElementHost
import com.rnett.kframe.dom.core.KFrameDSL
import com.rnett.kframe.dom.core.change
import com.rnett.kframe.utils.by
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

abstract class InputElement<T, S : InputElement<T, S>>(parent: DisplayElementHost, val backing: Watch<T>) :
    DisplayElement<S>(parent, "input") {

    var type by properties
    private var _rawValue by properties.by("value")
    var readonly by properties
    var name by properties

    //TODO better validation + error display/handling.

    abstract fun toString(value: T): String
    abstract fun fromString(rawValue: String): T
    abstract fun validate(rawValue: String, oldValue: T): Boolean
    abstract fun validateValue(newValue: T): Boolean

    var rawValue
        get() = _rawValue ?: ""
        set(value) {
            _rawValue = value
        }

    protected fun updateUIValue(value: T) {
        rawValue = toString(value)
    }

    inline var value
        get() = backing.value
        set(v) {
            backing.value = v
        }

    private val listener = backing.onSet {
        updateUIValue(it)
    }

    override fun remove() {
        listener.remove()
        super.remove()
    }

    var resetOnFailedValidation = true
    var resetOnFailedValueValidation = true

    init {
        updateUIValue(value)

        on.change {
            if (validate(rawValue, value)) {
                try {
                    val newValue = fromString(rawValue)
                    if (validateValue(newValue))
                        value = newValue
                    else if (resetOnFailedValueValidation)
                        updateUIValue(value)
                } catch (e: Exception) {
                    updateUIValue(value)
                }
            } else if (resetOnFailedValidation) {
                updateUIValue(value)
            }
        }
    }
}

abstract class CustomInput<T>(parent: DisplayElementHost, backing: Watch<T>) :
    InputElement<T, CustomInput<T>>(parent, backing)

abstract class IntInput(parent: DisplayElementHost, backing: Watch<Int>) :
    InputElement<Int, IntInput>(parent, backing) {
    override fun toString(value: Int): String {
        return value.toString()
    }

    override fun fromString(rawValue: String): Int {
        return rawValue.toInt()
    }

    override fun validate(rawValue: String, oldValue: Int): Boolean =
        if (rawValue.toIntOrNull() == null) {
            updateUIValue(oldValue)
            false
        } else true

    init {
        type = "number"
        properties["step"] = "1"
    }
}

abstract class NullableIntInput(parent: DisplayElementHost, backing: Watch<Int?>) :
    InputElement<Int?, NullableIntInput>(parent, backing) {
    override fun toString(value: Int?): String {
        return value?.toString() ?: ""
    }

    override fun fromString(rawValue: String): Int? {
        return if (rawValue.isEmpty()) null else rawValue.toInt()
    }

    override fun validate(rawValue: String, oldValue: Int?): Boolean =
        if (rawValue.isNotEmpty() && rawValue.toIntOrNull() == null) {
            updateUIValue(oldValue)
            false
        } else true

    init {
        type = "number"
        properties["step"] = "1"
    }
}

abstract class DoubleInput(parent: DisplayElementHost, backing: Watch<Double>) :
    InputElement<Double, DoubleInput>(parent, backing) {
    override fun toString(value: Double): String {
        return value.toString()
    }

    override fun fromString(rawValue: String): Double {
        return rawValue.toDouble()
    }

    override fun validate(rawValue: String, oldValue: Double): Boolean =
        if (rawValue.toDoubleOrNull() == null) {
            updateUIValue(oldValue)
            false
        } else true

    init {
        type = "number"
    }
}

abstract class NullableDoubleInput(parent: DisplayElementHost, backing: Watch<Double?>) :
    InputElement<Double?, NullableDoubleInput>(parent, backing) {
    override fun toString(value: Double?): String {
        return value?.toString() ?: ""
    }

    override fun fromString(rawValue: String): Double? {
        return if (rawValue.isEmpty()) null else rawValue.toDouble()
    }

    override fun validate(rawValue: String, oldValue: Double?): Boolean =
        if (rawValue.isNotEmpty() && rawValue.toDoubleOrNull() == null) {
            updateUIValue(oldValue)
            false
        } else true

    init {
        type = "number"
    }
}

abstract class StringInput(parent: DisplayElementHost, backing: Watch<String>) :
    InputElement<String, StringInput>(parent, backing) {
    override fun toString(value: String): String {
        return value
    }

    override fun fromString(rawValue: String): String {
        return rawValue
    }

    override fun validate(rawValue: String, oldValue: String): Boolean = true

    init {
        type = "text"
    }
}

abstract class NullableStringInput(parent: DisplayElementHost, backing: Watch<String?>) :
    InputElement<String?, NullableStringInput>(parent, backing) {
    override fun toString(value: String?): String {
        return value ?: ""
    }

    override fun fromString(rawValue: String): String? {
        return rawValue
    }

    override fun validate(rawValue: String, oldValue: String?): Boolean = true

    init {
        type = "text"
    }
}

@KFrameDSL
inline fun <T> DisplayElementHost.input(
    backing: Watch<T>,
    crossinline toString: (T) -> String,
    crossinline fromString: (String) -> T,
    crossinline validate: CustomInput<T>.(rawValue: String, newValue: T) -> Boolean = { _, _ -> true },
    crossinline validateValue: CustomInput<T>.(T) -> Boolean = { true },
    type: String = "text",
    builder: CustomInput<T>.() -> Unit = {}
): CustomInput<T> {
    contract {
        callsInPlace(builder, InvocationKind.EXACTLY_ONCE)
    }

    return object : CustomInput<T>(this, backing) {
        override fun toString(value: T): String = toString(value)

        override fun fromString(rawValue: String): T = fromString(rawValue)

        override fun validate(rawValue: String, oldValue: T): Boolean = validate(this, rawValue, oldValue)

        override fun validateValue(newValue: T): Boolean = validateValue(this, newValue)

        init {
            this.type = type
        }
    }.also(builder)
}

@KFrameDSL
inline fun <T> DisplayElementHost.input(
    initial: T,
    crossinline toString: (T) -> String,
    crossinline fromString: (String) -> T,
    crossinline validate: CustomInput<T>.(rawValue: String, newValue: T) -> Boolean = { _, _ -> true },
    crossinline validateValue: CustomInput<T>.(T) -> Boolean = { true },
    type: String = "text",
    builder: CustomInput<T>.() -> Unit = {}
): CustomInput<T> {
    contract {
        callsInPlace(builder, InvocationKind.EXACTLY_ONCE)
    }

    return input(watchWrapper(initial), toString, fromString, validate, validateValue, type, builder)
}

@KFrameDSL
inline fun DisplayElementHost.intInput(
    backing: Watch<Int>,
    crossinline validate: IntInput.(Int) -> Boolean = { true },
    builder: IntInput.() -> Unit = {}
): IntInput {
    contract {
        callsInPlace(builder, InvocationKind.EXACTLY_ONCE)
    }

    return object : IntInput(this, backing) {
        override fun validateValue(newValue: Int): Boolean = validate(this, newValue)
    }.also(builder)
}

@KFrameDSL
inline fun DisplayElementHost.intInput(
    initial: Int,
    crossinline validate: IntInput.(Int) -> Boolean = { true },
    builder: IntInput.() -> Unit = {}
): IntInput {
    contract {
        callsInPlace(builder, InvocationKind.EXACTLY_ONCE)
    }

    return intInput(watchWrapper(initial), validate, builder)
}

@KFrameDSL
inline fun DisplayElementHost.nullableIntInput(
    backing: Watch<Int?>,
    crossinline validate: NullableIntInput.(Int?) -> Boolean = { true },
    builder: NullableIntInput.() -> Unit = {}
): NullableIntInput {
    contract {
        callsInPlace(builder, InvocationKind.EXACTLY_ONCE)
    }

    return object : NullableIntInput(this, backing) {
        override fun validateValue(newValue: Int?): Boolean = validate(this, newValue)
    }.also(builder)
}

@KFrameDSL
inline fun DisplayElementHost.nullableIntInput(
    initial: Int?,
    crossinline validate: NullableIntInput.(Int?) -> Boolean = { true },
    builder: NullableIntInput.() -> Unit = {}
): NullableIntInput {
    contract {
        callsInPlace(builder, InvocationKind.EXACTLY_ONCE)
    }

    return nullableIntInput(watchWrapper(initial), validate, builder)
}

@KFrameDSL
inline fun DisplayElementHost.doubleInput(
    backing: Watch<Double>,
    crossinline validate: DoubleInput.(Double) -> Boolean = { true },
    builder: DoubleInput.() -> Unit = {}
): DoubleInput {
    contract {
        callsInPlace(builder, InvocationKind.EXACTLY_ONCE)
    }

    return object : DoubleInput(this, backing) {
        override fun validateValue(newValue: Double): Boolean = validate(this, newValue)
    }.also(builder)
}

@KFrameDSL
inline fun DisplayElementHost.doubleInput(
    initial: Double,
    crossinline validate: DoubleInput.(Double) -> Boolean = { true },
    builder: DoubleInput.() -> Unit = {}
): DoubleInput {
    contract {
        callsInPlace(builder, InvocationKind.EXACTLY_ONCE)
    }

    return doubleInput(watchWrapper(initial), validate, builder)
}

@KFrameDSL
inline fun DisplayElementHost.nullableDoubleInput(
    backing: Watch<Double?>,
    crossinline validate: NullableDoubleInput.(Double?) -> Boolean = { true },
    builder: NullableDoubleInput.() -> Unit = {}
): NullableDoubleInput {
    contract {
        callsInPlace(builder, InvocationKind.EXACTLY_ONCE)
    }

    return object : NullableDoubleInput(this, backing) {
        override fun validateValue(newValue: Double?): Boolean = validate(this, newValue)
    }.also(builder)
}

@KFrameDSL
inline fun DisplayElementHost.nullableDoubleInput(
    initial: Double?,
    crossinline validate: NullableDoubleInput.(Double?) -> Boolean = { true },
    builder: NullableDoubleInput.() -> Unit = {}
): NullableDoubleInput {
    contract {
        callsInPlace(builder, InvocationKind.EXACTLY_ONCE)
    }

    return nullableDoubleInput(watchWrapper(initial), validate, builder)
}

@KFrameDSL
inline fun DisplayElementHost.stringInput(
    backing: Watch<String>,
    crossinline validate: StringInput.(String) -> Boolean = { true },
    builder: StringInput.() -> Unit = {}
): StringInput {
    contract {
        callsInPlace(builder, InvocationKind.EXACTLY_ONCE)
    }

    return object : StringInput(this, backing) {
        override fun validateValue(newValue: String): Boolean = validate(this, newValue)
    }.also(builder)
}

@KFrameDSL
inline fun DisplayElementHost.stringInput(
    initial: String,
    crossinline validate: StringInput.(String) -> Boolean = { true },
    builder: StringInput.() -> Unit = {}
): StringInput {
    contract {
        callsInPlace(builder, InvocationKind.EXACTLY_ONCE)
    }

    return stringInput(watchWrapper(initial), validate, builder)
}

@KFrameDSL
inline fun DisplayElementHost.nullableStringInput(
    backing: Watch<String?>,
    crossinline validate: NullableStringInput.(String?) -> Boolean = { true },
    builder: NullableStringInput.() -> Unit = {}
): NullableStringInput {
    contract {
        callsInPlace(builder, InvocationKind.EXACTLY_ONCE)
    }

    return object : NullableStringInput(this, backing) {
        override fun validateValue(newValue: String?): Boolean = validate(this, newValue)
    }.also(builder)
}

@KFrameDSL
inline fun DisplayElementHost.nullableStringInput(
    initial: String?,
    crossinline validate: NullableStringInput.(String?) -> Boolean = { true },
    builder: NullableStringInput.() -> Unit = {}
): NullableStringInput {
    contract {
        callsInPlace(builder, InvocationKind.EXACTLY_ONCE)
    }

    return nullableStringInput(watchWrapper(initial), validate, builder)
}