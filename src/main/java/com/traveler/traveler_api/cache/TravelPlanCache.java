package com.traveler.traveler_api.cache;

import com.traveler.traveler_api.dto.TravelPlanResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

/**
 * LRU cache for GET /api/travel-plans/{id} responses with invalidation.
 * One instance is shared by all request threads of the application.
 */
@Component
public class TravelPlanCache {

    private final boolean enabled;
    private final LruCache<UUID, TravelPlanResponse> cache;

    private final Object lock = new Object();
    private long invalidations = 0; // guarded by lock

    private final AtomicLong hits = new AtomicLong();
    private final AtomicLong misses = new AtomicLong();

    public TravelPlanCache(@Value("${app.cache.plans.enabled:true}") boolean enabled,
                           @Value("${app.cache.plans.size:1000}") int maxSize){
        this.enabled = enabled;
        this.cache = new LruCache<>(maxSize);
    }

    public TravelPlanResponse get(UUID id){
        if (!enabled) {
            return null;
        }
        TravelPlanResponse value = cache.get(id);
        if (value != null) {
            hits.incrementAndGet();
        } else {
            misses.incrementAndGet();
        }
        return value;
    }

    // Taken BEFORE reading from the DB
    public long currentStamp(){
        synchronized (lock) {
            return invalidations;
        }
    }

    // Put the value only if nothing was invalidated while we were reading from the DB,
    // otherwise the value may already be outdated
    public void putIfUnchanged(UUID id, TravelPlanResponse value, long stamp){
        if (!enabled) {
            return;
        }
        synchronized (lock) {
            if (invalidations == stamp) {
                cache.put(id, value);
            }
        }
    }

    // Remove the plan from the cache only after the transaction is committed,
    // so other threads cannot re-cache the old data in between
    public void evictAfterCommit(UUID id){
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit(){
                    evict(id);
                }
            });
        } else {
            evict(id);
        }
    }

    public void evict(UUID id){
        synchronized (lock) {
            invalidations++;
            cache.remove(id);
        }
    }

    public Map<String, Object> stats(){
        long hitCount = hits.get();
        long missCount = misses.get();
        long total = hitCount + missCount;
        double hitRate = 0.0;
        if (total > 0) {
            hitRate = Math.round(hitCount * 10000.0 / total) / 100.0;
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("enabled", enabled);
        result.put("max_size", cache.getMaxSize());
        result.put("size", cache.size());
        result.put("hits", hitCount);
        result.put("misses", missCount);
        result.put("hit_rate_percent", hitRate);
        synchronized (lock) {
            result.put("invalidations", invalidations);
        }
        return result;
    }
}