package com.plugins.shortcuts;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import android.content.Context;
import android.content.pm.PackageManager;

import androidx.core.graphics.drawable.IconCompat;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import com.plugins.shortcuts.support.TestFixtures;

/**
 * Unit coverage for {@link ShortcutsPlugin#selectIcon}, called directly with a plain Context —
 * no plugin instance, no execute().
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class IconSelectionTest {

    private Context context;
    private int appIconResId;
    private int testDrawableId;

    @Before
    public void setUp() {
        context = RuntimeEnvironment.getApplication();
        appIconResId = TestFixtures.seedApplicationIcon(context);
        testDrawableId = TestFixtures.testDrawableId(context);
        assertNotEquals("the fixtures must be distinct for these assertions to mean anything",
            appIconResId, testDrawableId);
    }

    private IconCompat select(String jsonFields)
            throws JSONException, PackageManager.NameNotFoundException {
        return ShortcutsPlugin.selectIcon(context, new JSONObject("{" + jsonFields + "}"));
    }

    private static String bitmapField(String base64) {
        return "\"iconBitmap\": \"" + base64 + "\"";
    }

    @Test
    public void bitmap_becomesABitmapIcon() throws Exception {
        IconCompat icon = select(bitmapField(TestFixtures.ONE_BY_ONE_PNG_BASE64));

        assertEquals(IconCompat.TYPE_BITMAP, icon.getType());
    }

    @Test
    public void bitmap_takesPrecedenceOverAResourceAndTheApplicationIcon() throws Exception {
        IconCompat icon = select(bitmapField(TestFixtures.ONE_BY_ONE_PNG_BASE64)
            + ", \"iconFromResource\": \"" + TestFixtures.TEST_DRAWABLE_NAME + "\"");

        assertEquals(IconCompat.TYPE_BITMAP, icon.getType());
    }

    @Test
    public void adaptiveFlagWithBitmap_becomesAnAdaptiveIcon() throws Exception {
        IconCompat icon = select(bitmapField(TestFixtures.ADAPTIVE_48_PNG_BASE64)
            + ", \"iconAdaptiveBitmap\": true");

        assertEquals(IconCompat.TYPE_ADAPTIVE_BITMAP, icon.getType());
    }

    /**
     * IconCompat is not API-gated: it does the safe-zone rendering itself on pre-26, so the
     * compat icon stays adaptive-typed even at SDK 25. Pins the removal of the old SDK guard.
     */
    @Test
    @Config(sdk = 25)
    public void adaptiveFlagWithBitmap_staysAdaptiveBelowSdk26() throws Exception {
        IconCompat icon = select(bitmapField(TestFixtures.ADAPTIVE_48_PNG_BASE64)
            + ", \"iconAdaptiveBitmap\": true");

        assertEquals(IconCompat.TYPE_ADAPTIVE_BITMAP, icon.getType());
    }

    /** The adaptive flag only qualifies a bitmap; on its own it must not derail the chain. */
    @Test
    public void adaptiveFlagWithoutBitmap_fallsThroughToTheResource() throws Exception {
        IconCompat icon = select("\"iconAdaptiveBitmap\": true, \"iconFromResource\": \""
            + TestFixtures.TEST_DRAWABLE_NAME + "\"");

        assertEquals(IconCompat.TYPE_RESOURCE, icon.getType());
        assertEquals(testDrawableId, icon.getResId());
    }

    @Test
    public void adaptiveFlagAlone_fallsThroughToTheApplicationIcon() throws Exception {
        IconCompat icon = select("\"iconAdaptiveBitmap\": true");

        assertEquals(IconCompat.TYPE_RESOURCE, icon.getType());
        assertEquals(appIconResId, icon.getResId());
    }

    @Test
    public void resource_takesPrecedenceOverTheApplicationIcon() throws Exception {
        IconCompat icon = select("\"iconFromResource\": \"" + TestFixtures.TEST_DRAWABLE_NAME + "\"");

        assertEquals(IconCompat.TYPE_RESOURCE, icon.getType());
        assertEquals(testDrawableId, icon.getResId());
        assertNotEquals(appIconResId, icon.getResId());
    }

    @Test
    public void missingResource_fallsBackToTheApplicationIcon() throws Exception {
        IconCompat icon = select("\"iconFromResource\": \"does_not_exist\"");

        assertEquals(IconCompat.TYPE_RESOURCE, icon.getType());
        assertEquals(appIconResId, icon.getResId());
    }

    @Test
    public void noIconFields_useTheApplicationIcon() throws Exception {
        IconCompat icon = select("\"id\": \"one\"");

        assertEquals(IconCompat.TYPE_RESOURCE, icon.getType());
        assertEquals(appIconResId, icon.getResId());
    }

    /** Empty strings are indistinguishable from absent fields (the chain tests length > 0). */
    @Test
    public void emptyIconFields_useTheApplicationIcon() throws Exception {
        IconCompat icon = select("\"iconBitmap\": \"\", \"iconFromResource\": \"\"");

        assertEquals(IconCompat.TYPE_RESOURCE, icon.getType());
        assertEquals(appIconResId, icon.getResId());
    }
}
