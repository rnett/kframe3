package com.rnett.kframe.utils

import java.net.URLDecoder
import java.net.URLEncoder

actual fun urlParamEncode(url: String): String {
    return URLEncoder.encode(url, "UTF-8").replace("+", "%20")
}

actual fun urlParamDecode(url: String): String {
    return URLDecoder.decode(url, "UTF-8")
}