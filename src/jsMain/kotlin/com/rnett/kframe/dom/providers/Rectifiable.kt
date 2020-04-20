package com.rnett.kframe.dom.providers

interface Rectifiable<in S: Rectifiable<S>>{
    /**
     * Make this match source
     */
    fun rectify(source: S)
}