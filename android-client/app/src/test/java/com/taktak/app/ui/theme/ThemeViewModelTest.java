package com.taktak.app.ui.theme;

import androidx.arch.core.executor.ArchTaskExecutor;
import androidx.arch.core.executor.TaskExecutor;
import java.io.IOException;
import java.util.ArrayDeque;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class ThemeViewModelTest {
    private final ArrayDeque<Runnable> queue = new ArrayDeque<>();
    private final MemoryStore store = new MemoryStore();
    private ThemeViewModel model;
    @Before public void setup() {
        ArchTaskExecutor.getInstance().setDelegate(new TaskExecutor() {
            @Override public void executeOnDiskIO(Runnable runnable) { runnable.run(); }
            @Override public void postToMainThread(Runnable runnable) { runnable.run(); }
            @Override public boolean isMainThread() { return true; }
        });
        model = new ThemeViewModel(new ThemeRepository(store), store.selection, queue::add, Runnable::run);
    }
    @After public void cleanup() { model.onCleared(); ArchTaskExecutor.getInstance().setDelegate(null); }

    @Test public void selectingPreviewDoesNotWrite() {
        model.select(AppTheme.GLASS, ColorMode.DARK);
        assertTrue(model.state().getValue().dirty());
        assertEquals(ThemeSelection.defaults(), store.selection);
        assertEquals(0, store.writes);
    }
    @Test public void onlySuccessfulApplyPublishesSavedRevision() {
        model.select(AppTheme.SOFT, ColorMode.LIGHT);
        model.apply();
        assertTrue(model.state().getValue().busy());
        assertEquals(0, store.writes);
        queue.remove().run();
        var state = model.state().getValue();
        assertTrue(state.applied()); assertFalse(state.error()); assertFalse(state.dirty());
        assertEquals(new ThemeSelection(AppTheme.SOFT, ColorMode.LIGHT, 1), state.saved());
    }
    @Test public void doubleApplyAndSelectionDuringSaveAreIgnored() {
        model.select(AppTheme.GRADIENT, ColorMode.SYSTEM);
        model.apply(); model.apply(); model.select(AppTheme.GLASS, ColorMode.DARK);
        assertEquals(1, queue.size());
        queue.remove().run();
        assertEquals(1, store.writes);
        assertEquals(AppTheme.GRADIENT, store.selection.style());
    }
    @Test public void failureKeepsSavedAndDraftAndCanRetry() {
        store.fail = true;
        model.select(AppTheme.GLASS, ColorMode.DARK); model.apply(); queue.remove().run();
        var failed = model.state().getValue();
        assertTrue(failed.error()); assertFalse(failed.applied()); assertFalse(failed.busy());
        assertEquals(ThemeSelection.defaults(), failed.saved());
        assertEquals(AppTheme.GLASS, failed.style()); assertEquals(ColorMode.DARK, failed.mode());
        store.fail = false; model.apply(); queue.remove().run();
        assertTrue(model.state().getValue().applied()); assertEquals(1, store.selection.revision());
    }
    @Test public void unchangedApplyDoesNotScheduleWrite() {
        model.apply(); assertTrue(queue.isEmpty()); assertEquals(0, store.writes);
    }
    @Test public void reselectingSavedClearsDraftWithoutWrite() {
        model.select(AppTheme.SOFT, ColorMode.DARK);
        model.select(AppTheme.ORIGINAL, ColorMode.SYSTEM);
        assertFalse(model.state().getValue().dirty()); assertEquals(0, store.writes);
    }
    @Test public void clearedModelDoesNotPublishLateCompletion() {
        model.select(AppTheme.GLASS, ColorMode.LIGHT); model.apply();
        var before = model.state().getValue(); model.onCleared(); queue.remove().run();
        assertSame(before, model.state().getValue());
        // Commit đã bắt đầu vẫn có thể hoàn thành; không giả đã hủy giao dịch disk.
        assertEquals(1, store.writes);
    }
    @Test public void chooserResourcesAreDistinctWithoutChangingBusinessFallback() {
        var resources = new java.util.HashSet<Integer>();
        for (AppTheme style : AppTheme.values()) {
            resources.add(ThemeManager.chooserThemeResource(style));
            assertEquals(style, ThemeManager.resolve(new ThemeSelection(style, ColorMode.DARK, 1),
                    ThemeManager.ScreenRole.MAIN).effectiveStyle());
        }
        assertEquals(4, resources.size());
    }
    private static final class MemoryStore implements ThemeStore {
        ThemeSelection selection = ThemeSelection.defaults();
        boolean fail;
        int writes;
        public ThemeSelection load() { return selection; }
        public ThemeSelection save(AppTheme style, ColorMode mode) throws IOException {
            if (fail) { throw new IOException("QA disk failure"); }
            writes++; selection = new ThemeSelection(style, mode, selection.revision() + 1); return selection;
        }
    }
}
