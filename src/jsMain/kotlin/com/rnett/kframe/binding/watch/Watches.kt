package com.rnett.kframe.binding.watch

import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

class DelegateWatch<T>(val delegate: ReadWriteProperty<Any?, T>) : Watch<T>() {

    private var _storedProperty: KProperty<*>? = null

    @PublishedApi
    internal val storedProperty
        get() = _storedProperty ?: error("Must initialize DelegateWatch with a property by delegating from it")

    operator fun provideDelegate(thisRef: Any?, property: KProperty<*>): DelegateWatch<T> {
        _storedProperty = property
        return this
    }

    override fun doGet() = delegate.getValue(null, storedProperty)

    override fun doSet(value: T) = delegate.setValue(null, storedProperty, value)
}

class WrapperWatch<T>(initialValue: T) : Watch<T>() {
    private var _value = initialValue

    override fun doGet(): T = _value

    override fun doSet(value: T) {
        this._value = value
    }
}

class LateinitWrapperWatch<T>() : Watch<T>() {
    private var _value: T? = null

    override fun doGet(): T = _value ?: error("Must initialize before use")

    override fun doSet(value: T) {
        _value = value
    }
}

fun <T> watch(delegate: ReadWriteProperty<Any?, T>) =
    DelegateWatch(delegate)

fun <T> watchWrapper(value: T) = WrapperWatch(value)

fun <T> watchLateinitWrapper() = LateinitWrapperWatch<T>()