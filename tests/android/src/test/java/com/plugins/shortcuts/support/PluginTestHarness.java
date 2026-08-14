package com.plugins.shortcuts.support;

import static org.robolectric.Shadows.shadowOf;

import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ShortcutManager;

import androidx.appcompat.app.AppCompatActivity;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPreferences;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.robolectric.Robolectric;
import org.robolectric.shadows.ShadowShortcutManager;

import com.plugins.shortcuts.ShortcutsPlugin;

/**
 * One-stop setup: builds a Robolectric activity, wires the real ShortcutsPlugin to the
 * recording web view, and drives the real {@code execute()} entry point.
 */
public class PluginTestHarness {

    public final AppCompatActivity activity;
    public final RecordingCordovaWebView webView;
    public final TestCordovaInterface cordovaInterface;
    public final ShortcutsPlugin plugin;

    /** The resId seeded into {@code applicationInfo.icon}; the app-icon fallback target. */
    public final int appIconResId;

    private int callbackCounter = 0;

    public static class ExecuteResult {
        public final boolean handled;
        public final String callbackId;
        public final List<PluginResult> results;

        ExecuteResult(boolean handled, String callbackId, List<PluginResult> results) {
            this.handled = handled;
            this.callbackId = callbackId;
            this.results = results;
        }

        public PluginResult only() {
            if (results.size() != 1) {
                throw new AssertionError("Expected exactly one PluginResult, got " + results.size());
            }
            return results.get(0);
        }
    }

    public PluginTestHarness() {
        this(null);
    }

    public PluginTestHarness(Intent launchIntent) {
        this.activity = launchIntent == null
            ? Robolectric.buildActivity(AppCompatActivity.class).create().get()
            : Robolectric.buildActivity(AppCompatActivity.class, launchIntent).create().get();

        this.appIconResId = TestFixtures.seedApplicationIcon(activity);

        this.webView = new RecordingCordovaWebView(activity);
        this.cordovaInterface = new TestCordovaInterface(activity);
        this.plugin = new ShortcutsPlugin();
        this.plugin.privateInitialize(
            "ShortcutsPlugin", cordovaInterface, webView, new CordovaPreferences());
    }

    public Context context() {
        return activity.getApplicationContext();
    }

    public String packageName() {
        return activity.getPackageName();
    }

    public ShortcutManager shortcutManager() {
        return activity.getSystemService(ShortcutManager.class);
    }

    public ShadowShortcutManager shadowShortcutManager() {
        return shadowOf(shortcutManager());
    }

    public ExecuteResult execute(String action, String argsJson) {
        String callbackId = "cb" + (++callbackCounter);
        CallbackContext callbackContext = new CallbackContext(callbackId, webView);
        boolean handled;
        try {
            handled = plugin.execute(action, new JSONArray(argsJson), callbackContext);
        } catch (JSONException e) {
            throw new AssertionError("Malformed test args JSON: " + argsJson, e);
        }
        return new ExecuteResult(handled, callbackId, webView.resultsFor(callbackId));
    }

    /** Results captured for a callbackId after later activity (e.g. onNewIntent deliveries). */
    public List<PluginResult> resultsFor(String callbackId) {
        return webView.resultsFor(callbackId);
    }
}
