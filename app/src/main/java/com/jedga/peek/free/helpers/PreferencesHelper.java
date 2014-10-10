package com.jedga.peek.free.helpers;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.util.Map;

public class PreferencesHelper {

    private static final int DEFAULT_TIMEOUT_TIME = 5; // 5 secs
    private static final int DEFAULT_POLLING_TIME = 10; // 10 secs
    private static final String BLACKLIST = "blacklist";

    public static final float DEFAULT_MOTION_THRESHOLD = 0.5f;
    public static final float DEFAULT_PROXIMITY_VALUE = 0f;

    public static final String FIRST_TIME_RUN = "is_first_time_run";
    public static final String PEEK_STATUS = "peek_status";

    // customization
    public static final String CUSTOMIZATION_CATEGORY = "customization";
    public static final String POWER_ON = "power_on";
    public static final String IMMERSIVE_MODE = "immersive";
    public static final String BACKGROUND = "background";
    public static final String REVERSE_GESTURES = "reverse_gestures";
    public static final String SCREEN_TIMEOUT = "screen_timeout";
    public static final String SYSTEM_APPS = "system_apps";

    // sensor
    public static final String WAKE_UP = "wake_up";
    public static final String DETECTION_TYPE = "detection_type";
    public static final String POLLING_TIME = "sensor_polling_time";
    public static final String MOTION_THRESHOLD_VALUE = "motion_threshold_value";
    public static final String PROXIMITY_VALUE = "proximity_value";

    // detection types
    public static final int TYPE_BATTERY_SAVING = 0;
    public static final int TYPE_ALWAYS_ACTIVE = 1;
    public static final int TYPE_ACTIVE_WHILE_CHARGING = 2;

    // security
    public static final String SHOW_NOTIFICATION_TITLE = "show_notification_title";
    public static final String SHOW_NOTIFICATION_CONTENT = "show_notification_content";

    // partners
    public static final String XIONIDIS = "xionidis";
    public static final String ARZ = "arz";
    public static final String HUOT = "huot";
    public static final String TAYLOR = "taylor";

    public static final String XIONIDIS_URL = "http://goo.gl/pv3S04";
    public static final String ARZ_URL = "http://goo.gl/i3AeBe";
    public static final String HUOT_URL = "http://goo.gl/X2Iz0d";
    public static final String TAYLOR_URL = "http://goo.gl/O6kXcI";

    public static void setFirstTimeRun(Context context, boolean firstTime) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(FIRST_TIME_RUN, firstTime);
        editor.commit();
    }

    public static boolean isFirstTimeRun(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean(FIRST_TIME_RUN, true);
    }

    public static void setPeekStatus(Context context, boolean status) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(PEEK_STATUS, status);
        editor.commit();
    }

    public static boolean isPeekEnabled(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean(PEEK_STATUS, true);
    }

    public static void setPackageBlackListed(
            Context context, String packageName, boolean blackList) {
        SharedPreferences prefs = context.getSharedPreferences(BLACKLIST, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(packageName, blackList);
        editor.commit();
    }

    public static boolean isPackageBlackListed(Context context, String packageName) {
        SharedPreferences prefs = context.getSharedPreferences(BLACKLIST, Context.MODE_PRIVATE);
        Map<String, ?> keys = prefs.getAll();
        for (Map.Entry<String, ?> entry : keys.entrySet()) {
            if (entry.getKey().equals(packageName) && (Boolean) entry.getValue()) return true;
        }
        return false;
    }

    // customization
    public static boolean usePowerOnNotifications(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean(POWER_ON, false);
    }

    public static boolean useImmersiveMode(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean(IMMERSIVE_MODE, true);
    }

    public static boolean useBlackBackground(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean(BACKGROUND, false);
    }

    public static boolean useReversedGestures(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean(REVERSE_GESTURES, false);
    }

    public static int getScreenTimeout(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return getIntegerValue(prefs.getString(SCREEN_TIMEOUT,
                String.valueOf(DEFAULT_TIMEOUT_TIME)), DEFAULT_TIMEOUT_TIME);
    }

    public static boolean showSystemApps(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean(SYSTEM_APPS, false);
    }

    // sensors
    public static boolean shouldWakeUp(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean(WAKE_UP, false);
    }

    public static int getDetectionType(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return getIntegerValue(prefs.getString(DETECTION_TYPE,
                String.valueOf(TYPE_BATTERY_SAVING)), TYPE_BATTERY_SAVING);
    }

    public static int getPollingTime(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return getIntegerValue(prefs.getString(POLLING_TIME,
                String.valueOf(DEFAULT_POLLING_TIME)), DEFAULT_POLLING_TIME);
    }

    public static int getIntegerValue(String str, int defaultValue) {
        try {
            return Integer.parseInt(str);
        } catch (NumberFormatException nfe) {
            return defaultValue;
        }
    }

    public static void setMotionThresholdValue(Context context, float value) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putFloat(MOTION_THRESHOLD_VALUE, value);
        editor.commit();
    }

    public static float getMotionThresholdValue(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getFloat(MOTION_THRESHOLD_VALUE, DEFAULT_MOTION_THRESHOLD);
    }

    public static void setProximityValue(Context context, float value) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putFloat(PROXIMITY_VALUE, value);
        editor.commit();
    }

    public static float getProximityValue(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getFloat(PROXIMITY_VALUE, DEFAULT_PROXIMITY_VALUE);
    }

    // security
    public static boolean showNotificationTitleSecure(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean(SHOW_NOTIFICATION_TITLE, false);
    }

    public static boolean showNotificationContentSecure(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean(SHOW_NOTIFICATION_CONTENT, false);
    }
}
