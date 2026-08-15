package com.plugins.shortcuts;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;

import android.content.Intent;
import android.net.Uri;

import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import com.plugins.shortcuts.support.PluginTestHarness;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class ShortcutsPluginIntentsTest {

    private static final String LAUNCH_ACTION = "com.example.LAUNCH";
    private static final String LAUNCH_CATEGORY = "com.example.CATEGORY";
    private static final String LAUNCH_DATA = "https://example.com/path?q=1";
    private static final int LAUNCH_FLAGS =
        Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP;

    /** A launch intent exercising every field and every extra type buildIntent maps. */
    private static Intent fullLaunchIntent() {
        Intent intent = new Intent(LAUNCH_ACTION, Uri.parse(LAUNCH_DATA));
        intent.addCategory(LAUNCH_CATEGORY);
        intent.setFlags(LAUNCH_FLAGS);
        intent.putExtra("aBoolean", true);
        intent.putExtra("anInteger", 42);
        intent.putExtra("aLong", 4294967296L);
        intent.putExtra("aFloat", 1.5f);
        intent.putExtra("aDouble", 2.5d);
        intent.putExtra("aString", "hello");
        intent.putExtra("shortcut", "reserved-exact");
        intent.putExtra("shortcut.foo", "reserved-namespaced");
        intent.putExtra("shortcutty", "not-reserved");
        intent.putExtra("other", "untouched");
        return intent;
    }

    private static JSONObject okJson(PluginTestHarness.ExecuteResult result) {
        assertTrue("execute() should report getIntent as handled", result.handled);
        PluginResult pluginResult = result.only();
        assertEquals(PluginResult.Status.OK.ordinal(), pluginResult.getStatus());
        assertEquals(PluginResult.MESSAGE_TYPE_JSON, pluginResult.getMessageType());
        return parse(pluginResult);
    }

    private static JSONObject parse(PluginResult pluginResult) {
        try {
            return new JSONObject(pluginResult.getMessage());
        } catch (JSONException e) {
            throw new AssertionError("Result payload was not a JSON object: "
                + pluginResult.getMessage(), e);
        }
    }

    @Test
    public void getIntent_coldStart_reportsEveryFieldOfTheLaunchIntent() throws JSONException {
        PluginTestHarness harness = new PluginTestHarness(fullLaunchIntent());

        JSONObject json = okJson(harness.execute("getIntent", "[]"));

        assertEquals(LAUNCH_ACTION, json.getString("action"));
        assertEquals(LAUNCH_FLAGS, json.getInt("flags"));
        assertEquals(LAUNCH_DATA, json.getString("data"));

        JSONArray categories = json.getJSONArray("categories");
        assertEquals(1, categories.length());
        assertEquals(LAUNCH_CATEGORY, categories.getString(0));

        JSONObject extras = json.getJSONObject("extras");
        assertEquals(true, extras.getBoolean("aBoolean"));
        assertEquals(42, extras.getInt("anInteger"));
        assertEquals(4294967296L, extras.getLong("aLong"));
        assertEquals(1.5d, extras.getDouble("aFloat"), 0.0d);
        assertEquals(2.5d, extras.getDouble("aDouble"), 0.0d);
        assertEquals("hello", extras.getString("aString"));
    }

    @Test
    public void getIntent_firstCall_reportsTheReservedShortcutExtras() throws JSONException {
        PluginTestHarness harness = new PluginTestHarness(fullLaunchIntent());

        JSONObject extras = okJson(harness.execute("getIntent", "[]")).getJSONObject("extras");

        assertEquals("reserved-exact", extras.getString("shortcut"));
        assertEquals("reserved-namespaced", extras.getString("shortcut.foo"));
    }

    @Test
    public void getIntent_secondCall_consumesOnlyTheReservedShortcutExtras() throws JSONException {
        PluginTestHarness harness = new PluginTestHarness(fullLaunchIntent());
        harness.execute("getIntent", "[]");

        JSONObject json = okJson(harness.execute("getIntent", "[]"));
        JSONObject extras = json.getJSONObject("extras");

        assertFalse("'shortcut' must be consumed by the first getIntent", extras.has("shortcut"));
        assertFalse("'shortcut.foo' must be consumed by the first getIntent",
            extras.has("shortcut.foo"));
        assertEquals("not-reserved", extras.getString("shortcutty"));
        assertEquals("untouched", extras.getString("other"));
        assertEquals("hello", extras.getString("aString"));
        assertEquals(LAUNCH_ACTION, json.getString("action"));
    }

    @Test
    public void getIntent_afterAnotherPluginSwapsTheActivityIntent_stillReportsTheLaunchIntent()
            throws JSONException {
        PluginTestHarness harness = new PluginTestHarness(fullLaunchIntent());
        harness.activity.setIntent(new Intent());

        JSONObject json = okJson(harness.execute("getIntent", "[]"));

        assertEquals(LAUNCH_ACTION, json.getString("action"));
        assertEquals(LAUNCH_DATA, json.getString("data"));
        assertEquals("hello", json.getJSONObject("extras").getString("aString"));
    }

    @Test
    public void getIntent_withoutExtras_omitsTheExtrasKey() throws JSONException {
        PluginTestHarness harness = new PluginTestHarness(new Intent(Intent.ACTION_MAIN));

        JSONObject json = okJson(harness.execute("getIntent", "[]"));

        assertFalse("an intent with no extras must not report an 'extras' key", json.has("extras"));
        assertEquals(Intent.ACTION_MAIN, json.getString("action"));
        assertTrue("'flags' is always reported", json.has("flags"));
    }

    @Test
    public void onNewIntent_subscription_deliversNothingUntilAnIntentArrives() {
        PluginTestHarness harness = new PluginTestHarness();

        PluginTestHarness.ExecuteResult subscription = harness.execute("onNewIntent", "[]");

        assertTrue("execute() should report onNewIntent as handled", subscription.handled);
        assertTrue("subscribing must not send an immediate result", subscription.results.isEmpty());
    }

    @Test
    public void onNewIntent_deliversTheIntentToTheSubscriberWithKeepCallback() throws JSONException {
        PluginTestHarness harness = new PluginTestHarness();
        PluginTestHarness.ExecuteResult subscription = harness.execute("onNewIntent", "[]");

        Intent newIntent = new Intent("com.example.RESUMED", Uri.parse("https://example.com/next"));
        newIntent.putExtra("aString", "from-new-intent");
        harness.plugin.onNewIntent(newIntent);

        List<PluginResult> delivered = harness.resultsFor(subscription.callbackId);
        assertEquals(1, delivered.size());
        PluginResult result = delivered.get(0);
        assertEquals(PluginResult.Status.OK.ordinal(), result.getStatus());
        assertTrue("the subscription callback must be kept alive", result.getKeepCallback());

        JSONObject json = parse(result);
        assertEquals("com.example.RESUMED", json.getString("action"));
        assertEquals("https://example.com/next", json.getString("data"));
        assertEquals("from-new-intent", json.getJSONObject("extras").getString("aString"));
    }

    @Test
    public void onNewIntent_firesAgainOnTheSameCallbackForASecondIntent() throws JSONException {
        PluginTestHarness harness = new PluginTestHarness();
        PluginTestHarness.ExecuteResult subscription = harness.execute("onNewIntent", "[]");

        harness.plugin.onNewIntent(new Intent("com.example.FIRST"));
        harness.plugin.onNewIntent(new Intent("com.example.SECOND"));

        List<PluginResult> delivered = harness.resultsFor(subscription.callbackId);
        assertEquals(2, delivered.size());
        assertEquals("com.example.FIRST", parse(delivered.get(0)).getString("action"));
        assertEquals("com.example.SECOND", parse(delivered.get(1)).getString("action"));
        assertTrue(delivered.get(1).getKeepCallback());
    }

    @Test
    public void onNewIntent_unsubscribing_stopsFurtherDeliveries() {
        PluginTestHarness harness = new PluginTestHarness();
        PluginTestHarness.ExecuteResult subscription = harness.execute("onNewIntent", "[]");
        harness.plugin.onNewIntent(new Intent("com.example.FIRST"));

        PluginTestHarness.ExecuteResult unsubscribe = harness.execute("onNewIntent", "[true]");
        harness.plugin.onNewIntent(new Intent("com.example.SECOND"));

        assertTrue("execute() should report the unsubscribe as handled", unsubscribe.handled);
        assertTrue("unsubscribing must not send a result", unsubscribe.results.isEmpty());
        assertTrue("no result may be sent to the unsubscribe callback",
            harness.resultsFor(unsubscribe.callbackId).isEmpty());
        assertEquals("no further deliveries after unsubscribing",
            1, harness.resultsFor(subscription.callbackId).size());
    }

    @Test
    public void onNewIntent_withoutASubscriber_deliversNothingAndDoesNotThrow() {
        PluginTestHarness harness = new PluginTestHarness();

        harness.plugin.onNewIntent(new Intent("com.example.NO_SUBSCRIBER"));

        assertTrue("nothing may be sent when no callback is registered",
            harness.webView.capturedResults.isEmpty());
    }
}
