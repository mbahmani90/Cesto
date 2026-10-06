package com.majidbahmani.cesto.core.logging

// println, not NSLog: calling the variadic NSLog from Kotlin/Native crashed the process (signal 11).
// stdout shows up in the Xcode console.
actual fun logWarning(tag: String, message: String, throwable: Throwable?) {
    val details = throwable?.let { " (${it::class.simpleName}: ${it.message})" }.orEmpty()
    println("W/$tag: $message$details")
}
