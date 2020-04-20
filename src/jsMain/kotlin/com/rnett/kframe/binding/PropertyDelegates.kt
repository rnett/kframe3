package com.rnett.kframe.binding

import kotlin.properties.ReadOnlyProperty
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KMutableProperty0
import kotlin.reflect.KProperty
import kotlin.reflect.KProperty0

class MutablePropertyDelegate<T>(val prop: KMutableProperty0<T>) : ReadWriteProperty<Any?, T> {
    override inline fun getValue(thisRef: Any?, property: KProperty<*>) = prop.get()

    override inline fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        prop.set(value)
    }
}

class PropertyDelegate<T>(val prop: KProperty0<T>) : ReadOnlyProperty<Any?, T> {
    override inline fun getValue(thisRef: Any?, property: KProperty<*>) = prop.get()
}