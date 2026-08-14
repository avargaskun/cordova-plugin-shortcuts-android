package com.plugins.shortcuts.support;

import static org.robolectric.Shadows.shadowOf;

import java.lang.reflect.Method;

import android.content.Context;
import android.content.pm.ShortcutInfo;
import android.graphics.drawable.Icon;

public final class TestFixtures {

    /** A valid 1x1 PNG. Only for non-adaptive bitmap and decodeBase64Bitmap cases. */
    public static final String ONE_BY_ONE_PNG_BASE64 =
        "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNkYPhfDwAChwGA60e6kgAAAABJRU5ErkJggg==";

    /**
     * A valid 48x48 PNG used by every adaptive-bitmap case at any SDK.
     * IconCompat's legacy adaptive rendering sizes its output as (int)(2/3 * min(w,h)),
     * so a 1x1 source computes 0 and Bitmap.createBitmap(0, 0, ...) throws.
     * 48x48 masks down to exactly 32x32.
     */
    public static final String ADAPTIVE_48_PNG_BASE64 =
        "iVBORw0KGgoAAAANSUhEUgAAADAAAAAwCAYAAABXAvmHAAAARElEQVR42u3PQREAAAQAMJ100klaKvi6"
        + "22MBFlk9n4WAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAwNUCvgy6HsJbLkYAAAAASUVORK5CYII=";

    public static final String TEST_DRAWABLE_NAME = "test_icon";

    private TestFixtures() {
    }

    /**
     * Reads the platform {@code ShortcutInfo.getIcon()}, which is {@code @hide} and therefore
     * absent from the public android.jar we compile against but present in Robolectric's
     * android-all jars at runtime.
     */
    public static Icon iconOf(ShortcutInfo info) {
        try {
            Method getIcon = info.getClass().getMethod("getIcon");
            getIcon.setAccessible(true);
            return (Icon) getIcon.invoke(info);
        } catch (Exception e) {
            throw new AssertionError("Could not read ShortcutInfo.getIcon() reflectively", e);
        }
    }

    public static int testDrawableId(Context context) {
        int id = context.getResources().getIdentifier(
            TEST_DRAWABLE_NAME, "drawable", context.getPackageName());
        if (id == 0) {
            throw new AssertionError("Test drawable '" + TEST_DRAWABLE_NAME
                + "' did not resolve for package " + context.getPackageName());
        }
        return id;
    }

    /**
     * Robolectric defaults {@code applicationInfo.icon} to 0, and
     * {@code IconCompat.createWithResource(ctx, 0)} throws — every default/fallback icon path
     * needs a real resId seeded first. Returns the seeded resId.
     */
    public static int seedApplicationIcon(Context context) {
        int resId = testDrawableId(context);
        shadowOf(context.getPackageManager())
            .getInternalMutablePackageInfo(context.getPackageName())
            .applicationInfo.icon = resId;
        return resId;
    }
}
