package com.zxyw.sdk.tools;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;

import java.util.Set;

public class SPUtils {
    private SPUtils() {
        throw new UnsupportedOperationException("cannot be instantiated");
    }

    private static String FILE_NAME = "DEFAULT_NAME";
    private static Application app;

    public static void init(String name, Application app) {
        FILE_NAME = name;
        SPUtils.app = app;
    }

    public static void put(String key, String value) {
        app.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE).edit().putString(key, value).apply();
    }

    public static void put(String key, int value) {
        app.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE).edit().putInt(key, value).apply();
    }

    public static void put(String key, long value) {
        app.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE).edit().putLong(key, value).apply();
    }

    public static void put(String key, boolean value) {
        app.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE).edit().putBoolean(key, value).apply();
    }

    public static void put(String key, float value) {
        app.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE).edit().putFloat(key, value).apply();
    }

    public static void put(String key, Set<String> value) {
        app.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE).edit().putStringSet(key, value).apply();
    }

    public static String get(String key, String defaultValue) {
        return app.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE).getString(key, defaultValue);
    }

    public static int get(String key, int defaultValue) {
        return app.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE).getInt(key, defaultValue);
    }

    public static boolean get(String key, boolean defaultValue) {
        return app.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE).getBoolean(key, defaultValue);
    }

    public static float get(String key, float defaultValue) {
        return app.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE).getFloat(key, defaultValue);
    }

    public static long get(String key, long defaultValue) {
        return app.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE).getLong(key, defaultValue);
    }

    public static Set<String> get(String key, Set<String> defaultValue) {
        return app.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE).getStringSet(key, defaultValue);
    }

    public static void remove(String key) {
        SharedPreferences sp = app.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.remove(key);
        editor.apply();
    }

    public static void clear() {
        SharedPreferences sp = app.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.clear();
        editor.apply();
    }
}