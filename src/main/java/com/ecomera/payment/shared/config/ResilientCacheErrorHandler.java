package com.ecomera.payment.shared.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.interceptor.CacheErrorHandler;

/**
 * Treats cache failures (e.g. Redis unreachable) as cache misses: the request
 * falls through to the source of truth instead of failing.
 */
@Slf4j
public class ResilientCacheErrorHandler implements CacheErrorHandler {

    @Override
    public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
        log.warn("Cache GET failed for cache '{}' key '{}' — falling back to source. Reason: {}",
                cacheName(cache), key, exception.getMessage());
    }

    @Override
    public void handleCachePutError(RuntimeException exception, Cache cache, Object key, Object value) {
        log.warn("Cache PUT failed for cache '{}' key '{}' — continuing without caching. Reason: {}",
                cacheName(cache), key, exception.getMessage());
    }

    @Override
    public void handleCacheEvictError(RuntimeException exception, Cache cache, Object key) {
        log.warn("Cache EVICT failed for cache '{}' key '{}' — continuing. Reason: {}",
                cacheName(cache), key, exception.getMessage());
    }

    @Override
    public void handleCacheClearError(RuntimeException exception, Cache cache) {
        log.warn("Cache CLEAR failed for cache '{}' — continuing. Reason: {}",
                cacheName(cache), exception.getMessage());
    }

    private String cacheName(Cache cache) {
        return cache != null ? cache.getName() : "unknown";
    }
}
