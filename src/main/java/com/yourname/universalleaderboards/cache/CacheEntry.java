package com.yourname.universalleaderboards.cache;

public final class CacheEntry<T> {

    private final T value;
    private final long cachedAt;

    public CacheEntry(T value) {
        this.value = value;
        this.cachedAt = System.currentTimeMillis();
    }

    public T getValue() { return value; }

    public boolean isExpired(long ttlMillis) {
        return System.currentTimeMillis() - cachedAt > ttlMillis;
    }
}
