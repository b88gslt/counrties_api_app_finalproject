package com.example.hm_third_count.data.cache

import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test

/**
 * Чистая бизнес-логика — без Android, без корутин, без Hilt.
 * Покрывает граничные случаи TTL и принятие решений stale-while-revalidate.
 */
class CachePolicyTest {

    private lateinit var policy: CachePolicy

    @Before
    fun setup() {
        policy = CachePolicy()
    }

    // ---- isStale ----

    @Test
    fun `isStale returns false when cache is fresh`() {
        val now = 1_000_000L
        val fetched = now - HOUR_MS
        assertThat(policy.isStale(fetched, now, ttlHours = 6)).isFalse()
    }

    @Test
    fun `isStale returns true when cache is older than ttl`() {
        val now = 1_000_000L
        val fetched = now - 7 * HOUR_MS
        assertThat(policy.isStale(fetched, now, ttlHours = 6)).isTrue()
    }

    @Test
    fun `isStale at exact ttl boundary is stale`() {
        val now = 1_000_000L
        val fetched = now - 6 * HOUR_MS
        // Граница: ageMs == ttlMs → возвращает true (>=)
        assertThat(policy.isStale(fetched, now, ttlHours = 6)).isTrue()
    }

    @Test
    fun `isStale handles clock-from-future gracefully`() {
        // fetched > now (часы скакнули назад) — не считаем как stale.
        val now = 1_000_000L
        val fetched = now + HOUR_MS
        assertThat(policy.isStale(fetched, now, ttlHours = 6)).isFalse()
    }

    @Test
    fun `isStale with zero ttl is always stale`() {
        val now = 1_000_000L
        assertThat(policy.isStale(now, now, ttlHours = 0)).isTrue()
    }

    // ---- strategy ----

    @Test
    fun `strategy with no cache returns FetchOnly`() {
        val s = policy.strategy(cachedAt = null, now = 1_000_000L, ttlHours = 6)
        assertThat(s).isEqualTo(CacheStrategy.FetchOnly)
    }

    @Test
    fun `strategy with fresh cache returns UseCacheOnly`() {
        val now = 1_000_000L
        val s = policy.strategy(cachedAt = now - HOUR_MS, now = now, ttlHours = 6)
        assertThat(s).isEqualTo(CacheStrategy.UseCacheOnly)
    }

    @Test
    fun `strategy with stale cache returns UseCacheAndRevalidate`() {
        val now = 1_000_000L
        val s = policy.strategy(cachedAt = now - 10 * HOUR_MS, now = now, ttlHours = 6)
        assertThat(s).isEqualTo(CacheStrategy.UseCacheAndRevalidate)
    }

    @Test
    fun `strategy with forceRefresh always revalidates even if cache fresh`() {
        val now = 1_000_000L
        val s = policy.strategy(
            cachedAt = now - HOUR_MS,
            now = now,
            ttlHours = 6,
            forceRefresh = true
        )
        assertThat(s).isEqualTo(CacheStrategy.UseCacheAndRevalidate)
    }

    @Test
    fun `strategy with forceRefresh and no cache is still FetchOnly`() {
        // forceRefresh не превращает «нет кэша» в «использовать кэш».
        val s = policy.strategy(cachedAt = null, now = 1_000_000L, ttlHours = 6, forceRefresh = true)
        assertThat(s).isEqualTo(CacheStrategy.FetchOnly)
    }

    companion object {
        private const val HOUR_MS = 60L * 60L * 1000L
    }
}
