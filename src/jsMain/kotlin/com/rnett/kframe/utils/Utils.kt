package com.rnett.kframe.utils

external fun encodeURIComponent(url: String): String
external fun decodeURIComponent(url: String): String

actual fun urlParamEncode(url: String): String {
    return encodeURIComponent(url)
}

actual fun urlParamDecode(url: String): String {
    return decodeURIComponent(url)
}