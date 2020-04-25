package com.rnett.kframe.dom.core

import com.rnett.kframe.dom.core.providers.ExistenceAttachable
import com.rnett.kframe.dom.core.providers.PropertyProviderWrapper
import com.rnett.kframe.dom.core.providers.RealizedExistenceProvider
import com.rnett.kframe.utils.StringDelegatable

class Properties internal constructor(element: Element<*>) : StringDelegatable(), ExistenceAttachable {

    internal val provider = PropertyProviderWrapper(element.provider.propertiesProvider())

    override fun attach(provider: RealizedExistenceProvider) {
        this.provider.attach(provider)
    }

    override fun detach() {
        provider.detach()
    }

    private val blacklist = setOf("class", "style")

    override fun getValue(key: String) =
        if (key in blacklist) error("Can't get $key from properties, use accessor") else provider.getValue(key)

    override fun setValue(key: String, value: String?) {
        if (key in blacklist)
            error("Can't set $key from properties, use accessor")

        provider.setValue(key, value)
    }
}
