package com.traveler.traveler_api.cache;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Simple thread-safe LRU (Least Recently Used) cache.
 * LinkedHashMap with accessOrder = true keeps entries ordered from the least
 * to the most recently used; when the size limit is exceeded, the least
 * recently used entry is removed automatically.
 */
public class LruCache<K, V> {

    private final int maxSize;
    private final LinkedHashMap<K, V> map;

    public LruCache(int maxSize){
        this.maxSize = maxSize;
        this.map = new LinkedHashMap<>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<K, V> eldest){
                return size() > LruCache.this.maxSize;
            }
        };
    }

    // get() must be synchronized too: with accessOrder = true it moves the entry to the end
    public synchronized V get(K key){
        return map.get(key);
    }

    public synchronized void put(K key, V value){
        map.put(key, value);
    }

    public synchronized void remove(K key){
        map.remove(key);
    }

    public synchronized int size(){
        return map.size();
    }

    public int getMaxSize(){
        return maxSize;
    }
}