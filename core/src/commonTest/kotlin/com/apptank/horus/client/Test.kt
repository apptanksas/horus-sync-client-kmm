package com.apptank.horus.client

import kotlin.test.Test
import kotlin.test.assertTrue

class CommonGreetingTest {

    @Test
    fun testExample() {
        assertTrue(GreetingHorusClient().greet().contains("Hello"), "Check 'Hello' is mentioned")
    }
}