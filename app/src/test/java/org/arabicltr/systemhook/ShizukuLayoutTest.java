package org.arabicltr.systemhook;

import android.app.Activity;
import android.app.Dialog;
import android.content.pm.ApplicationInfo;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33, qualifiers = "ar-ldrtl")
public class ShizukuLayoutTest {
    @Test public void selectsBothManagersAndExcludesOtherPackagesAndServices() {
        String app = "af.shizuku.manager.ShizukuApplication";
        assertTrue(ShizukuLayout.isManager("af.shizuku.plus.api", app));
        assertTrue(ShizukuLayout.isManager("moe.shizuku.privileged.api", app));
        assertFalse(ShizukuLayout.isManager("moe.shizuku.privileged.api", "other.Application"));
        assertFalse(ShizukuLayout.isManager("af.shizuku.plus.api:service", app));
        assertFalse(ShizukuLayout.isManager("unrelated.app", app));
    }

    @Test public void activityAndDialogChildrenInheritLtrWithArabicText() {
        RuntimeEnvironment.getApplication().getApplicationInfo().flags |= ApplicationInfo.FLAG_SUPPORTS_RTL;
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
        verifyFirstLayout(activity, false);
        Activity reopened = Robolectric.buildActivity(Activity.class).setup().get();
        verifyFirstLayout(reopened, true);

        Dialog dialog = new Dialog(activity);
        FrameLayout dialogContent = new FrameLayout(activity);
        dialog.setContentView(dialogContent);
        dialog.getWindow().getDecorView().setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        ShizukuLayout.apply(dialog.getWindow());
        dialog.show();
        layout(dialog.getWindow().getDecorView());
        assertEquals(View.LAYOUT_DIRECTION_LTR, dialogContent.getLayoutDirection());
        dialog.dismiss();
    }

    private static void verifyFirstLayout(Activity activity, boolean enableModule) {
        FrameLayout host = new FrameLayout(activity);
        TextView label = new TextView(activity);
        label.setText("عربي Android 17");
        label.setTextDirection(View.TEXT_DIRECTION_LOCALE);
        host.addView(label, new FrameLayout.LayoutParams(120, 48, Gravity.START));
        activity.setContentView(host);
        View root = activity.getWindow().getDecorView();
        root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        if (enableModule) {
            ShizukuLayout.apply(activity.getWindow());
        }
        layout(root);
        int expected = enableModule ? View.LAYOUT_DIRECTION_LTR : View.LAYOUT_DIRECTION_RTL;
        assertEquals(expected, host.getLayoutDirection());
        assertEquals(expected, label.getLayoutDirection());
        assertTrue(host.getWidth() > label.getWidth());
        assertEquals(enableModule ? 0 : host.getWidth() - label.getWidth(), label.getLeft());
        assertEquals(View.TEXT_DIRECTION_LOCALE, label.getTextDirection());
        assertEquals("ar", label.getResources().getConfiguration().getLocales().get(0).getLanguage());
        assertEquals("عربي Android 17", label.getText().toString());
    }

    private static void layout(View root) {
        int size = View.MeasureSpec.makeMeasureSpec(600, View.MeasureSpec.EXACTLY);
        root.measure(size, size);
        root.layout(0, 0, 600, 600);
    }
}
