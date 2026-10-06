package com.majidbahmani.cesto.core.logging

/**
 * Minimal warning log: Logcat on Android, stdout (Xcode console) on iOS.
 * Never pass email content, receipt data or tokens; exception types and Gmail error codes are fine.
 */
expect fun logWarning(tag: String, message: String, throwable: Throwable? = null)
