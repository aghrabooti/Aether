package com.cluvex.aether

object AetherNative {
    init { System.loadLibrary("aether_jni") }
    external fun start(argumentsJson: String): Long
    external fun poll(job: Long): String
    external fun cancel(job: Long)
}
