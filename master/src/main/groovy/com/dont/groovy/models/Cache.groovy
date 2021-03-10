package com.dont.groovy.models

import java.util.function.Function

class Cache<K, V> {

    private final Map<K, V> cache = [:]
    private final Function<V, K> extractor
    private final Class<V> vClazz

    Cache(vClazz, extractor) {
        this.vClazz = vClazz
        this.extractor = extractor
    }

    void cache(V value) {
        cache(value, false)
    }

    void cache(V value, boolean flush) {
        def key = extractor.apply(value)
        if (isCached(key)) return
        if (flush) value.save()
        this.cache.put(key, value)
    }

    void uncache(K key) {
        this.cache.remove(key)
    }

    V getCached(K key) {
        return this.cache.get(key)
    }

    V getOrCache(K key, V value) {
        if (this.isCached(key)) return getCached(key)
        cache(value)
        return value
    }

    boolean isCached(K key) {
        return this.cache.containsKey(key)
    }

    int saveCached() {
        cache.values().each { it.save() }
        return cache.size()
    }

    Collection<V> getCached() {
        return cache.values()
    }

    Collection<V> getNonCached() {
        Collection<V> all = vClazz.metaClass.pickMethod("findAll").invoke(null)
        all.removeIf({ value -> cache.containsKey(extractor.apply(value)) })
        return all
    }

    Collection<V> getAll() {
        return getCached() + getNonCached()
    }

    int loadAll() {
        def nonCached = getNonCached()
        nonCached.each { cache(it) }
        return nonCached.size()
    }


    @Override
    public String toString() {
        return "Cache{" +
                "cache=" + cache +
                '}';
    }
}
