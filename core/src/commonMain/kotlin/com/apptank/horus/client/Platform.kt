package com.apptank.horus.client

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform