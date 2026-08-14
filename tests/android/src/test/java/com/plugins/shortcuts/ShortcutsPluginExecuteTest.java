package com.plugins.shortcuts;

import static org.junit.Assert.assertEquals;
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

    @Test
    public void supportsDynamic_returnsTrue() {
        PluginTestHarness harness = new PluginTestHarness();

        PluginTestHarness.ExecuteResult result = harness.execute("supportsDynamic", "[]");

        assertTrue("execute() should report the action as handled", result.handled);
        assertEquals(1, result.results.size());
        PluginResult pluginResult = result.only();
        assertEquals(PluginResult.Status.OK.ordinal(), pluginResult.getStatus());
        assertEquals("true", pluginResult.getMessage());
    }
}
