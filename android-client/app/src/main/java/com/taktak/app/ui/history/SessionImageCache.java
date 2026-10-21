package com.taktak.app.ui.history;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.LongSupplier;
import java.util.function.ToIntFunction;

/** RAM only; callers must obtain a fresh server grant before every lookup. */
final class SessionImageCache<T> {
    private static final class Entry<T> {
        final long generation, created;
        final int weight;
        final T value;
        Entry(long generation, long created, int weight, T value) {
            this.generation = generation; this.created = created; this.weight = weight; this.value = value;
        }
    }
    private final Map<String, Entry<T>> entries = new LinkedHashMap<>(16, .75f, true);
    private final long ttl;
    private final int limit;
    private final LongSupplier clock;
    private final ToIntFunction<T> weigh;
    private int size;

    SessionImageCache(int limit, long ttl, LongSupplier clock, ToIntFunction<T> weigh) {
        if (limit <= 0 || ttl <= 0) { throw new IllegalArgumentException("Invalid cache bounds"); }
        this.limit = limit; this.ttl = ttl; this.clock = clock; this.weigh = weigh;
    }
    synchronized T get(String key, long generation) {
        purge();
        Entry<T> entry = entries.get(key);
        return entry != null && entry.generation == generation ? entry.value : null;
    }
    synchronized void put(String key, long generation, T value) {
        if (key == null || value == null) { return; }
        purge(); remove(key);
        int weight = Math.max(1, weigh.applyAsInt(value));
        if (weight > limit) { return; }
        entries.put(key, new Entry<>(generation, clock.getAsLong(), weight, value)); size += weight;
        Iterator<Entry<T>> iterator = entries.values().iterator();
        while (size > limit && iterator.hasNext()) { size -= iterator.next().weight; iterator.remove(); }
    }
    synchronized void remove(String key) {
        Entry<T> removed = entries.remove(key);
        if (removed != null) { size -= removed.weight; }
    }
    synchronized void clear() { entries.clear(); size = 0; }
    private void purge() {
        long now = clock.getAsLong();
        Iterator<Entry<T>> iterator = entries.values().iterator();
        while (iterator.hasNext()) {
            Entry<T> entry = iterator.next();
            if (now < entry.created || now - entry.created >= ttl) {
                size -= entry.weight; iterator.remove();
            }
        }
    }
}
