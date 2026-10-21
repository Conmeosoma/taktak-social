package com.taktak.app.ui.history;

import static org.junit.Assert.*;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.Test;

public class SessionImageCacheTest {
    private final AtomicLong clock = new AtomicLong(100);
    private SessionImageCache<String> cache() { return new SessionImageCache<>(6, 10, clock::get, String::length); }

    @Test public void accountGenerationNeverSharesAnImage() {
        var cache = cache(); cache.put("moment", 1, "one");
        assertNull(cache.get("moment", 2)); assertEquals("one", cache.get("moment", 1));
        cache.put("moment", 2, "two"); assertNull(cache.get("moment", 1));
        assertEquals("two", cache.get("moment", 2));
    }
    @Test public void ttlExpiresAtBoundaryWithoutSlidingOnRead() {
        var cache = cache(); cache.put("moment", 1, "one");
        clock.set(109); assertEquals("one", cache.get("moment", 1));
        clock.set(110); assertNull(cache.get("moment", 1));
        cache.put("moment", 1, "new"); assertEquals("new", cache.get("moment", 1));
    }
    @Test public void revokeAndLogoutDropOnlyExpectedEntries() {
        var cache = cache(); cache.put("revoked", 1, "old"); cache.put("fresh", 1, "new");
        cache.remove("revoked"); assertNull(cache.get("revoked", 1));
        assertEquals("new", cache.get("fresh", 1)); cache.clear(); assertNull(cache.get("fresh", 1));
    }
    @Test public void lruAndOversizedEntriesRespectMemoryBound() {
        var cache = cache(); cache.put("a", 1, "aa"); cache.put("b", 1, "bb"); cache.put("c", 1, "cc");
        assertNotNull(cache.get("a", 1)); cache.put("d", 1, "dd");
        assertNull(cache.get("b", 1)); assertNotNull(cache.get("a", 1));
        cache.put("large", 1, "toolarge"); assertNull(cache.get("large", 1));
    }
    @Test public void clockRegressionFailsClosedButFreshWriteStillWorks() {
        var cache = cache(); cache.put("a", 1, "old"); clock.set(99); assertNull(cache.get("a", 1));
        cache.put("a", 1, "new"); assertEquals("new", cache.get("a", 1));
    }
}
