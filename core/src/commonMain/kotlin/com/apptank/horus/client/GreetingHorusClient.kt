package com.apptank.horus.client

class GreetingHorusClient {
    private val platform: Platform = getPlatform()

    fun greet(): String {
        return "Hello, ${platform.name}!"
    }
}