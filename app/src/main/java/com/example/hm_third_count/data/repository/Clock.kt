package com.example.hm_third_count.data.repository

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Тонкая обёртка над System.currentTimeMillis() — чтобы в тестах подменять время
 * и проверять TTL-логику детерминированно.
 */
fun interface Clock {
    fun nowMillis(): Long
}

@Singleton
class SystemClock @Inject constructor() : Clock {
    override fun nowMillis(): Long = System.currentTimeMillis()
}
