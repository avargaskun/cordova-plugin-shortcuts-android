package com.plugins.shortcuts;

import java.security.InvalidParameterException;
import java.util.Set;
import java.util.ArrayList;
import java.util.Iterator;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;
import android.net.Uri;
import android.util.Base64;
import android.util.Log;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Icon;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.content.res.Resources;
import android.support.v4.content.pm.ShortcutInfoCompat;
import android.support.v4.content.pm.ShortcutManagerCompat;
import android.support.v4.graphics.drawable.IconCompat;

public class ShortcutsPlugin extends CordovaPlugin {

    private static final String TAG = "ShortcutsPlugin";
    private static final String ACTION_SUPPORTS_DYNAMIC = "supportsDynamic";
    private static final String ACTION_SUPPORTS_PINNED = "supportsPinned";
    private static final String ACTION_SET_DYNAMIC = "setDynamic";
    private static final String ACTION_ADD_PINNED = "addPinned";
    private static final String ACTION_GET_INTENT = "getIntent";
    private static final String ACTION_ON_NEW_INTENT = "onNewIntent";

    private CallbackContext onNewIntentCallbackContext = null;

    @Override
    public boolean execute(
        String action, 
        JSONArray args,
        CallbackContext callbackContext) {
            try {
                if (action.equals(ACTION_SUPPORTS_DYNAMIC)) {
                    boolean supported = Build.VERSION.SDK_INT >= 25;
                    callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK, supported));
                    return true;
                }
                else if (action.equals(ACTION_SUPPORTS_PINNED)) {
                    boolean supported = this.supportsPinned();
                    callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK, supported));
                    return true;
                } 
                else if (action.equals(ACTION_SET_DYNAMIC)) {
                    setDynamicShortcuts(args);
                    callbackContext.success();
                    return true;
                } else if (action.equals(ACTION_ADD_PINNED)) {
                    boolean success = addPinnedShortcut(args);
                    if (success) {
                        callbackContext.success();
                    } else {
                        callbackContext.error("Pinned shortcuts are not supported by the default launcher.");
                    }
                } else if (action.equals(ACTION_GET_INTENT)) {
                    getIntent(callbackContext);
                    return true;
                } else if (action.equals(ACTION_ON_NEW_INTENT)) {
                    subscribeOnNewIntent(args, callbackContext);
                    return true;
                }
            }
            catch (Exception e) {
                Log.e(TAG, "Exception executing Cordova action: " + e.getMessage());
                callbackContext.error(e.getMessage());
                return true;
            }

            return false;
    }

    @Override
    public void onNewIntent(
        Intent intent
    ) {
        try {
            if (this.onNewIntentCallbackContext != null) {
                PluginResult result = new PluginResult(PluginResult.Status.OK, buildIntent(intent));
                result.setKeepCallback(true);
                this.onNewIntentCallbackContext.sendPluginResult(result);
            }
        } catch (Exception e) {
            Log.e(TAG, "Exception handling onNewIntent: " + e.getMessage());            
        }
    }

    private boolean supportsPinned() {
        Context context = this.cordova.getActivity().getApplicationContext();
        return ShortcutManagerCompat.isRequestPinShortcutSupported(context);
    }

    private void subscribeOnNewIntent(
        JSONArray args, 
        CallbackContext callbackContext
    ) throws JSONException {
        boolean remove = args.optBoolean(0);
        if (remove) {
            this.onNewIntentCallbackContext = null;
            Log.i(TAG, "Removed callback for onNewIntent");
        }
        else {
            this.onNewIntentCallbackContext = callbackContext;
            Log.i(TAG, "Added a new callback for onNewIntent");
        }
    }

    private void getIntent(CallbackContext callbackContext) throws JSONException  {
        Intent intent = this.cordova.getActivity().getIntent();
        PluginResult result = new PluginResult(PluginResult.Status.OK, buildIntent(intent));
        callbackContext.sendPluginResult(result);
    }

    private JSONObject buildIntent(
        Intent intent
    ) throws JSONException  {
        JSONObject jsonIntent = new JSONObject();

        jsonIntent.put("action", intent.getAction());
        jsonIntent.put("flags", intent.getFlags());
        
        Set<String> categories = intent.getCategories();
        if (categories != null) {
            jsonIntent.put("categories", new JSONArray(categories));
        }
        
        Uri data = intent.getData();
        if (data != null) {
            jsonIntent.put("data", data.toString());
        }

        Bundle extras = intent.getExtras();
        if (extras != null) {
            JSONObject jsonExtras = new JSONObject();
            jsonIntent.put("extras", jsonExtras);
            Iterator<String> keys = extras.keySet().iterator();
            while (keys.hasNext()) {
                String key = keys.next();
                Object value = extras.get(key);
                if (value instanceof Boolean) {
                    jsonExtras.put(key, (Boolean)value);
                } 
                else if (value instanceof Integer) {
                    jsonExtras.put(key, (Integer)value);
                }
                else if (value instanceof Long) {
                    jsonExtras.put(key, (Long)value);
                }
                else if (value instanceof Float) {
                    jsonExtras.put(key, (Float)value);
                }
                else if (value instanceof Double) {
                    jsonExtras.put(key, (Double)value);
                }
                else {
                    jsonExtras.put(key, value.toString());
                }
            }
        }

        return jsonIntent;
    }

    private Intent parseIntent(
        JSONObject jsonIntent
    ) throws JSONException {

        Intent intent = new Intent();
        
        String activityClass = jsonIntent.optString(
            "activityClass", 
            this.cordova.getActivity().getClass().getName());
        String activityPackage = jsonIntent.optString(
            "activityPackage", 
            this.cordova.getActivity().getPackageName());
        intent.setClassName(activityPackage, activityClass);

        String action = jsonIntent.optString("action", Intent.ACTION_VIEW);
        if (action.indexOf('.') < 0) {
            action = activityPackage + '.' + action;
        }
        Log.i(TAG, "Creating new intent with action: " + action);
        intent.setAction(action);

        int flags = jsonIntent.optInt("flags", Intent.FLAG_ACTIVITY_NEW_TASK + Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.setFlags(flags); // TODO: Support passing different flags

        JSONArray jsonCategories = jsonIntent.optJSONArray("categories");
        if (jsonCategories != null) {
            int count = jsonCategories.length();
            for (int i = 0; i < count; ++i) {
                String category = jsonCategories.getString(i);
                if (category.indexOf('.') < 0) {
                    category = activityPackage + '.' + category;
                }
                intent.addCategory(category);
            }
        }

        String data = jsonIntent.optString("data");
        if (data.length() > 0) {
            intent.setData(Uri.parse(data));
        }

        JSONObject extras = jsonIntent.optJSONObject("extras");
        if (extras != null) {
            Iterator<String> keys = extras.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                Object value = extras.get(key);
                if (value != null) {
                    if (key.indexOf('.') < 0) {
                        key = activityPackage + "." + key;                            
                    }
                    if (value instanceof Boolean) {
                        intent.putExtra(key, (Boolean)value);
                    } 
                    else if (value instanceof Integer) {
                        intent.putExtra(key, (Integer)value);
                    }
                    else if (value instanceof Long) {
                        intent.putExtra(key, (Long)value);
                    } 
                    else if (value instanceof Float) {
                        intent.putExtra(key, (Float)value);
                    } 
                    else if (value instanceof Double) {
                        intent.putExtra(key, (Double)value);
                    } 
                    else {
                        intent.putExtra(key, value.toString());
                    }
                }
            }
        }
        return intent;
    }

    private ShortcutInfo buildDynamicShortcut(
        JSONObject jsonShortcut) throws PackageManager.NameNotFoundException, JSONException {
            if (jsonShortcut == null) {
                throw new InvalidParameterException("Shortcut object cannot be null");
            }

            Context context = this.cordova.getActivity().getApplicationContext();
            String shortcutId = jsonShortcut.optString("id");
            if (shortcutId.length() == 0) {
                throw new InvalidParameterException("A value for 'id' is required");
            }

            ShortcutInfo.Builder builder = new ShortcutInfo.Builder(context, shortcutId);
        
            String shortLabel = jsonShortcut.optString("shortLabel");
            String longLabel = jsonShortcut.optString("longLabel");
            if (shortLabel.length() == 0 && longLabel.length() == 0) {
                throw new InvalidParameterException("A value for either 'shortLabel' or 'longLabel' is required");
            }

            if (shortLabel.length() == 0) {
                shortLabel = longLabel;
            }

            if (longLabel.length() == 0) {
                longLabel = shortLabel;
            }

            Icon icon;
            String iconBitmap = jsonShortcut.optString("iconBitmap");
            String iconFromResource = jsonShortcut.optString("iconFromResource");

            String activityPackage = this.cordova.getActivity().getPackageName();
        
            if (iconBitmap.length() > 0) {
                icon = Icon.createWithBitmap(decodeBase64Bitmap(iconBitmap));
            } 
        
            if (iconFromResource.length() > 0){
                Resources activityRes = this.cordova.getActivity().getResources();
                int iconId = activityRes.getIdentifier(iconFromResource, "drawable", activityPackage);
                icon = Icon.createWithResource(context, iconId);
            }
        
            else {
                PackageManager pm = context.getPackageManager();
                ApplicationInfo applicationInfo = pm.getApplicationInfo(activityPackage, PackageManager.GET_META_DATA);
                icon = Icon.createWithResource(activityPackage, applicationInfo.icon);
            }

            JSONObject jsonIntent = jsonShortcut.optJSONObject("intent");
            if (jsonIntent == null) {
                jsonIntent = new JSONObject();
            }

            Intent intent = parseIntent(jsonIntent);

            return builder
                .setShortLabel(shortLabel)
                .setLongLabel(longLabel)
                .setIntent(intent)
                .setIcon(icon)
                .build();
    }

    private void setDynamicShortcuts(
        JSONArray args) throws PackageManager.NameNotFoundException, JSONException {
            int count = args.length();
            ArrayList<ShortcutInfo> shortcuts = new ArrayList<ShortcutInfo>(count);

            for (int i = 0; i < count; ++i) {
                shortcuts.add(buildDynamicShortcut(args.optJSONObject(i)));
            }

            Context context = this.cordova.getActivity().getApplicationContext();
            ShortcutManager shortcutManager = context.getSystemService(ShortcutManager.class);
            shortcutManager.setDynamicShortcuts(shortcuts);

            Log.i(TAG, String.format("Saved % dynamic shortcuts.", count));
    }

    private ShortcutInfoCompat buildPinnedShortcut(
        JSONObject jsonShortcut
    ) throws PackageManager.NameNotFoundException, JSONException {
        if (jsonShortcut == null) {
            throw new InvalidParameterException("Parameters must include a valid shorcut.");
        }

        Context context = this.cordova.getActivity().getApplicationContext();
        String shortcutId = jsonShortcut.optString("id");
        if (shortcutId.length() == 0) {
            throw new InvalidParameterException("A value for 'id' is required");
        }

        ShortcutInfoCompat.Builder builder = new ShortcutInfoCompat.Builder(context, shortcutId);
    
        String shortLabel = jsonShortcut.optString("shortLabel");
        String longLabel = jsonShortcut.optString("longLabel");
        if (shortLabel.length() == 0 && longLabel.length() == 0) {
            throw new InvalidParameterException("A value for either 'shortLabel' or 'longLabel' is required");
        }

        if (shortLabel.length() == 0) {
            shortLabel = longLabel;
        }

        if (longLabel.length() == 0) {
            longLabel = shortLabel;
        }

        IconCompat icon;
        String iconBitmap = jsonShortcut.optString("iconBitmap");

        if (iconBitmap.length() > 0) {
            icon = IconCompat.createWithBitmap(decodeBase64Bitmap(iconBitmap));
        }
        else {
            String activityPackage = this.cordova.getActivity().getPackageName();
            PackageManager pm = context.getPackageManager();
            ApplicationInfo applicationInfo = pm.getApplicationInfo(activityPackage, PackageManager.GET_META_DATA);
            icon = IconCompat.createWithResource(context, applicationInfo.icon);
        }

        JSONObject jsonIntent = jsonShortcut.optJSONObject("intent");
        if (jsonIntent == null) {
            jsonIntent = new JSONObject();
        }

        Intent intent = parseIntent(jsonIntent);

        return builder
            .setActivity(intent.getComponent())
            .setShortLabel(shortLabel)
            .setLongLabel(longLabel)
            .setIcon(icon)
            .setIntent(intent)
            .build();
    }

    private boolean addPinnedShortcut(
        JSONArray args
    ) throws PackageManager.NameNotFoundException, JSONException {
        ShortcutInfoCompat shortcut = buildPinnedShortcut(args.optJSONObject(0));
        Context context = this.cordova.getActivity().getApplicationContext();
        return ShortcutManagerCompat.requestPinShortcut(context, shortcut, null);
    }

    private static Bitmap decodeBase64Bitmap(
        String input) {
            byte[] decodedByte = Base64.decode(input, 0);
            return BitmapFactory.decodeByteArray(decodedByte, 0, decodedByte.length);
    }
}
