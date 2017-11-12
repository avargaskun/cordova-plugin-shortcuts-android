package com.plugins.shortcuts;

import java.security.InvalidParameterException;
import java.util.ArrayList;
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
import android.content.Intent;
import android.content.Context;
import android.os.Build;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;

public class ShortcutsPlugin extends CordovaPlugin {

    private static final String TAG = "ShortcutsPlugin";
    private static final String ACTION_SUPPORTS_DYNAMIC = "supportsDynamic";
    private static final String ACTION_SUPPORTS_PINNED = "supportsPinned";
    private static final String ACTION_SET_DYNAMIC = "setDynamic";

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
                    boolean supported = Build.VERSION.SDK_INT >= 26;
                    callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK, supported));
                    return true;
                } 
                else if (action.equals(ACTION_SET_DYNAMIC)) {
                    setDynamicShortcuts(args);
                    return true;
                }
            }
            catch (Exception e) {
                Log.e(TAG, "Exception: " + e.getMessage());
                callbackContext.error(e.getMessage());
                return true;
            }

            return false;
    }

    private ShortcutInfo buildDynamicShortcut(
        JSONObject arg) throws PackageManager.NameNotFoundException {
            if (arg == null) {
                throw new InvalidParameterException("Shortcut object cannot be null");
            }

            String shortcutId = arg.optString("id");
            if (shortcutId.length() == 0) {
                throw new InvalidParameterException("A value for 'id' is required");
            }

            String activityClass = arg.optString(
                "activityClass", 
                this.cordova.getActivity().getClass().getName());

            String activityPackage = arg.optString(
                "activityPackage", 
                this.cordova.getActivity().getPackageName());

            Intent intent = new Intent();
            intent.setClassName(activityPackage, activityClass);
            intent.setAction(Intent.ACTION_VIEW);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK + Intent.FLAG_ACTIVITY_CLEAR_TOP);

            Context context = this.cordova.getActivity().getApplicationContext();
            ShortcutInfo.Builder builder = new ShortcutInfo.Builder(context, shortcutId);
        
            String data = arg.optString("data");
            if (data.length() > 0) {
                intent.setData(Uri.parse(data));
            }

            String subject = arg.optString("extraSubject");
            if (subject.length() > 0) {
                intent.putExtra(Intent.EXTRA_SUBJECT, subject);
            }

            String shortLabel = arg.optString("shortLabel");
            String longLabel = arg.optString("longLabel");
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
            String iconPath = arg.optString("iconPath");
            String iconBitmap = arg.optString("iconBitmap");

            if (iconPath.length() > 0) {
                icon = Icon.createWithFilePath(iconPath);
            }
            else if (iconBitmap.length() > 0) {
                icon = Icon.createWithBitmap(decodeBase64Bitmap(iconBitmap));
            }
            else {
                PackageManager pm = context.getPackageManager();
                ApplicationInfo applicationInfo = pm.getApplicationInfo(activityPackage, PackageManager.GET_META_DATA);
                icon = Icon.createWithResource(activityPackage, applicationInfo.icon);
            }

            return builder
                .setShortLabel(shortLabel)
                .setLongLabel(longLabel)
                .setIntent(intent)
                .setIcon(icon)
                .build();
    }

    private void setDynamicShortcuts(
        JSONArray args) throws PackageManager.NameNotFoundException {
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

    private static Bitmap decodeBase64Bitmap(
        String input) {
            byte[] decodedByte = Base64.decode(input, 0);
            return BitmapFactory.decodeByteArray(decodedByte, 0, decodedByte.length);
    }
}
