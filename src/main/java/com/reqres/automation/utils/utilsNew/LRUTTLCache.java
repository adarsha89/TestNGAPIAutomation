package com.reqres.automation.utils.utilsNew;

import java.util.LinkedHashMap;
import java.util.Map;

public class LRUTTLCache<K,V>{

    int capacity;
    Map<K, CacheEntry<V>> mapOfCaches;
    long ttlMillis;


    public LRUTTLCache(int capacity,long ttlMillis){
        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity must be > 0, was: " + capacity);
        }
        if (ttlMillis <= 0) {
            throw new IllegalArgumentException("ttlMillis must be > 0, was: " + ttlMillis);
        }
        this.capacity = capacity;
        this.ttlMillis = ttlMillis;
        mapOfCaches = new  LinkedHashMap<>(16, 0.75f, true){
            @Override
            protected boolean removeEldestEntry(Map.Entry<K,CacheEntry<V>> eldest) {
                return false;
            }
        };
    }

    public synchronized V get(K key){
        CacheEntry<V> cacheEntry = mapOfCaches.get(key);
        if(cacheEntry == null){
            return null;
        }
        if(cacheEntry.isExpired()){
            mapOfCaches.remove(key);
            return null;
        }
        return cacheEntry.value();
    }

    public synchronized void put(K key, V value){
        mapOfCaches.put(key, new CacheEntry<V>(value, System.currentTimeMillis() + ttlMillis));
    }

    class CacheEntry<V>{
        private final V value;
        private final long expiresAt;

        private CacheEntry(V value, Long expiresAt){
            this.value = value;
            this.expiresAt = expiresAt;
        }

        private V value(){return value;}
        private boolean isExpired(){
            return expiresAt < System.currentTimeMillis();
        }

    }

    public static void main(String[] args) {

    }
}
