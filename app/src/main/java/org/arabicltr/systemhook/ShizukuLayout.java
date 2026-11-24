package org.arabicltr.systemhook;

import android.view.View;
import android.view.Window;

final class ShizukuLayout {
    private static final String PLUS = "af.shizuku.plus.api";
    private static final String DROP_IN = "moe.shizuku.privileged.api";

    private ShizukuLayout() {}

    static boolean isCandidateProcess(String process) {
        return PLUS.equals(process) || DROP_IN.equals(process)
                || process.startsWith(PLUS + ":") || process.startsWith(DROP_IN + ":");
    }

    static boolean isManager(String process, String applicationClass) {
        return (PLUS.equals(process) || DROP_IN.equals(process))
                && "af.shizuku.manager.ShizukuApplication".equals(applicationClass);
    }

    static void apply(Window window) {
        if (window != null) {
            // Set the raw direction even if the inherited value is currently LTR.
            window.getDecorView().setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        }
    }
}
