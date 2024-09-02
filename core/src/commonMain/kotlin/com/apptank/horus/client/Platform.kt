package com.apptank.horus.client

import com.apptank.horus.client.interfaces.IDatabaseDriverFactory

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform

expect fun getPlatformDatabaseDriverFactory(): IDatabaseDriverFactory