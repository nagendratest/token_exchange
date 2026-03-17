package de.telefonica.apigw.tokengenerator.cache;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;

public class InMemoryCache<K, V> {

  private static final int EXPIRED_SWEEP_LIMIT = 200;

  private final ConcurrentHashMap<K, CacheEntry<V>> store = new ConcurrentHashMap<>();
  private final ConcurrentLinkedQueue<CacheKeyVersion<K>> order = new ConcurrentLinkedQueue<>();
  private final AtomicLong sequence = new AtomicLong();
  private final long maxSize;
  private final long ttlMillis;

  public InMemoryCache(long maxSize, Duration ttl) {
    this.maxSize = maxSize;
    this.ttlMillis = ttl.toMillis();
  }

  public V getIfPresent(K key) {
    if (key == null) {
      return null;
    }
    CacheEntry<V> entry = store.get(key);
    if (entry == null) {
      return null;
    }
    long now = System.currentTimeMillis();
    if (entry.isExpired(now)) {
      store.remove(key, entry);
      return null;
    }
    return entry.value();
  }

  public void put(K key, V value) {
    if (key == null || value == null) {
      return;
    }
    if (maxSize <= 0 || ttlMillis <= 0) {
      return;
    }
    long now = System.currentTimeMillis();
    long version = sequence.incrementAndGet();
    CacheEntry<V> entry = new CacheEntry<>(value, now + ttlMillis, version);
    store.put(key, entry);
    order.add(new CacheKeyVersion<>(key, version));
    evictIfNeeded(now);
  }

  private void evictIfNeeded(long now) {
    if (store.size() <= maxSize) {
      return;
    }
    sweepExpired(now);
    while (store.size() > maxSize) {
      CacheKeyVersion<K> candidate = order.poll();
      if (candidate == null) {
        break;
      }
      CacheEntry<V> current = store.get(candidate.key());
      if (current != null && current.version() == candidate.version()) {
        store.remove(candidate.key(), current);
      }
    }
  }

  private void sweepExpired(long now) {
    int scanned = 0;
    for (Map.Entry<K, CacheEntry<V>> entry : store.entrySet()) {
      if (scanned >= EXPIRED_SWEEP_LIMIT) {
        break;
      }
      CacheEntry<V> candidate = entry.getValue();
      if (candidate.isExpired(now)) {
        store.remove(entry.getKey(), candidate);
      }
      scanned++;
    }
  }

  private static final class CacheEntry<V> {
    private final V value;
    private final long expiresAtMillis;
    private final long version;

    private CacheEntry(V value, long expiresAtMillis, long version) {
      this.value = value;
      this.expiresAtMillis = expiresAtMillis;
      this.version = version;
    }

    private V value() {
      return value;
    }

    private long version() {
      return version;
    }

    private boolean isExpired(long nowMillis) {
      return nowMillis >= expiresAtMillis;
    }
  }

  private static final class CacheKeyVersion<K> {
    private final K key;
    private final long version;

    private CacheKeyVersion(K key, long version) {
      this.key = key;
      this.version = version;
    }

    private K key() {
      return key;
    }

    private long version() {
      return version;
    }
  }
}
