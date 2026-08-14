package com.plugins.shortcuts.support;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import android.content.Context;
import android.view.View;
import android.content.Intent;
import android.webkit.WebChromeClient.CustomViewCallback;

import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPreferences;
import org.apache.cordova.CordovaResourceApi;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.CordovaWebViewEngine;
import org.apache.cordova.ICordovaCookieManager;
import org.apache.cordova.PluginEntry;
import org.apache.cordova.PluginManager;
import org.apache.cordova.PluginResult;

/**
 * Captures everything handed to {@code sendPluginResult} — the single funnel every
 * {@code CallbackContext.success()/error()/sendPluginResult()} call goes through.
 */
public class RecordingCordovaWebView implements CordovaWebView {

    public static class CapturedResult {
        public final PluginResult result;
        public final String callbackId;

        public CapturedResult(PluginResult result, String callbackId) {
            this.result = result;
            this.callbackId = callbackId;
        }
    }

    public final List<CapturedResult> capturedResults = new ArrayList<CapturedResult>();

    private final Context context;

    public RecordingCordovaWebView(Context context) {
        this.context = context;
    }

    public List<PluginResult> resultsFor(String callbackId) {
        List<PluginResult> matches = new ArrayList<PluginResult>();
        for (CapturedResult captured : capturedResults) {
            if (callbackId.equals(captured.callbackId)) {
                matches.add(captured.result);
            }
        }
        return matches;
    }

    @Override
    public void sendPluginResult(PluginResult cr, String callbackId) {
        capturedResults.add(new CapturedResult(cr, callbackId));
    }

    @Override
    public Context getContext() {
        return context;
    }

    @Override
    public void init(CordovaInterface cordova, List<PluginEntry> pluginEntries, CordovaPreferences preferences) {
    }

    @Override
    public boolean isInitialized() {
        return true;
    }

    @Override
    public View getView() {
        return null;
    }

    @Override
    public void loadUrlIntoView(String url, boolean recreatePlugins) {
    }

    @Override
    public void stopLoading() {
    }

    @Override
    public boolean canGoBack() {
        return false;
    }

    @Override
    public void clearCache() {
    }

    @Override
    @Deprecated
    public void clearCache(boolean b) {
    }

    @Override
    public void clearHistory() {
    }

    @Override
    public boolean backHistory() {
        return false;
    }

    @Override
    public void handlePause(boolean keepRunning) {
    }

    @Override
    public void onNewIntent(Intent intent) {
    }

    @Override
    public void handleResume(boolean keepRunning) {
    }

    @Override
    public void handleStart() {
    }

    @Override
    public void handleStop() {
    }

    @Override
    public void handleDestroy() {
    }

    @Override
    @Deprecated
    public void sendJavascript(String statememt) {
    }

    @Override
    public void showWebPage(String url, boolean openExternal, boolean clearHistory, Map<String, Object> params) {
    }

    @Override
    @Deprecated
    public boolean isCustomViewShowing() {
        return false;
    }

    @Override
    @Deprecated
    public void showCustomView(View view, CustomViewCallback callback) {
    }

    @Override
    @Deprecated
    public void hideCustomView() {
    }

    @Override
    public CordovaResourceApi getResourceApi() {
        return null;
    }

    @Override
    public void setButtonPlumbedToJs(int keyCode, boolean override) {
    }

    @Override
    public boolean isButtonPlumbedToJs(int keyCode) {
        return false;
    }

    @Override
    public PluginManager getPluginManager() {
        return null;
    }

    @Override
    public CordovaWebViewEngine getEngine() {
        return null;
    }

    @Override
    public CordovaPreferences getPreferences() {
        return null;
    }

    @Override
    public ICordovaCookieManager getCookieManager() {
        return null;
    }

    @Override
    public String getUrl() {
        return null;
    }

    @Override
    public void loadUrl(String url) {
    }

    @Override
    public Object postMessage(String id, Object data) {
        return null;
    }
}
