package com.rnett.kframe.utils

import com.rnett.kframe.dom.core.JSValue
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty


interface DelegateInterface {
    operator fun get(key: String): String?

    operator fun set(key: String, value: String?)

    operator fun set(key: String, value: Int?) = set(key, value?.toString())
    operator fun set(key: String, value: Double?) = set(key, value?.toString())
    operator fun set(key: String, value: Boolean?) = set(key, value?.toString())

    operator fun contains(key: String) = get(key) != null

    fun remove(key: String) = set(key, null as String?)

    operator fun provideDelegate(thisRef: Any?, prop: KProperty<*>) = by(prop.name)
}

abstract class StringDelegatable(protected val prefix: String = "") : DelegateInterface {

    protected abstract fun getValue(key: String): String?
    protected abstract fun setValue(key: String, value: String?)
    protected open fun transformKey(key: String): String = key

    override operator fun get(key: String): String? = getValue(transformKey(prefix + key))

    override operator fun set(key: String, value: String?) {
        setValue(transformKey(prefix + key), value)
    }
}

abstract class StringDelegatePassthrough(val base: DelegateInterface, protected val prefix: String) :
    DelegateInterface {
    override fun get(key: String) = base[prefix + key]
    override fun set(key: String, value: String?) {
        base[prefix + key] = value
    }
}

inline fun DelegateInterface.by(key: String) = object : ReadWriteProperty<Any?, String?> {
    override fun getValue(thisRef: Any?, property: KProperty<*>) = get(key)

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: String?) {
        set(key, value)
    }
}

inline fun DelegateInterface.byInt(key: String) = object : ReadWriteProperty<Any?, Int?> {
    override fun getValue(thisRef: Any?, property: KProperty<*>) = get(key)?.toIntOrNull()

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: Int?) = set(key, value?.toString())
}

interface Helper<T> {
    operator fun provideDelegate(thisRef: Any?, prop: KProperty<*>): ReadWriteProperty<Any?, T>
}

inline val DelegateInterface.byInt
    inline get() = object : Helper<Int?> {
        override inline operator fun provideDelegate(thisRef: Any?, prop: KProperty<*>) = byInt(prop.name)
    }

inline fun DelegateInterface.byDouble(key: String) = object : ReadWriteProperty<Any?, Double?> {
    override fun getValue(thisRef: Any?, property: KProperty<*>) = get(key)?.toDoubleOrNull()

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: Double?) = set(key, value)
}

inline val DelegateInterface.byDouble
    inline get() = object : Helper<Double?> {
        override inline operator fun provideDelegate(thisRef: Any?, prop: KProperty<*>) = byDouble(prop.name)
    }

inline fun DelegateInterface.byBoolean(key: String) = object : ReadWriteProperty<Any?, Boolean?> {
    override fun getValue(thisRef: Any?, property: KProperty<*>) = get(key)?.toBoolean()

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: Boolean?) = set(key, value)
}

inline val DelegateInterface.byBoolean
    inline get() = object : Helper<Boolean?> {
        override inline operator fun provideDelegate(thisRef: Any?, prop: KProperty<*>) = byBoolean(prop.name)
    }

inline fun DelegateInterface.byBoolFlag(key: String) = object : ReadWriteProperty<Any?, Boolean> {
    override fun getValue(thisRef: Any?, property: KProperty<*>) = get(key) != null

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: Boolean) =
        if (value) set(key, key) else remove(key)
}

inline val DelegateInterface.byBoolFlag
    inline get() = object : Helper<Boolean> {
        override inline operator fun provideDelegate(thisRef: Any?, prop: KProperty<*>) = byBoolFlag(prop.name)
    }

inline fun <T : JSValue?> DelegateInterface.by(key: String, crossinline producer: (String?) -> T) = object :
    ReadWriteProperty<Any?, T> {
    override fun getValue(thisRef: Any?, property: KProperty<*>): T = producer(get(key))

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) = set(key, value?.toJS())
}

inline fun <T : JSValue?> DelegateInterface.by(crossinline producer: (String?) -> T) = object : Helper<T> {
    override inline operator fun provideDelegate(thisRef: Any?, prop: KProperty<*>) = by(prop.name, producer)
}

inline fun <T : Any?> DelegateInterface.by(
    key: String,
    crossinline producer: (String?) -> T,
    crossinline serializer: (T) -> String?
) = object :
    ReadWriteProperty<Any?, T> {
    override fun getValue(thisRef: Any?, property: KProperty<*>): T = producer(get(key))

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) = set(key, serializer(value))
}


inline fun <T : Any?> DelegateInterface.by(
    crossinline producer: (String?) -> T,
    crossinline serializer: (T) -> String?
) = object :
    Helper<T> {
    override inline operator fun provideDelegate(thisRef: Any?, prop: KProperty<*>) =
        by(prop.name, producer, serializer)
}

