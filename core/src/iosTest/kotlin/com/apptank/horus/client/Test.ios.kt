package com.apptank.horus.client

import kotlin.test.Test
import kotlin.test.assertTrue

class IosGreetingTest {

    @Test
    fun testExample() {
        assertTrue(GreetingHorusClient().greet().contains("iOS"), "Check iOS is mentioned")
    }
}