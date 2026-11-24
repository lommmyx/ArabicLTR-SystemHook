package org.arabicltr.systemhook;

import android.app.Activity;
import android.app.Dialog;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Parcel;
import android.os.LocaleList;
import android.util.Log;
import android.view.View;

import io.github.libxposed.api.XposedInterface.ExceptionMode;
import io.github.libxposed.api.XposedInterface.HookHandle;
import io.github.libxposed.api.XposedInterface.Hooker;
import io.github.libxposed.api.XposedModule;
import io.github.libxposed.api.XposedModuleInterface.HotReloadedParam;
import io.github.libxposed.api.XposedModuleInterface.HotReloadingParam;
import io.github.libxposed.api.XposedModuleInterface.ModuleLoadedParam;

import io.github.libxposed.api.XposedModuleInterface.PackageReadyParam;

import java.lang.reflect.Method;
import java.util.Locale;

/**
 * Forces locale-derived Android UI layout direction to LTR while keeping the
 * device language and locale-based text heuristics available to text rendering.
 *
 * Targets libxposed API 102 (Vector v2.2-compatible API surface).
 *
 * ART deoptimization is deliberately not attempted blindly: libxposed requires
 * deoptimizing callers of hooked methods, not the hooked callee itself. This
 * module does not guess OEM caller methods; it instead hooks Configuration's
 * locale mutators and normal layout-direction accessors directly.
 */
public final class MainHook extends XposedModule {
    private static final String TAG = "ArabicLTR";

    private static final String ID_CONFIG_GET_DIR = "arabic-ltr-config-get-dir";
    private static final String ID_CONFIG_SET_DIR = "arabic-ltr-config-set-dir";
    private static final String ID_CONFIG_SET_LOCALE = "arabic-ltr-config-set-locale";
    private static final String ID_CONFIG_SET_LOCALES = "arabic-ltr-config-set-locales";
    private static final String ID_CONFIG_SET_TO = "arabic-ltr-config-set-to";
    private static final String ID_CONFIG_UPDATE_FROM = "arabic-ltr-config-update-from";
    private static final String ID_CONFIG_READ_PARCEL = "arabic-ltr-config-read-parcel";

    private static final String ID_ACTIVITY_WINDOW = "arabic-ltr-shizuku-activity";
    private static final String ID_DIALOG_WINDOW = "arabic-ltr-shizuku-dialog";
    private String processName;

    private final Hooker activityWindowHooker = chain -> {
        Object result = chain.proceed();
        ShizukuLayout.apply(((Activity) chain.getThisObject()).getWindow());
        return result;
    };

    private final Hooker dialogWindowHooker = chain -> {
        Object result = chain.proceed();
        ShizukuLayout.apply(((Dialog) chain.getThisObject()).getWindow());
        return result;
    };

    private final Hooker returnLtrHooker =
            chain -> Integer.valueOf(View.LAYOUT_DIRECTION_LTR);

    private final Hooker forceConfigLtrHooker = chain -> {
        Object result = chain.proceed();
        Object receiver = chain.getThisObject();
        if (receiver instanceof Configuration) {
            forceScreenLayoutLtr((Configuration) receiver);
        }
        return result;
    };

    private final Hooker sanitizeRtlConfigHooker = chain -> {
        Object result = chain.proceed();
        Object receiver = chain.getThisObject();
        if (receiver instanceof Configuration) {
            replaceRtlWithLtr((Configuration) receiver);
        }
        return result;
    };

    @Override
    public void onModuleLoaded(ModuleLoadedParam param) {
        processName = param.getProcessName();
        if (!ShizukuLayout.isCandidateProcess(processName)) {
            installConfigurationHooks(processName);
        }
    }

    @Override
    public void onPackageReady(PackageReadyParam param) {
        if (!param.isFirstPackage() || !param.getPackageName().equals(processName)
                || !ShizukuLayout.isManager(processName, param.getApplicationInfo().className)) {
            return;
        }
        installConfigurationHooks(processName);
        hookWindow(Activity.class, "onPostCreate", ID_ACTIVITY_WINDOW, activityWindowHooker, Bundle.class);
        hookWindow(Dialog.class, "onStart", ID_DIALOG_WINDOW, dialogWindowHooker);
    }

    private void hookWindow(Class<?> owner, String methodName, String id, Hooker hooker,
                            Class<?>... parameterTypes) {
        try {
            hook(owner.getDeclaredMethod(methodName, parameterTypes))
                    .setId(id)
                    .setExceptionMode(ExceptionMode.PROTECTIVE)
                    .intercept(hooker);
            log(Log.INFO, TAG, "Shizuku+ " + owner.getSimpleName() + "." + methodName + " in " + processName);
        } catch (NoSuchMethodException e) {
            log(Log.WARN, TAG, owner.getName() + "." + methodName + " unavailable", e);
        }
    }

    private void installConfigurationHooks(String process) {
        hookConfigurationGetLayoutDirection(process);
        hookConfigurationSetLayoutDirection(process);
        hookConfigurationSetLocale(process);
        hookConfigurationSetLocales(process);
        hookConfigurationSetTo(process);
        hookConfigurationUpdateFrom(process);
        hookConfigurationReadFromParcel(process);

        log(Log.INFO, TAG, "Loaded in " + process + " (api=" + getApiVersion() + ")");
    }

    @Override
    public boolean onHotReloading(HotReloadingParam param) {
        return true;
    }

    @Override
    public void onHotReloaded(HotReloadedParam param) {
        processName = param.getProcessName();
        for (HookHandle old : param.getOldHookHandles()) {
            try {
                Hooker replacement = resolveHookerForId(old.getId());
                if (replacement != null) {
                    old.replaceHook(replacement);
                } else {
                    old.unhook();
                }
            } catch (IllegalStateException ignored) {
                // Handle was already invalidated.
            } catch (Throwable error) {
                log(Log.WARN, TAG, "Hot reload replace failed for " + old.getId(), error);
            }
        }
        log(Log.INFO, TAG, "Hot reload hook replacement pass completed");
    }

    private Hooker resolveHookerForId(String id) {
        if (id == null) return null;
        if (ID_ACTIVITY_WINDOW.equals(id)) return activityWindowHooker;
        if (ID_DIALOG_WINDOW.equals(id)) return dialogWindowHooker;
        if (ID_CONFIG_GET_DIR.equals(id)) {
            return returnLtrHooker;
        }
        if (ID_CONFIG_SET_DIR.equals(id)
                || ID_CONFIG_SET_LOCALE.equals(id)
                || ID_CONFIG_SET_LOCALES.equals(id)) {
            return forceConfigLtrHooker;
        }
        if (ID_CONFIG_SET_TO.equals(id)
                || ID_CONFIG_UPDATE_FROM.equals(id)
                || ID_CONFIG_READ_PARCEL.equals(id)) {
            return sanitizeRtlConfigHooker;
        }
        return null;
    }

    /**
     * Standard Configuration getter used by many Android UI code paths.
     * Samsung-specific consumers must still be checked on the target firmware.
     */
    private void hookConfigurationGetLayoutDirection(String process) {
        try {
            Method method = Configuration.class.getDeclaredMethod("getLayoutDirection");

            hook(method)
                    .setId(ID_CONFIG_GET_DIR)
                    .setExceptionMode(ExceptionMode.PROTECTIVE)
                    .intercept(returnLtrHooker);

            log(Log.INFO, TAG, "Configuration.getLayoutDirection hooked in " + process);
        } catch (Throwable error) {
            log(Log.ERROR, TAG, "Could not hook Configuration.getLayoutDirection in " + process, error);
        }
    }

    /**
     * Configuration.setLayoutDirection(Locale) writes the layout direction bits
     * into Configuration.screenLayout. Force those bits to LTR after the original runs.
     */
    private void hookConfigurationSetLayoutDirection(String process) {
        try {
            Method method = Configuration.class.getDeclaredMethod(
                    "setLayoutDirection", Locale.class);

            hook(method)
                    .setId(ID_CONFIG_SET_DIR)
                    .setExceptionMode(ExceptionMode.PROTECTIVE)
                    .intercept(forceConfigLtrHooker);

            log(Log.INFO, TAG, "Configuration.setLayoutDirection hooked in " + process);
        } catch (Throwable error) {
            log(Log.ERROR, TAG, "Could not hook Configuration.setLayoutDirection in " + process, error);
        }
    }

    /**
     * Configuration.setLocale(Locale) delegates to setLocales() on AOSP. Hooking
     * the public mutator after it returns gives us a second enforcement point
     * even if ART inlines the internal setLayoutDirection(Locale) call.
     */
    private void hookConfigurationSetLocale(String process) {
        try {
            Method method = Configuration.class.getDeclaredMethod("setLocale", Locale.class);
            hook(method)
                    .setId(ID_CONFIG_SET_LOCALE)
                    .setExceptionMode(ExceptionMode.PROTECTIVE)
                    .intercept(forceConfigLtrHooker);
            log(Log.INFO, TAG, "Configuration.setLocale hooked in " + process);
        } catch (Throwable error) {
            log(Log.WARN, TAG, "Could not hook Configuration.setLocale in " + process, error);
        }
    }

    /**
     * Configuration.setLocales(LocaleList) chooses layout direction from the
     * first locale. Post-processing this method preserves LocaleList while
     * changing only the layout-direction bits.
     */
    private void hookConfigurationSetLocales(String process) {
        try {
            Method method = Configuration.class.getDeclaredMethod("setLocales", LocaleList.class);
            hook(method)
                    .setId(ID_CONFIG_SET_LOCALES)
                    .setExceptionMode(ExceptionMode.PROTECTIVE)
                    .intercept(forceConfigLtrHooker);
            log(Log.INFO, TAG, "Configuration.setLocales hooked in " + process);
        } catch (Throwable error) {
            log(Log.WARN, TAG, "Could not hook Configuration.setLocales in " + process, error);
        }
    }

    /**
     * Sanitizes RTL bits when a Configuration is copied from another instance.
     * Leaves Configuration.SCREENLAYOUT_LAYOUTDIR_UNDEFINED untouched.
     */
    private void hookConfigurationSetTo(String process) {
        try {
            Method method = Configuration.class.getDeclaredMethod(
                    "setTo", Configuration.class);

            hook(method)
                    .setId(ID_CONFIG_SET_TO)
                    .setExceptionMode(ExceptionMode.PROTECTIVE)
                    .intercept(sanitizeRtlConfigHooker);
        } catch (Throwable error) {
            log(Log.WARN, TAG, "Could not hook Configuration.setTo in " + process, error);
        }
    }

    /**
     * Sanitizes RTL bits when a Configuration delta is applied via updateFrom().
     */
    private void hookConfigurationUpdateFrom(String process) {
        try {
            Method method = Configuration.class.getDeclaredMethod(
                    "updateFrom", Configuration.class);

            hook(method)
                    .setId(ID_CONFIG_UPDATE_FROM)
                    .setExceptionMode(ExceptionMode.PROTECTIVE)
                    .intercept(sanitizeRtlConfigHooker);
        } catch (Throwable error) {
            log(Log.WARN, TAG, "Could not hook Configuration.updateFrom in " + process, error);
        }
    }

    /**
     * Sanitizes RTL bits on Configuration instances received across Binder IPC
     * (a possible path for SystemUI, Settings, and launcher processes receiving configurations).
     */
    private void hookConfigurationReadFromParcel(String process) {
        try {
            Method method = Configuration.class.getDeclaredMethod(
                    "readFromParcel", Parcel.class);

            hook(method)
                    .setId(ID_CONFIG_READ_PARCEL)
                    .setExceptionMode(ExceptionMode.PROTECTIVE)
                    .intercept(sanitizeRtlConfigHooker);
        } catch (Throwable error) {
            log(Log.WARN, TAG, "Could not hook Configuration.readFromParcel in " + process, error);
        }
    }

    private static void forceScreenLayoutLtr(Configuration configuration) {
        configuration.screenLayout =
                (configuration.screenLayout & ~Configuration.SCREENLAYOUT_LAYOUTDIR_MASK)
                        | Configuration.SCREENLAYOUT_LAYOUTDIR_LTR;
    }

    private static void replaceRtlWithLtr(Configuration configuration) {
        // Keep UNDEFINED as UNDEFINED; getLayoutDirection() returns LTR for
        // UNDEFINED on Android. Only rewrite the explicit RTL bit pattern.
        if ((configuration.screenLayout & Configuration.SCREENLAYOUT_LAYOUTDIR_MASK)
                == Configuration.SCREENLAYOUT_LAYOUTDIR_RTL) {
            forceScreenLayoutLtr(configuration);
        }
    }
}
