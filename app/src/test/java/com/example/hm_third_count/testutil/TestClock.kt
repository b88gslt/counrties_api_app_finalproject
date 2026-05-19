package com.example.hm_third_count.testutil

import com.example.hm_third_count.data.repository.Clock

class TestClock(initial: Long = 1_000_000_000L) : Clock {
    var now: Long = initial
    override fun nowMillis(): Long = now
    fun advance(deltaMs: Long) { now += deltaMs }
}
