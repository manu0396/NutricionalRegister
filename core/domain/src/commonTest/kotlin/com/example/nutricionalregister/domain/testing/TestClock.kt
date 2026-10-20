package com.example.nutricionalregister.domain.testing

import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler

internal class TestClock(
    var nowEpochMillis: Long = 1_000L,
) {
    fun advanceBy(millis: Long) {
        require(millis >= 0L)
        nowEpochMillis += millis
    }
}

internal class TestDispatcherProvider {
    val scheduler = TestCoroutineScheduler()
    val dispatcher = StandardTestDispatcher(scheduler)
}
