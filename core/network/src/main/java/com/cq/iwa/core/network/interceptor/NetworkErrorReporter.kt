package com.cq.iwa.core.network.interceptor

fun interface NetworkErrorReporter {
    fun report(error: Throwable)
}
