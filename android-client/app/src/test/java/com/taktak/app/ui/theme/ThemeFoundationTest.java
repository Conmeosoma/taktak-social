package com.taktak.app.ui.theme;

import android.content.SharedPreferences;
import androidx.appcompat.app.AppCompatDelegate;
import com.taktak.app.R;
import java.io.IOException;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import org.junit.Test;
import static org.junit.Assert.*;

public class ThemeFoundationTest {
    @Test public void stableIdsAndUnknownFallback() {
        assertArrayEquals(new String[]{"original", "gradient", "glass", "soft"},
                java.util.Arrays.stream(AppTheme.values()).map(AppTheme::id).toArray(String[]::new));
        for (AppTheme theme : AppTheme.values()) { assertEquals(theme, AppTheme.fromId(theme.id())); }
        for (ColorMode mode : ColorMode.values()) { assertEquals(mode, ColorMode.fromId(mode.id())); }
        for (String unknown : new String[]{null, "", "GLASS", "future", "0"}) {
            assertEquals(AppTheme.ORIGINAL, AppTheme.fromId(unknown));
            assertEquals(ColorMode.SYSTEM, ColorMode.fromId(unknown));
        }
    }

    @Test public void resolverMatrixExplicitlyFallsBackWithoutLosingRequestedSelection() {
        for (AppTheme theme : AppTheme.values()) {
            for (ColorMode mode : ColorMode.values()) {
                ThemeSelection requested = new ThemeSelection(theme, mode, 42);
                for (ThemeManager.ScreenRole role : ThemeManager.ScreenRole.values()) {
                    var resolution = ThemeManager.resolve(requested, role);
                    assertEquals(requested, resolution.requested());
                    boolean login = role == ThemeManager.ScreenRole.LOGIN;
                    boolean auth = role == ThemeManager.ScreenRole.AUTH;
                    assertEquals(theme, resolution.effectiveStyle());
                    assertTrue(resolution.requestedStyleAvailable());
                    int resource = auth ? switch (theme) {
                        case ORIGINAL -> R.style.Theme_TakTak_Auth;
                        case GRADIENT -> R.style.Theme_TakTak_Auth_Gradient;
                        case GLASS -> R.style.Theme_TakTak_Auth_Glass;
                        case SOFT -> R.style.Theme_TakTak_Auth_Soft;
                    } : login ? switch (theme) {
                        case ORIGINAL -> com.taktak.app.R.style.Theme_TakTak_LoginReference;
                        case GRADIENT -> com.taktak.app.R.style.Theme_TakTak_Login_Gradient;
                        case GLASS -> com.taktak.app.R.style.Theme_TakTak_Login_Glass;
                        case SOFT -> com.taktak.app.R.style.Theme_TakTak_Login_Soft;
                    } : mediaResource(theme, role);
                    assertEquals(resource, resolution.themeResource());
                }
            }
        }
    }

    @Test public void modeRemainsIndependentAndNullSelectionUsesDefaults() {
        assertEquals(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM, ThemeManager.nightMode(ColorMode.SYSTEM));
        assertEquals(AppCompatDelegate.MODE_NIGHT_NO, ThemeManager.nightMode(ColorMode.LIGHT));
        assertEquals(AppCompatDelegate.MODE_NIGHT_YES, ThemeManager.nightMode(ColorMode.DARK));
        assertEquals(ThemeSelection.defaults(), ThemeManager.resolve(null, ThemeManager.ScreenRole.LOGIN).requested());
    }

    @Test public void selectionRejectsNullAndNegativeRevision() {
        assertThrows(NullPointerException.class, () -> new ThemeSelection(null, ColorMode.SYSTEM, 0));
        assertThrows(NullPointerException.class, () -> new ThemeSelection(AppTheme.ORIGINAL, null, 0));
        assertThrows(IllegalArgumentException.class, () -> new ThemeSelection(AppTheme.ORIGINAL, ColorMode.SYSTEM, -1));
    }

    @Test public void emptyReadIsSideEffectFree() {
        FakePreferences fake = new FakePreferences();
        assertEquals(ThemeSelection.defaults(), new ThemePreferences(fake.preferences).load());
        assertTrue(fake.values.isEmpty());
        assertEquals(0, fake.commits);
    }

    @Test public void everySelectionPersistsAcrossNewRepositoryInstances() throws Exception {
        FakePreferences fake = new FakePreferences();
        ThemeRepository repository = new ThemeRepository(new ThemePreferences(fake.preferences));
        for (AppTheme theme : AppTheme.values()) {
            for (ColorMode mode : ColorMode.values()) {
                ThemeSelection saved = repository.save(theme, mode);
                assertEquals(saved, new ThemeRepository(new ThemePreferences(fake.preferences)).load());
                assertEquals(theme, saved.style());
                assertEquals(mode, saved.colorMode());
            }
        }
    }

    @Test public void noOpDoesNotWriteOrIncrementRevision() throws Exception {
        FakePreferences fake = new FakePreferences();
        ThemePreferences store = new ThemePreferences(fake.preferences);
        assertEquals(ThemeSelection.defaults(), store.save(AppTheme.ORIGINAL, ColorMode.SYSTEM));
        assertEquals(0, fake.commits);
        ThemeSelection saved = store.save(AppTheme.GLASS, ColorMode.DARK);
        assertEquals(1, saved.revision());
        assertEquals(saved, store.save(AppTheme.GLASS, ColorMode.DARK));
        assertEquals(1, fake.commits);
        assertEquals(2, store.save(AppTheme.GLASS, ColorMode.LIGHT).revision());
    }

    @Test public void corruptAndFutureDataFallbackWithoutMutatingDisk() throws Exception {
        for (String key : new String[]{ThemePreferences.STYLE, ThemePreferences.MODE,
                ThemePreferences.SCHEMA, ThemePreferences.REVISION}) {
            for (Object invalid : new Object[]{true, "unknown", -1L, 2, new java.util.HashSet<String>()}) {
                FakePreferences fake = new FakePreferences();
                ThemePreferences store = new ThemePreferences(fake.preferences);
                store.save(AppTheme.SOFT, ColorMode.LIGHT);
                fake.values.put(key, invalid);
                Map<String, Object> before = new HashMap<>(fake.values);
                int commits = fake.commits;
                assertEquals(ThemeSelection.defaults(), store.load());
                assertEquals(before, fake.values);
                assertEquals(commits, fake.commits);
            }
        }
    }

    @Test public void newerSchemaIsNeverOverwritten() throws Exception {
        FakePreferences fake = new FakePreferences();
        fake.values.put(ThemePreferences.SCHEMA, 99);
        ThemePreferences store = new ThemePreferences(fake.preferences);
        assertThrows(IOException.class, () -> store.save(AppTheme.SOFT, ColorMode.DARK));
        assertEquals(99, fake.values.get(ThemePreferences.SCHEMA));
        assertEquals(0, fake.commits);
    }

    @Test public void failedCommitRestoresMemoryAndDoesNotPublishSuccess() throws Exception {
        FakePreferences fake = new FakePreferences();
        ThemeRepository repository = new ThemeRepository(new ThemePreferences(fake.preferences));
        ThemeSelection before = repository.save(AppTheme.GRADIENT, ColorMode.LIGHT);
        fake.values.put("unrelated", "keep");
        fake.failCommits = 1;
        assertThrows(IOException.class, () -> repository.save(AppTheme.GLASS, ColorMode.DARK));
        assertEquals(before, repository.load());
        assertEquals("keep", fake.values.get("unrelated"));
        assertEquals(before.revision() + 1, repository.save(AppTheme.GLASS, ColorMode.DARK).revision());
    }

    @Test public void initialFailureAndRollbackFailureAreReported() {
        FakePreferences fake = new FakePreferences();
        ThemePreferences store = new ThemePreferences(fake.preferences);
        fake.failCommits = 2;
        IOException error = assertThrows(IOException.class, () -> store.save(AppTheme.SOFT, ColorMode.LIGHT));
        assertTrue(error.getMessage().contains("rollback failed"));
        assertEquals(ThemeSelection.defaults(), store.load());
        assertTrue(fake.values.isEmpty());
    }

    @Test public void failedRepairPreservesCorruptTypedValuesAndUnrelatedKeys() {
        for (Object corrupt : new Object[]{false, 3.5f, java.util.Set.of("old")}) {
            FakePreferences fake = new FakePreferences();
            fake.values.put(ThemePreferences.STYLE, corrupt);
            fake.values.put("unrelated", "keep");
            var before = new HashMap<>(fake.values);
            fake.failCommits = 1;
            assertThrows(IOException.class,
                    () -> new ThemePreferences(fake.preferences).save(AppTheme.SOFT, ColorMode.DARK));
            assertEquals(before, fake.values);
        }
    }

    @Test public void revisionOverflowAndInvalidInputDoNotWrite() throws Exception {
        FakePreferences fake = new FakePreferences();
        ThemePreferences store = new ThemePreferences(fake.preferences);
        store.save(AppTheme.SOFT, ColorMode.LIGHT);
        fake.values.put(ThemePreferences.REVISION, Long.MAX_VALUE);
        int commits = fake.commits;
        assertThrows(IOException.class, () -> store.save(AppTheme.GLASS, ColorMode.DARK));
        assertThrows(NullPointerException.class, () -> store.save(null, ColorMode.DARK));
        assertEquals(commits, fake.commits);
    }

    @Test public void differentStoresSerializeConcurrentReadModifyWrite() throws Exception {
        FakePreferences fake = new FakePreferences();
        var first = new ThemePreferences(fake.preferences);
        var second = new ThemePreferences(fake.preferences);
        var executor = Executors.newFixedThreadPool(2);
        try {
            var one = executor.submit(() -> first.save(AppTheme.GLASS, ColorMode.DARK));
            var two = executor.submit(() -> second.save(AppTheme.SOFT, ColorMode.LIGHT));
            assertEquals(3, one.get().revision() + two.get().revision());
            assertEquals(2, first.load().revision());
            assertEquals(2, fake.commits);
        } finally { executor.shutdownNow(); }
    }

    private static int mediaResource(AppTheme style, ThemeManager.ScreenRole role) {
        int[] resources = switch (role) {
            case MAIN -> new int[]{R.style.Theme_TakTak_Media_Main, R.style.Theme_TakTak_Media_Main_Gradient,
                    R.style.Theme_TakTak_Media_Main_Glass, R.style.Theme_TakTak_Media_Main_Soft};
            case PREVIEW -> new int[]{R.style.Theme_TakTak_Media_Preview, R.style.Theme_TakTak_Media_Preview_Gradient,
                    R.style.Theme_TakTak_Media_Preview_Glass, R.style.Theme_TakTak_Media_Preview_Soft};
            case VIEWER -> new int[]{R.style.Theme_TakTak_Media_Viewer, R.style.Theme_TakTak_Media_Viewer_Gradient,
                    R.style.Theme_TakTak_Media_Viewer_Glass, R.style.Theme_TakTak_Media_Viewer_Soft};
            default -> throw new AssertionError(role);
        };
        return resources[style.ordinal()];
    }

    /** Mô phỏng commit cập nhật memory ngay cả khi ghi disk thất bại như Android. */
    private static final class FakePreferences {
        final Map<String, Object> values = new HashMap<>();
        int commits;
        int failCommits;
        final SharedPreferences preferences = (SharedPreferences) Proxy.newProxyInstance(
                SharedPreferences.class.getClassLoader(), new Class<?>[]{SharedPreferences.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getAll" -> new HashMap<>(values);
                    case "edit" -> editor();
                    default -> throw new AssertionError("Unexpected preferences method: " + method.getName());
                });

        private SharedPreferences.Editor editor() {
            Map<String, Object> pending = new HashMap<>();
            return (SharedPreferences.Editor) Proxy.newProxyInstance(SharedPreferences.Editor.class.getClassLoader(),
                    new Class<?>[]{SharedPreferences.Editor.class}, (proxy, method, args) -> {
                        if (method.getName().startsWith("put")) { pending.put((String) args[0], args[1]); return proxy; }
                        if (method.getName().equals("remove")) { pending.put((String) args[0], null); return proxy; }
                        if (method.getName().equals("commit")) {
                            pending.forEach((key, value) -> { if (value == null) { values.remove(key); } else { values.put(key, value); } });
                            commits++;
                            if (failCommits > 0) { failCommits--; return false; }
                            return true;
                        }
                        throw new AssertionError("Unexpected editor method: " + method.getName());
                    });
        }
    }
}
