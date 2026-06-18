package com.example.hm_third_count.data.cache

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Решение о том, что делать с кэшем: использовать как есть, обновить в фоне или жёстко перезагрузить.
 * Чистая бизнес-логика — без Android-зависимостей, легко покрывается unit-тестами.
 */
sealed class CacheStrategy {
    /** Кэша нет — обязательно идти в сеть. */
    data object FetchOnly : CacheStrategy()

    /** Кэш свежий — отдаём как есть, сеть не дёргаем. */
    data object UseCacheOnly : CacheStrategy()

    /** Кэш протух или просто старый — отдаём пользователю сразу, параллельно обновляем. */
    data object UseCacheAndRevalidate : CacheStrategy()
}

@Singleton
class CachePolicy @Inject constructor() {

    /**
     * Протух ли кэш относительно TTL.
     * @param fetchedAt когда данные были положены в кэш (epoch millis)
     * @param now текущее время (epoch millis)
     * @param ttlHours TTL в часах из настроек
     */
    fun isStale(fetchedAt: Long, now: Long, ttlHours: Int): Boolean {
        if (ttlHours <= 0) return true
        val ageMs = now - fetchedAt
        if (ageMs < 0) return false // защита от часов "из будущего"
        val ttlMs = ttlHours.toLong() * 60L * 60L * 1000L
        return ageMs >= ttlMs
    }

    /**
     * Стратегия для конкретного запроса:
     *  - нет кэша  → FetchOnly
     *  - свежий    → UseCacheOnly
     *  - протух    → UseCacheAndRevalidate (stale-while-revalidate)
     *
     * @param forceRefresh пользователь нажал Refresh — игнорируем свежесть, всё равно идём в сеть
     */
    fun strategy(
        cachedAt: Long?,
        now: Long,
        ttlHours: Int,
        forceRefresh: Boolean = false
    ): CacheStrategy {
        if (cachedAt == null) return CacheStrategy.FetchOnly
        if (forceRefresh) return CacheStrategy.UseCacheAndRevalidate
        return if (isStale(cachedAt, now, ttlHours)) {
            CacheStrategy.UseCacheAndRevalidate
        } else {
            CacheStrategy.UseCacheOnly
        }
    }
}
