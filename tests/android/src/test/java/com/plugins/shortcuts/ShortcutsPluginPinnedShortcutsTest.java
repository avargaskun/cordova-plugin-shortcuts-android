package com.plugins.shortcuts;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

import java.util.List;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.ResolveInfo;
import android.content.pm.ShortcutInfo;
import android.graphics.Bitmap;
import android.graphics.drawable.Icon;
import android.os.Bundle;

import org.apache.cordova.PluginResult;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import com.plugins.shortcuts.support.PluginTestHarness;
import com.plugins.shortcuts.support.TestFixtures;

/**
 * Integration coverage for {@code addPinned}, driven through the real {@code execute()}.
 *
 * <p>Note that {@code execute()} returns <b>false</b> for a successful (or launcher-rejected)
 * addPinned: that branch sends its result but has no {@code return true}, so control falls through.
 * That is current 0.1.4 behavior and is characterized here deliberately, not fixed.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class ShortcutsPluginPinnedShortcutsTest {

    private static final String INSTALL_SHORTCUT_ACTION = "com.android.launcher.action.INSTALL_SHORTCUT";
    private static final String INSTALL_SHORTCUT_PERMISSION =
        "com.android.launcher.permission.INSTALL_SHORTCUT";

    /** A pin request delivered to the launcher: OK result, and execute() falls through to false. */
    private static void assertPinRequested(PluginTestHarness.ExecuteResult result) {
        assertFalse("the addPinned branch has no 'return true' — execute() falls through",
            result.handled);
        assertEquals(PluginResult.Status.OK.ordinal(), result.only().getStatus());
    }

    /** The launcher refused: error() is sent from the same fall-through branch. */
    private static void assertUnsupportedLauncherError(PluginTestHarness.ExecuteResult result) {
        assertFalse("the addPinned branch has no 'return true' — execute() falls through",
            result.handled);
        PluginResult pluginResult = result.only();
        assertEquals(PluginResult.Status.ERROR.ordinal(), pluginResult.getStatus());
        assertEquals("Pinned shortcuts are not supported by the default launcher.",
            pluginResult.getStrMessage());
    }

    /** A thrown validation failure is caught by execute()'s catch block, which returns true. */
    private static void assertThrownError(PluginTestHarness.ExecuteResult result, String message) {
        assertTrue("execute()'s catch block reports the action as handled", result.handled);
        PluginResult pluginResult = result.only();
        assertEquals(PluginResult.Status.ERROR.ordinal(), pluginResult.getStatus());
        assertEquals(message, pluginResult.getStrMessage());
    }

    /**
     * At SDK 26+ ShortcutManagerCompat.requestPinShortcut goes straight to the platform manager and
     * never consults isRequestPinShortcutSupported, so the flag only governs {@code supportsPinned}
     * here. It is still set explicitly because the shadow's backing field is static.
     */
    private static PluginTestHarness harnessWithPinSupport() {
        PluginTestHarness harness = new PluginTestHarness();
        harness.shadowShortcutManager().setIsRequestPinShortcutSupported(true);
        return harness;
    }

    private static ShortcutInfo onlyPinnedShortcut(PluginTestHarness harness) {
        List<ShortcutInfo> pinned = harness.shortcutManager().getPinnedShortcuts();
        assertEquals("expected exactly one pin request", 1, pinned.size());
        return pinned.get(0);
    }

    /** Requests a pin for a single shortcut carrying the given extra icon fields, returns its Icon. */
    private static Icon pinnedIconFor(PluginTestHarness harness, String iconFields) {
        assertPinRequested(harness.execute("addPinned",
            "[{\"id\": \"one\", \"shortLabel\": \"One\", " + iconFields + "}]"));
        return TestFixtures.iconOf(onlyPinnedShortcut(harness));
    }

    // ---------------------------------------------------------------- happy path

    @Test
    public void addPinned_mapsEveryFieldOfAFullyPopulatedShortcut() {
        PluginTestHarness harness = harnessWithPinSupport();

        assertPinRequested(harness.execute("addPinned", "["
            + "{"
            + "  \"id\": \"pinned-1\","
            + "  \"shortLabel\": \"Short\","
            + "  \"longLabel\": \"A considerably longer label\","
            + "  \"intent\": {"
            + "    \"action\": \"com.example.DO_THING\","
            + "    \"categories\": [\"com.example.CAT_ONE\"],"
            + "    \"data\": \"https://example.com/path?q=1\","
            + "    \"flags\": " + Intent.FLAG_ACTIVITY_NEW_TASK + ","
            + "    \"extras\": {\"greeting\": \"hello\", \"count\": 3, \"on\": true}"
            + "  }"
            + "}]"));

        ShortcutInfo shortcut = onlyPinnedShortcut(harness);
        assertEquals("pinned-1", shortcut.getId());
        assertEquals("Short", shortcut.getShortLabel().toString());
        assertEquals("A considerably longer label", shortcut.getLongLabel().toString());
        assertEquals(harness.packageName(), shortcut.getActivity().getPackageName());
        assertEquals(harness.activity.getClass().getName(), shortcut.getActivity().getClassName());

        Intent intent = shortcut.getIntent();
        assertEquals("com.example.DO_THING", intent.getAction());
        assertEquals("https://example.com/path?q=1", intent.getData().toString());
        assertEquals(1, intent.getCategories().size());
        assertTrue(intent.getCategories().contains("com.example.CAT_ONE"));
        assertEquals(Intent.FLAG_ACTIVITY_NEW_TASK, intent.getFlags());
        assertEquals(harness.packageName(), intent.getComponent().getPackageName());

        Bundle extras = intent.getExtras();
        assertEquals("hello", extras.get("greeting"));
        assertEquals(Integer.valueOf(3), extras.get("count"));
        assertEquals(Boolean.TRUE, extras.get("on"));

        // Default (no icon fields given) is the application icon.
        Icon icon = TestFixtures.iconOf(shortcut);
        assertEquals(Icon.TYPE_RESOURCE, TestFixtures.iconType(icon));
        assertEquals(harness.appIconResId, TestFixtures.iconResId(icon));
    }

    // The launcher-refusal error is exercised at sdk 25 by legacyPin_withoutLauncherSupport_isAnError:
    // at 26+ ShortcutManagerCompat delegates straight to ShortcutManager.requestPinShortcut, which
    // never reports refusal (and Robolectric's shadow unconditionally returns true), so the plugin's
    // error branch is unreachable there.

    // ------------------------------------------------------------- validation

    @Test
    public void addPinned_withoutId_isAnError() {
        PluginTestHarness harness = harnessWithPinSupport();

        assertThrownError(harness.execute("addPinned", "[{\"shortLabel\": \"No id\"}]"),
            "A value for 'id' is required");
        assertTrue(harness.shortcutManager().getPinnedShortcuts().isEmpty());
    }

    @Test
    public void addPinned_withoutEitherLabel_isAnError() {
        PluginTestHarness harness = harnessWithPinSupport();

        assertThrownError(harness.execute("addPinned", "[{\"id\": \"one\"}]"),
            "A value for either 'shortLabel' or 'longLabel' is required");
        assertTrue(harness.shortcutManager().getPinnedShortcuts().isEmpty());
    }

    /**
     * The pinned path has its own null-shortcut message, distinct from the dynamic path's
     * "Shortcut object cannot be null" — typo included. Pinned deliberately here: unifying the two
     * texts would be a user-visible change.
     */
    @Test
    public void addPinned_withNoShortcutObject_isAnErrorWithThePinnedPathsOwnMessage() {
        PluginTestHarness harness = harnessWithPinSupport();

        assertThrownError(harness.execute("addPinned", "[]"),
            "Parameters must include a valid shorcut.");
        assertTrue(harness.shortcutManager().getPinnedShortcuts().isEmpty());
    }

    @Test
    public void addPinned_withANullShortcut_isAnErrorWithThePinnedPathsOwnMessage() {
        PluginTestHarness harness = harnessWithPinSupport();

        assertThrownError(harness.execute("addPinned", "[null]"),
            "Parameters must include a valid shorcut.");
    }

    // ----------------------------------------------------------- icon branches

    @Test
    public void icon_bitmap_becomesABitmapIcon() {
        PluginTestHarness harness = harnessWithPinSupport();

        Icon icon = pinnedIconFor(harness,
            "\"iconBitmap\": \"" + TestFixtures.ONE_BY_ONE_PNG_BASE64 + "\"");

        assertEquals(Icon.TYPE_BITMAP, TestFixtures.iconType(icon));
    }

    @Test
    public void icon_adaptiveBitmap_becomesAnAdaptiveIcon() {
        PluginTestHarness harness = harnessWithPinSupport();

        Icon icon = pinnedIconFor(harness,
            "\"iconBitmap\": \"" + TestFixtures.ADAPTIVE_48_PNG_BASE64 + "\","
            + "\"iconAdaptiveBitmap\": true");

        assertEquals(Icon.TYPE_ADAPTIVE_BITMAP, TestFixtures.iconType(icon));
    }

    @Test
    public void icon_fromResource_becomesAResourceIconWithThatDrawablesId() {
        PluginTestHarness harness = harnessWithPinSupport();

        Icon icon = pinnedIconFor(harness,
            "\"iconFromResource\": \"" + TestFixtures.TEST_DRAWABLE_NAME + "\"");

        assertEquals(Icon.TYPE_RESOURCE, TestFixtures.iconType(icon));
        assertEquals(TestFixtures.testDrawableId(harness.activity), TestFixtures.iconResId(icon));
    }

    @Test
    public void icon_bitmapTakesPrecedenceOverFromResource() {
        PluginTestHarness harness = harnessWithPinSupport();

        Icon icon = pinnedIconFor(harness,
            "\"iconBitmap\": \"" + TestFixtures.ONE_BY_ONE_PNG_BASE64 + "\","
            + "\"iconFromResource\": \"" + TestFixtures.TEST_DRAWABLE_NAME + "\"");

        assertEquals(Icon.TYPE_BITMAP, TestFixtures.iconType(icon));
    }

    @Test
    public void icon_withNeitherField_fallsBackToTheApplicationIcon() {
        PluginTestHarness harness = harnessWithPinSupport();

        assertPinRequested(harness.execute("addPinned",
            "[{\"id\": \"one\", \"shortLabel\": \"One\"}]"));

        Icon icon = TestFixtures.iconOf(onlyPinnedShortcut(harness));
        assertEquals(Icon.TYPE_RESOURCE, TestFixtures.iconType(icon));
        assertEquals(harness.appIconResId, TestFixtures.iconResId(icon));
    }

    @Test
    public void icon_fromMissingResource_fallsBackToTheApplicationIcon() {
        PluginTestHarness harness = harnessWithPinSupport();

        Icon icon = pinnedIconFor(harness, "\"iconFromResource\": \"does_not_exist\"");

        assertEquals(Icon.TYPE_RESOURCE, TestFixtures.iconType(icon));
        assertEquals(harness.appIconResId, TestFixtures.iconResId(icon));
    }

    // --------------------------------------------------- legacy pin path (sdk 25)

    /**
     * Below API 26 ShortcutManagerCompat has no platform requestPinShortcut: it broadcasts
     * {@code com.android.launcher.action.INSTALL_SHORTCUT}, and gates that on the app holding the
     * INSTALL_SHORTCUT permission AND a receiver being registered for the action.
     */
    private static void arrangeLegacyPinSupport(PluginTestHarness harness) {
        shadowOf(RuntimeEnvironment.getApplication()).grantPermissions(INSTALL_SHORTCUT_PERMISSION);

        ResolveInfo launcherReceiver = new ResolveInfo();
        launcherReceiver.activityInfo = new ActivityInfo();
        launcherReceiver.activityInfo.name = "InstallShortcutReceiver";
        launcherReceiver.activityInfo.packageName = "com.example.launcher";
        launcherReceiver.activityInfo.permission = INSTALL_SHORTCUT_PERMISSION;
        launcherReceiver.activityInfo.applicationInfo = new ApplicationInfo();
        launcherReceiver.activityInfo.applicationInfo.packageName = "com.example.launcher";

        shadowOf(harness.activity.getPackageManager())
            .addResolveInfoForIntent(new Intent(INSTALL_SHORTCUT_ACTION), launcherReceiver);
    }

    private static Bitmap legacyBroadcastIconFor(PluginTestHarness harness, String iconFields) {
        arrangeLegacyPinSupport(harness);

        assertPinRequested(harness.execute("addPinned",
            "[{\"id\": \"one\", \"shortLabel\": \"One\", " + iconFields + "}]"));

        Intent install = null;
        for (Intent broadcast : shadowOf(RuntimeEnvironment.getApplication()).getBroadcastIntents()) {
            if (INSTALL_SHORTCUT_ACTION.equals(broadcast.getAction())) {
                install = broadcast;
            }
        }
        if (install == null) {
            throw new AssertionError("No INSTALL_SHORTCUT broadcast was sent");
        }
        Bitmap bitmap = install.getParcelableExtra(Intent.EXTRA_SHORTCUT_ICON);
        if (bitmap == null) {
            throw new AssertionError("INSTALL_SHORTCUT broadcast carried no EXTRA_SHORTCUT_ICON");
        }
        return bitmap;
    }

    @Test
    @Config(sdk = 25)
    public void legacyPin_usesTheInstallShortcutBroadcast() {
        PluginTestHarness harness = new PluginTestHarness();
        arrangeLegacyPinSupport(harness);

        assertPinRequested(harness.execute("addPinned",
            "[{\"id\": \"one\", \"shortLabel\": \"One\"}]"));

        boolean broadcast = false;
        for (Intent intent : shadowOf(RuntimeEnvironment.getApplication()).getBroadcastIntents()) {
            if (INSTALL_SHORTCUT_ACTION.equals(intent.getAction())) {
                broadcast = true;
                assertEquals("One", intent.getStringExtra(Intent.EXTRA_SHORTCUT_NAME));
            }
        }
        assertTrue("expected an INSTALL_SHORTCUT broadcast", broadcast);
    }

    @Test
    @Config(sdk = 25)
    public void legacyPin_withoutLauncherSupport_isAnError() {
        PluginTestHarness harness = new PluginTestHarness();

        assertUnsupportedLauncherError(harness.execute("addPinned",
            "[{\"id\": \"one\", \"shortLabel\": \"One\"}]"));
    }

    /**
     * IconCompat does safe-zone cropping itself below API 26, so with no SDK guard the legacy
     * broadcast carries the masked rendering (32 = 2/3 x 48), not the raw 48x48 source.
     */
    @Test
    @Config(sdk = 25)
    public void legacyPin_adaptiveBitmapBelowSdk26_broadcastsTheLegacyMaskedBitmap() {
        PluginTestHarness harness = new PluginTestHarness();

        Bitmap bitmap = legacyBroadcastIconFor(harness,
            "\"iconBitmap\": \"" + TestFixtures.ADAPTIVE_48_PNG_BASE64 + "\","
            + "\"iconAdaptiveBitmap\": true");

        assertNotEquals(48, bitmap.getWidth());
        assertEquals(32, bitmap.getWidth());
        assertEquals(32, bitmap.getHeight());
    }
}
