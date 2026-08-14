package com.plugins.shortcuts;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.apache.cordova.PluginResult;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import com.plugins.shortcuts.support.PluginTestHarness;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class ShortcutsPluginExecuteTest {

    private static void assertOkBoolean(PluginTestHarness.ExecuteResult result, boolean expected) {
        assertTrue("execute() should report the action as handled", result.handled);
        assertEquals(1, result.results.size());
        PluginResult pluginResult = result.only();
        assertEquals(PluginResult.Status.OK.ordinal(), pluginResult.getStatus());
        assertEquals(Boolean.toString(expected), pluginResult.getMessage());
    }

    @Test
    public void supportsDynamic_returnsTrue() {
        PluginTestHarness harness = new PluginTestHarness();

        assertOkBoolean(harness.execute("supportsDynamic", "[]"), true);
    }

    @Test
    @Config(sdk = 26)
    public void supportsDynamic_returnsTrueOnSdk26() {
        PluginTestHarness harness = new PluginTestHarness();

        assertOkBoolean(harness.execute("supportsDynamic", "[]"), true);
    }

    /** Exact boundary of the {@code SDK_INT >= 25} check — catches a tightening to {@code >= 26}. */
    @Test
    @Config(sdk = 25)
    public void supportsDynamic_returnsTrueOnSdk25Boundary() {
        PluginTestHarness harness = new PluginTestHarness();

        assertOkBoolean(harness.execute("supportsDynamic", "[]"), true);
    }

    @Test
    @Config(sdk = 24)
    public void supportsDynamic_returnsFalseBelowSdk25() {
        PluginTestHarness harness = new PluginTestHarness();

        assertOkBoolean(harness.execute("supportsDynamic", "[]"), false);
    }

    @Test
    public void supportsPinned_returnsTrueWhenLauncherSupportsPinning() {
        PluginTestHarness harness = new PluginTestHarness();
        harness.shadowShortcutManager().setIsRequestPinShortcutSupported(true);

        assertOkBoolean(harness.execute("supportsPinned", "[]"), true);
    }

    @Test
    public void supportsPinned_returnsFalseWhenLauncherDoesNotSupportPinning() {
        PluginTestHarness harness = new PluginTestHarness();
        harness.shadowShortcutManager().setIsRequestPinShortcutSupported(false);

        assertOkBoolean(harness.execute("supportsPinned", "[]"), false);
    }

    @Test
    public void unknownAction_isNotHandledAndSendsNothing() {
        PluginTestHarness harness = new PluginTestHarness();

        PluginTestHarness.ExecuteResult result = harness.execute("noSuchAction", "[]");

        assertFalse("execute() should report an unknown action as unhandled", result.handled);
        assertTrue("an unknown action must not send any result", result.results.isEmpty());
    }

    @Test
    public void malformedArgs_surfaceTheExceptionMessageAsAnError() {
        PluginTestHarness harness = new PluginTestHarness();

        PluginTestHarness.ExecuteResult result = harness.execute("setDynamic", "[{}]");

        assertTrue("execute() handles the action even when it fails", result.handled);
        PluginResult pluginResult = result.only();
        assertEquals(PluginResult.Status.ERROR.ordinal(), pluginResult.getStatus());
        assertEquals("A value for 'id' is required", pluginResult.getStrMessage());
    }
}
