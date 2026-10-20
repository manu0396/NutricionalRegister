package com.example.nutricionalregister.data

import kotlinx.coroutines.isActive
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PlatformDataFactoriesTest {
    @Test
    fun createProductHttpClientBuildsAnActiveClientThatClosesCleanly() {
        // WHEN the platform HTTP client factory runs
        val client = createProductHttpClient()

        // THEN it produces an active client
        assertTrue(client.isActive)

        // AND closing it shuts the client down
        client.close()
        assertFalse(client.isActive)
    }
}
