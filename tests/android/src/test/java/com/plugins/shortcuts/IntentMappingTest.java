package com.plugins.shortcuts;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.ComponentName;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import com.plugins.shortcuts.support.TestFixtures;

/**
 * Unit coverage for the extracted mapping helpers: {@code parseIntent}, {@code buildIntent},
 * {@code consumeShortcutExtras} and {@code decodeBase64Bitmap}.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class IntentMappingTest {

    private static final String PKG = "com.test.pkg";
    private static final String CLS = "com.test.pkg.MainActivity";

    private static final int FLAG_NEW_TASK_CLEAR_TOP =
        Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP;

    private static Intent parse(String json) throws JSONException {
        return ShortcutsPlugin.parseIntent(new JSONObject(json), PKG, CLS);
    }

    private static Intent parse(JSONObject json) throws JSONException {
        return ShortcutsPlugin.parseIntent(json, PKG, CLS);
    }

    private static Object extra(Intent intent, String key) {
        Bundle extras = intent.getExtras();
        assertNotNull("intent should carry extras", extras);
        return extras.get(key);
    }

    // ---------------------------------------------------------------- parseIntent

    @Test
    public void parse_withoutAComponent_usesTheSuppliedDefaults() throws Exception {
        Intent intent = parse("{}");

        assertEquals(new ComponentName(PKG, CLS), intent.getComponent());
    }

    @Test
    public void parse_honoursAnExplicitActivityClassAndPackage() throws Exception {
        Intent intent = parse("{\"activityPackage\": \"com.other\","
            + " \"activityClass\": \"com.other.Other\"}");

        assertEquals(new ComponentName("com.other", "com.other.Other"), intent.getComponent());
    }

    @Test
    public void parse_withoutAnAction_defaultsToActionView() throws Exception {
        assertEquals(Intent.ACTION_VIEW, parse("{}").getAction());
    }

    @Test
    public void parse_prefixesADotlessActionWithTheDefaultPackage() throws Exception {
        assertEquals(PKG + ".OPEN", parse("{\"action\": \"OPEN\"}").getAction());
    }

    @Test
    public void parse_prefixesADotlessActionWithAnExplicitActivityPackage() throws Exception {
        Intent intent = parse("{\"action\": \"OPEN\", \"activityPackage\": \"com.other\"}");

        assertEquals("com.other.OPEN", intent.getAction());
    }

    @Test
    public void parse_keepsADottedActionAsIs() throws Exception {
        assertEquals("com.example.DO", parse("{\"action\": \"com.example.DO\"}").getAction());
    }

    @Test
    public void parse_withoutFlags_defaultsToNewTaskAndClearTop() throws Exception {
        assertEquals(FLAG_NEW_TASK_CLEAR_TOP, parse("{}").getFlags());
    }

    @Test
    public void parse_honoursExplicitFlags() throws Exception {
        assertEquals(Intent.FLAG_ACTIVITY_SINGLE_TOP,
            parse("{\"flags\": " + Intent.FLAG_ACTIVITY_SINGLE_TOP + "}").getFlags());
    }

    @Test
    public void parse_prefixesDotlessCategoriesAndKeepsDottedOnes() throws Exception {
        Intent intent = parse("{\"categories\": [\"BARE\", \"com.example.DOTTED\"]}");

        assertEquals(2, intent.getCategories().size());
        assertTrue(intent.getCategories().contains(PKG + ".BARE"));
        assertTrue(intent.getCategories().contains("com.example.DOTTED"));
    }

    @Test
    public void parse_withoutCategories_leavesThemNull() throws Exception {
        assertNull(parse("{}").getCategories());
    }

    @Test
    public void parse_setsTheDataUri() throws Exception {
        Intent intent = parse("{\"data\": \"https://example.com/a?b=c\"}");

        assertEquals(Uri.parse("https://example.com/a?b=c"), intent.getData());
    }

    @Test
    public void parse_withoutData_leavesItNull() throws Exception {
        assertNull(parse("{}").getData());
    }

    @Test
    public void parse_mapsJsonExtrasToTypedIntentExtras() throws Exception {
        Intent intent = parse("{\"extras\": {"
            + "\"aBoolean\": true,"
            + "\"anInteger\": 7,"
            + "\"aLong\": 4294967296,"
            + "\"aDouble\": 2.5,"
            + "\"aString\": \"hello\","
            + "\"anObject\": {\"k\": \"v\"}"
            + "}}");

        assertEquals(Boolean.TRUE, extra(intent, "aBoolean"));
        assertEquals(Integer.valueOf(7), extra(intent, "anInteger"));
        assertEquals(Long.valueOf(4294967296L), extra(intent, "aLong"));
        assertEquals(Double.valueOf(2.5), extra(intent, "aDouble"));
        assertEquals("hello", extra(intent, "aString"));
        assertEquals("{\"k\":\"v\"}", extra(intent, "anObject"));
    }

    /**
     * A Float extra is unreachable through parsed JSON text (org.json produces Double), so the
     * Float branch is only exercisable by putting a real Float into the JSONObject.
     */
    @Test
    public void parse_mapsAFloatExtraToAFloat() throws Exception {
        JSONObject extras = new JSONObject();
        extras.put("aFloat", (Object) Float.valueOf(1.5f));
        JSONObject json = new JSONObject();
        json.put("extras", extras);

        Intent intent = parse(json);

        assertEquals(Float.valueOf(1.5f), extra(intent, "aFloat"));
    }

    @Test
    public void parse_withoutExtras_leavesThemNull() throws Exception {
        assertNull(parse("{}").getExtras());
    }

    // ---------------------------------------------------------------- buildIntent

    @Test
    public void build_alwaysReportsActionAndFlags() throws Exception {
        Intent intent = new Intent("com.example.DO");
        intent.setFlags(FLAG_NEW_TASK_CLEAR_TOP);

        JSONObject json = ShortcutsPlugin.buildIntent(intent);

        assertEquals("com.example.DO", json.getString("action"));
        assertEquals(FLAG_NEW_TASK_CLEAR_TOP, json.getInt("flags"));
    }

    @Test
    public void build_omitsDataCategoriesAndExtrasWhenAbsent() throws Exception {
        JSONObject json = ShortcutsPlugin.buildIntent(new Intent("com.example.DO"));

        assertFalse(json.has("data"));
        assertFalse(json.has("categories"));
        assertFalse(json.has("extras"));
    }

    @Test
    public void build_reportsDataAndCategories() throws Exception {
        Intent intent = new Intent("com.example.DO", Uri.parse("https://example.com/a"));
        intent.addCategory("com.example.CAT");

        JSONObject json = ShortcutsPlugin.buildIntent(intent);

        assertEquals("https://example.com/a", json.getString("data"));
        JSONArray categories = json.getJSONArray("categories");
        assertEquals(1, categories.length());
        assertEquals("com.example.CAT", categories.getString(0));
    }

    @Test
    public void build_mapsEachExtraTypeToItsJsonCounterpart() throws Exception {
        Intent intent = new Intent("com.example.DO");
        intent.putExtra("aBoolean", true);
        intent.putExtra("anInteger", 7);
        intent.putExtra("aLong", 4294967296L);
        intent.putExtra("aFloat", 1.5f);
        intent.putExtra("aDouble", 2.5d);
        intent.putExtra("aString", "hello");

        JSONObject extras = ShortcutsPlugin.buildIntent(intent).getJSONObject("extras");

        assertEquals(true, extras.getBoolean("aBoolean"));
        assertEquals(7, extras.getInt("anInteger"));
        assertEquals(4294967296L, extras.getLong("aLong"));
        // A Float is rendered through Number.toString(), so it reads back as a double.
        assertEquals(1.5d, extras.getDouble("aFloat"), 0d);
        assertEquals(2.5d, extras.getDouble("aDouble"), 0d);
        assertEquals("hello", extras.getString("aString"));
    }

    @Test
    public void build_mapsUnsupportedExtraTypesToTheirToString() throws Exception {
        Intent intent = new Intent("com.example.DO");
        intent.putExtra("aShort", (short) 7);
        intent.putExtra("chars", new char[] { 'a', 'b' });

        JSONObject extras = ShortcutsPlugin.buildIntent(intent).getJSONObject("extras");

        assertEquals("7", extras.getString("aShort"));
        assertTrue(extras.getString("chars").startsWith("[C@"));
    }

    @Test
    public void roundTrip_preservesActionDataCategoriesAndExtras() throws Exception {
        Intent original = new Intent("com.example.DO", Uri.parse("https://example.com/a?b=c"));
        original.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        original.addCategory("com.example.CAT");
        original.putExtra("aBoolean", true);
        original.putExtra("anInteger", 7);
        original.putExtra("aLong", 4294967296L);
        original.putExtra("aDouble", 2.5d);
        original.putExtra("aString", "hello");

        Intent result = parse(ShortcutsPlugin.buildIntent(original));

        assertEquals(original.getAction(), result.getAction());
        assertEquals(original.getData(), result.getData());
        assertEquals(original.getFlags(), result.getFlags());
        assertEquals(original.getCategories(), result.getCategories());
        assertEquals(Boolean.TRUE, extra(result, "aBoolean"));
        assertEquals(Integer.valueOf(7), extra(result, "anInteger"));
        assertEquals(Long.valueOf(4294967296L), extra(result, "aLong"));
        assertEquals(Double.valueOf(2.5), extra(result, "aDouble"));
        assertEquals("hello", extra(result, "aString"));
        // buildIntent does not report the component, so the defaults apply on the way back.
        assertEquals(new ComponentName(PKG, CLS), result.getComponent());
    }

    // ---------------------------------------------------------- consumeShortcutExtras

    @Test
    public void consume_onANullIntent_isANoOp() {
        ShortcutsPlugin.consumeShortcutExtras(null);
    }

    @Test
    public void consume_onAnIntentWithoutExtras_isANoOp() {
        Intent intent = new Intent("com.example.DO");

        ShortcutsPlugin.consumeShortcutExtras(intent);

        assertNull(intent.getExtras());
    }

    @Test
    public void consume_stripsOnlyTheShortcutKeyAndItsNamespace() {
        Intent intent = new Intent("com.example.DO");
        intent.putExtra("shortcut", "one");
        intent.putExtra("shortcut.foo", "bar");
        intent.putExtra("shortcutty", "kept");
        intent.putExtra("other", "kept");

        ShortcutsPlugin.consumeShortcutExtras(intent);

        Bundle extras = intent.getExtras();
        assertNotNull(extras);
        assertFalse(extras.containsKey("shortcut"));
        assertFalse(extras.containsKey("shortcut.foo"));
        assertEquals("kept", extras.getString("shortcutty"));
        assertEquals("kept", extras.getString("other"));
    }

    // ----------------------------------------------------------- decodeBase64Bitmap

    @Test
    public void decode_readsAValidPngFixture() {
        Bitmap bitmap = ShortcutsPlugin.decodeBase64Bitmap(TestFixtures.ONE_BY_ONE_PNG_BASE64);

        assertNotNull(bitmap);
        assertEquals(1, bitmap.getWidth());
        assertEquals(1, bitmap.getHeight());
    }

    /**
     * Characterization, not a contract: Robolectric's BitmapFactory never really decodes, so
     * non-image bytes still yield a stub bitmap at its default 100x100 instead of the null a
     * real device returns. Asserted so a Robolectric upgrade that starts returning null is
     * noticed rather than silently changing what the icon paths see.
     */
    @Test
    public void decode_onNonImageInput_returnsARobolectricStubBitmap() {
        // base64 of "not an image"
        Bitmap bitmap = ShortcutsPlugin.decodeBase64Bitmap("bm90IGFuIGltYWdl");

        assertNotNull(bitmap);
        assertEquals(100, bitmap.getWidth());
        assertEquals(100, bitmap.getHeight());
    }
}
