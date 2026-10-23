package com.taktak.app.ui.splash;

import org.junit.Test;
import static org.junit.Assert.*;

public class IntroPolicyTest {
    @Test public void fullIntroOnlyOncePerProcess() {
        var policy = new IntroPolicy();
        assertTrue(policy.consume(false, true, false, true, true));
        assertFalse(policy.consume(false, true, false, true, true));
    }
    @Test public void restorationNeverReplays() { assertFalse(new IntroPolicy().consume(true, true, false, true, true)); }
    @Test public void quickStartSkips() { assertFalse(new IntroPolicy().consume(false, true, false, false, true)); }
    @Test public void disabledMotionSkips() { assertFalse(new IntroPolicy().consume(false, true, false, true, false)); }
    @Test public void notificationConsumesOpportunityWithoutBlockingDestination() {
        var policy = new IntroPolicy();
        assertFalse(policy.consume(false, false, true, true, true));
        assertFalse(policy.consume(false, true, false, true, true));
    }
    @Test public void specialLauncherExtrasSkip() { assertFalse(new IntroPolicy().consume(false, true, true, true, true)); }
    @Test public void soundRequiresEverySafetyCondition() {
        assertTrue(IntroPolicy.audible(true, true, true, true, false, false));
        assertFalse(IntroPolicy.audible(false, true, true, true, false, false));
        assertFalse(IntroPolicy.audible(true, false, true, true, false, false));
        assertFalse(IntroPolicy.audible(true, true, false, true, false, false));
        assertFalse(IntroPolicy.audible(true, true, true, false, false, false));
        assertFalse(IntroPolicy.audible(true, true, true, true, true, false));
        assertFalse(IntroPolicy.audible(true, true, true, true, false, true));
    }
    @Test public void fitNeverCropsAndKeepsSourceAspect() {
        assertEquals(1.5f, IntroPolicy.fitScale(1080, 2400, 720, 1280), .0001f);
        assertEquals(.84375f, IntroPolicy.fitScale(2400, 1080, 720, 1280), .0001f);
        assertEquals(0, IntroPolicy.fitScale(0, 1080, 720, 1280), 0);
    }
}
