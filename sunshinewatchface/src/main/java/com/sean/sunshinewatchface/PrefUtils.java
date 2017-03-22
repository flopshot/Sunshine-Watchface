package com.sean.sunshinewatchface;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

/**
 * Shared Preferences Utility to get saved weather data
 */

public class PrefUtils {

    private static final String KEY_SYNC_TEMP = "SharedPrefsSyncTemp";
    private static final String KEY_SYNC_ICON = "SharedPrefsSyncIcon";
    private static final String KEY_SYNC_WAITING = "SharedPrefsSyncWaiting";

    private static final String KEY_SYNC_TEMP_DEFAULT = "-- / --";
    private static final String KEY_SYNC_ICON_DEFAULT = "ic_logo";

    public static String getSyncTemp(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getString(KEY_SYNC_TEMP, KEY_SYNC_TEMP_DEFAULT);
    }

    public static String getSyncIconName(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getString(KEY_SYNC_ICON, KEY_SYNC_ICON_DEFAULT);
    }

    public static void setSyncTemp(Context context, String tempString) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(KEY_SYNC_TEMP, tempString);
        editor.apply();
    }

    public static void setSyncIconName(Context context, String iconName) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(KEY_SYNC_ICON, iconName);
        editor.apply();
    }

    public static void setSyncIsWaitingBoolean(Context context, boolean isWaiting) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(KEY_SYNC_WAITING, isWaiting);
        editor.apply();
    }

    public static boolean getSyncIsWaitingBoolean(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean(KEY_SYNC_WAITING, false);
    }

}
