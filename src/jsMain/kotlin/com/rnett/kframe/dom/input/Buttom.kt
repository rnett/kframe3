package com.rnett.kframe.dom.input

import com.rnett.kframe.dom.core.DisplayElementHost
import com.rnett.kframe.dom.core.KFrameDSL
import com.rnett.kframe.dom.core.element

@KFrameDSL
inline val DisplayElementHost.button inline get() = element("button")