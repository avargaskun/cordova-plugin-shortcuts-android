package com.plugins.shortcuts;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import android.content.Intent;
import android.content.pm.ShortcutInfo;
import android.os.Bundle;

import org.apache.cordova.PluginResult;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import com.plugins.shortcuts.support.PluginTestHarness;

/**
 * Integration coverage for {@code setDynamic}, driven through the real {@code execute()} and
 * asserted against the shadow ShortcutManager's dynamic shortcut set. Icon branches live in
 * their own phase; every shortcut here falls through to the seeded application icon.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class ShortcutsPluginDynamicShortcutsTest {

    private static final int FLAG_NEW_TASK_CLEAR_TOP =
        Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP;

    private static void assertOk(PluginTestHarness.ExecuteResult result) {
        assertTrue("execute() should report setDynamic as handled", result.handled);
        assertEquals(PluginResult.Status.OK.ordinal(), result.only().getStatus());
    }

    private static void assertError(PluginTestHarness.ExecuteResult result, String message) {
        assertTrue("execute() handles the action even when it fails", result.handled);
        PluginResult pluginResult = result.only();
        assertEquals(PluginResult.Status.ERROR.ordinal(), pluginResult.getStatus());
        assertEquals(message, pluginResult.getStrMessage());
    }

    private static List<ShortcutInfo> dynamicShortcuts(PluginTestHarness harness) {
        return harness.shortcutManager().getDynamicShortcuts();
    }

    private static ShortcutInfo onlyShortcut(PluginTestHarness harness) {
        List<ShortcutInfo> shortcuts = dynamicShortcuts(harness);
        assertEquals("expected exactly one dynamic shortcut", 1, shortcuts.size());
        return shortcuts.get(0);
    }

    /** Publishes a single shortcut whose {@code intent} object is the given JSON, and returns its Intent. */
    private static Intent intentFor(PluginTestHarness harness, String intentJson) {
        assertOk(harness.execute("setDynamic",
            "[{\"id\": \"one\", \"shortLabel\": \"One\", \"intent\": " + intentJson + "}]"));
        return onlyShortcut(harness).getIntent();
    }

    // ---------------------------------------------------------------- happy path

    @Test
    public void setDynamic_mapsEveryFieldOfAFullyPopulatedShortcut() {
        PluginTestHarness harness = new PluginTestHarness();

        assertOk(harness.execute("setDynamic", "["
            + "{"
            + "  \"id\": \"shortcut-1\","
            + "  \"shortLabel\": \"Short\","
            + "  \"longLabel\": \"A considerably longer label\","
            + "  \"intent\": {"
            + "    \"action\": \"com.example.DO_THING\","
            + "    \"categories\": [\"com.example.CAT_ONE\", \"com.example.CAT_TWO\"],"
            + "    \"data\": \"https://example.com/path?q=1\","
            + "    \"flags\": " + Intent.FLAG_ACTIVITY_NEW_TASK + ","
            + "    \"extras\": {\"greeting\": \"hello\", \"count\": 3, \"on\": true}"
            + "  }"
            + "}]"));

        ShortcutInfo shortcut = onlyShortcut(harness);
        assertEquals("shortcut-1", shortcut.getId());
        assertEquals("Short", shortcut.getShortLabel().toString());
        assertEquals("A considerably longer label", shortcut.getLongLabel().toString());

        Intent intent = shortcut.getIntent();
        assertEquals("com.example.DO_THING", intent.getAction());
        assertEquals("https://example.com/path?q=1", intent.getData().toString());
        assertEquals(2, intent.getCategories().size());
        assertTrue(intent.getCategories().contains("com.example.CAT_ONE"));
        assertTrue(intent.getCategories().contains("com.example.CAT_TWO"));
        assertEquals(Intent.FLAG_ACTIVITY_NEW_TASK, intent.getFlags());
        assertEquals(harness.packageName(), intent.getComponent().getPackageName());
        assertEquals(harness.activity.getClass().getName(), intent.getComponent().getClassName());

        Bundle extras = intent.getExtras();
        assertEquals("hello", extras.get("greeting"));
        assertEquals(Integer.valueOf(3), extras.get("count"));
        assertEquals(Boolean.TRUE, extras.get("on"));
    }

    // ---------------------------------------------------- labels and validation

    @Test
    public void setDynamic_longLabelMirrorsShortLabelWhenOnlyShortLabelIsGiven() {
        PluginTestHarness harness = new PluginTestHarness();

        assertOk(harness.execute("setDynamic", "[{\"id\": \"a\", \"shortLabel\": \"Only short\"}]"));

        ShortcutInfo shortcut = onlyShortcut(harness);
        assertEquals("Only short", shortcut.getShortLabel().toString());
        assertEquals("Only short", shortcut.getLongLabel().toString());
    }

    @Test
    public void setDynamic_shortLabelMirrorsLongLabelWhenOnlyLongLabelIsGiven() {
        PluginTestHarness harness = new PluginTestHarness();

        assertOk(harness.execute("setDynamic", "[{\"id\": \"a\", \"longLabel\": \"Only long\"}]"));

        ShortcutInfo shortcut = onlyShortcut(harness);
        assertEquals("Only long", shortcut.getShortLabel().toString());
        assertEquals("Only long", shortcut.getLongLabel().toString());
    }

    @Test
    public void setDynamic_withoutEitherLabel_isAnError() {
        PluginTestHarness harness = new PluginTestHarness();

        assertError(harness.execute("setDynamic", "[{\"id\": \"a\"}]"),
            "A value for either 'shortLabel' or 'longLabel' is required");
        assertTrue(dynamicShortcuts(harness).isEmpty());
    }

    @Test
    public void setDynamic_withoutId_isAnError() {
        PluginTestHarness harness = new PluginTestHarness();

        assertError(harness.execute("setDynamic", "[{\"shortLabel\": \"No id\"}]"),
            "A value for 'id' is required");
        assertTrue(dynamicShortcuts(harness).isEmpty());
    }

    @Test
    public void setDynamic_withANullElement_isAnError() {
        PluginTestHarness harness = new PluginTestHarness();

        assertError(harness.execute("setDynamic", "[null]"), "Shortcut object cannot be null");
        assertTrue(dynamicShortcuts(harness).isEmpty());
    }

    @Test
    public void setDynamic_withAnEmptyArray_publishesNoShortcuts() {
        PluginTestHarness harness = new PluginTestHarness();

        assertOk(harness.execute("setDynamic", "[]"));

        assertTrue(dynamicShortcuts(harness).isEmpty());
    }

    // ------------------------------------------------------------ set semantics

    @Test
    public void setDynamic_publishesEveryShortcutInTheCall() {
        PluginTestHarness harness = new PluginTestHarness();

        assertOk(harness.execute("setDynamic",
            "[{\"id\": \"a\", \"shortLabel\": \"A\"}, {\"id\": \"b\", \"shortLabel\": \"B\"}]"));

        List<ShortcutInfo> shortcuts = dynamicShortcuts(harness);
        assertEquals(2, shortcuts.size());
        assertTrue(shortcuts.stream().anyMatch(s -> "a".equals(s.getId())));
        assertTrue(shortcuts.stream().anyMatch(s -> "b".equals(s.getId())));
    }

    @Test
    public void setDynamic_replacesTheWholeSetRatherThanMerging() {
        PluginTestHarness harness = new PluginTestHarness();
        assertOk(harness.execute("setDynamic",
            "[{\"id\": \"a\", \"shortLabel\": \"A\"}, {\"id\": \"b\", \"shortLabel\": \"B\"}]"));

        assertOk(harness.execute("setDynamic", "[{\"id\": \"c\", \"shortLabel\": \"C\"}]"));

        assertEquals("c", onlyShortcut(harness).getId());
    }

    /** An invalid element must abort before the manager is touched, leaving prior state intact. */
    @Test
    public void setDynamic_leavesThePreviousSetIntactWhenOneElementIsInvalid() {
        PluginTestHarness harness = new PluginTestHarness();
        assertOk(harness.execute("setDynamic", "[{\"id\": \"a\", \"shortLabel\": \"A\"}]"));

        assertError(harness.execute("setDynamic",
            "[{\"id\": \"b\", \"shortLabel\": \"B\"}, {}]"), "A value for 'id' is required");

        assertEquals("a", onlyShortcut(harness).getId());
    }

    // ---------------------------------------------------------- intent mapping

    @Test
    public void intent_actionWithoutADot_isPrefixedWithTheActivityPackage() {
        PluginTestHarness harness = new PluginTestHarness();

        Intent intent = intentFor(harness, "{\"action\": \"DO_THING\"}");

        assertEquals(harness.packageName() + ".DO_THING", intent.getAction());
    }

    @Test
    public void intent_absentAction_defaultsToActionView() {
        PluginTestHarness harness = new PluginTestHarness();

        Intent intent = intentFor(harness, "{}");

        assertEquals(Intent.ACTION_VIEW, intent.getAction());
    }

    @Test
    public void intent_categoryWithoutADot_isPrefixedWhileDottedCategoriesAreKeptAsIs() {
        PluginTestHarness harness = new PluginTestHarness();

        Intent intent = intentFor(harness,
            "{\"categories\": [\"BARE\", \"com.example.DOTTED\"]}");

        assertEquals(2, intent.getCategories().size());
        assertTrue(intent.getCategories().contains(harness.packageName() + ".BARE"));
        assertTrue(intent.getCategories().contains("com.example.DOTTED"));
    }

    @Test
    public void intent_withoutFlags_defaultsToNewTaskAndClearTop() {
        PluginTestHarness harness = new PluginTestHarness();

        Intent intent = intentFor(harness, "{}");

        assertEquals(FLAG_NEW_TASK_CLEAR_TOP, intent.getFlags());
    }

    @Test
    public void intent_explicitFlags_areHonored() {
        PluginTestHarness harness = new PluginTestHarness();

        Intent intent = intentFor(harness,
            "{\"flags\": " + Intent.FLAG_ACTIVITY_SINGLE_TOP + "}");

        assertEquals(Intent.FLAG_ACTIVITY_SINGLE_TOP, intent.getFlags());
    }

    @Test
    public void intent_data_becomesTheIntentUri() {
        PluginTestHarness harness = new PluginTestHarness();

        Intent intent = intentFor(harness, "{\"data\": \"myapp://host/path\"}");

        assertEquals("myapp://host/path", intent.getData().toString());
    }

    @Test
    public void intent_withoutData_hasNoUri() {
        PluginTestHarness harness = new PluginTestHarness();

        Intent intent = intentFor(harness, "{}");

        assertNull(intent.getData());
    }

    @Test
    public void intent_extras_areMappedToTypedIntentExtras() {
        PluginTestHarness harness = new PluginTestHarness();

        Intent intent = intentFor(harness, "{\"extras\": {"
            + "\"flag\": true,"
            + "\"small\": 7,"
            + "\"big\": 4294967296,"
            + "\"real\": 1.5,"
            + "\"text\": \"hello\""
            + "}}");

        Bundle extras = intent.getExtras();
        assertEquals(Boolean.TRUE, extras.get("flag"));
        assertEquals(Integer.valueOf(7), extras.get("small"));
        assertEquals(Long.valueOf(4294967296L), extras.get("big"));
        assertEquals(Double.valueOf(1.5d), extras.get("real"));
        assertEquals("hello", extras.get("text"));
    }

    @Test
    public void intent_nonPrimitiveExtra_isStoredAsItsStringForm() {
        PluginTestHarness harness = new PluginTestHarness();

        Intent intent = intentFor(harness, "{\"extras\": {\"nested\": {\"k\": \"v\"}}}");

        assertEquals("{\"k\":\"v\"}", intent.getExtras().get("nested"));
    }

    @Test
    public void intent_defaultComponent_isTheActivityPackageAndClass() {
        PluginTestHarness harness = new PluginTestHarness();

        Intent intent = intentFor(harness, "{}");

        assertEquals(harness.packageName(), intent.getComponent().getPackageName());
        assertEquals(harness.activity.getClass().getName(), intent.getComponent().getClassName());
    }

    @Test
    public void intent_explicitActivityPackageAndClass_areHonored() {
        PluginTestHarness harness = new PluginTestHarness();

        Intent intent = intentFor(harness,
            "{\"activityPackage\": \"com.other.app\", \"activityClass\": \"com.other.app.Main\"}");

        assertEquals("com.other.app", intent.getComponent().getPackageName());
        assertEquals("com.other.app.Main", intent.getComponent().getClassName());
    }

    /** The explicit package is also what a dot-less action gets prefixed with. */
    @Test
    public void intent_explicitActivityPackage_alsoPrefixesABareAction() {
        PluginTestHarness harness = new PluginTestHarness();

        Intent intent = intentFor(harness,
            "{\"activityPackage\": \"com.other.app\", \"action\": \"DO_THING\"}");

        assertEquals("com.other.app.DO_THING", intent.getAction());
    }
}
