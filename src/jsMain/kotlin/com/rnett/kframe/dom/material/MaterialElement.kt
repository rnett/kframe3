package com.rnett.kframe.dom.material

import com.rnett.kframe.dom.core.DisplayElement
import com.rnett.kframe.dom.core.DisplayElementHost
import com.rnett.kframe.dom.core.providers.RealizedExistenceProvider
import com.rnett.kframe.utils.Helper
import org.w3c.dom.HTMLElement
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

abstract class MaterialElement<S: MaterialElement<S>>(parent: DisplayElementHost, tag: String, val materialClass: String, val materialInit: (HTMLElement) -> Any?): DisplayElement<S>(parent, tag){

    init {
        classes += materialClass
        realizedProviderOrNull?.let{ materialInit(it.underlying)  }
    }

    override fun initUnderlying(provider: RealizedExistenceProvider) {
        classes += materialClass
        if(materialInit != null) // shouldn't have to do this, but I do
            materialInit(provider.underlying)
    }

    inner class MaterialClassFlagDelegate(val flag: String): ReadWriteProperty<Any?, Boolean>{
        val fullClass = "$materialClass--$flag"

        override inline operator fun getValue(thisRef: Any?, property: KProperty<*>): Boolean = fullClass in classes

        override inline operator fun setValue(thisRef: Any?, property: KProperty<*>, value: Boolean) {
            classes += fullClass
        }
    }

    protected fun byMaterialFlag(flag: String) = MaterialClassFlagDelegate(flag)
    protected val byMaterialFlag get() = object : Helper<Boolean>{
        override fun provideDelegate(thisRef: Any?, prop: KProperty<*>): ReadWriteProperty<Any?, Boolean> = byMaterialFlag(prop.name)
    }
}