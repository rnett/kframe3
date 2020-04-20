package com.rnett.kframe.dom.providers

interface Attachable<P> {
    fun attach(provider: P)
    fun detach()
}

interface ExistenceAttachable: Attachable<RealizedExistenceProvider>

interface SingleProvider<S>{
    fun copyFrom(other: S)
}
internal interface VirtualProvider  {
}

internal interface RealizedProvider  {
}

