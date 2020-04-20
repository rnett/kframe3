package com.rnett.kframe.utils

import kotlin.properties.ReadOnlyProperty
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

class Wrapper<T>(val value: T) : ReadOnlyProperty<Any?, T> {
    override inline operator fun getValue(thisRef: Any?, property: KProperty<*>) = value
}

class MutableWrapper<T>(var value: T) : ReadWriteProperty<Any?, T> {
    override inline operator fun getValue(thisRef: Any?, property: KProperty<*>) = value
    override inline operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        this.value = value
    }
}

class PropNameHelper<T>(val value: (String) -> T){
    inline operator fun provideDelegate(thisRef: Any?, property: KProperty<*>) =
        Wrapper(value(property.name))
}