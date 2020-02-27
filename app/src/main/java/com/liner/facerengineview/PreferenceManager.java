package com.liner.facerengineview;


import android.content.Context;
import android.content.SharedPreferences;

public class PreferenceManager {
    private static PreferenceManager preferenceManager;
    private SharedPreferences sharedPreferences;

    public static PreferenceManager getInstance(Context context, String name) {
        if (preferenceManager == null) {
            preferenceManager = new PreferenceManager(context, name);
        }
        return preferenceManager;
    }
    public static PreferenceManager getInstance(Context context) {
        if (preferenceManager == null) {
            preferenceManager = new PreferenceManager(context, "Data");
        }
        return preferenceManager;
    }

    private PreferenceManager(Context context, String name) {
        sharedPreferences = context.getSharedPreferences(name,Context.MODE_PRIVATE);
    }

    public void saveString(String key, String value) {
        SharedPreferences.Editor prefsEditor = sharedPreferences.edit();
        prefsEditor.putString(key, value);
        prefsEditor.apply();
    }

    public void saveInt(String key, int value) {
        SharedPreferences.Editor prefsEditor = sharedPreferences.edit();
        prefsEditor.putInt(key, value);
        prefsEditor.apply();
    }

    public void saveBoolean(String key, boolean value) {
        SharedPreferences.Editor prefsEditor = sharedPreferences.edit();
        prefsEditor.putBoolean(key, value);
        prefsEditor.apply();
    }

    public void saveFloat(String key, float value) {
        SharedPreferences.Editor prefsEditor = sharedPreferences.edit();
        prefsEditor.putFloat(key, value);
        prefsEditor.apply();
    }

    public void saveLong(String key, long value){
        SharedPreferences.Editor prefsEditor = sharedPreferences.edit();
        prefsEditor.putLong(key, value);
        prefsEditor.apply();
    }


    public long getLong(String key, long defvalue){
        return sharedPreferences.getLong(key, defvalue);
    }

    public String getString(String key, String defvalue) {
        return sharedPreferences.getString(key, defvalue);
    }

    public int getInt(String key, int defvalue) {
        return sharedPreferences.getInt(key, defvalue);
    }
    public float getFloat(String key, float defvalue) {
        return sharedPreferences.getFloat(key, defvalue);
    }

    public boolean getBool(String key, boolean defvalue) {
        return sharedPreferences.getBoolean(key, defvalue);
    }
}