package com.framgia.youralarm1.util;

import android.content.Context;
import android.content.SharedPreferences;

import com.framgia.youralarm1.activity.MainActivity;
import com.framgia.youralarm1.contstant.Const;

/**
 * Created by phongtran on 13/06/2016.
 */
public class PreferenceUtil {
    private Context mContext;
    private SharedPreferences mSettings;


    public PreferenceUtil(Context mContext) {
        this.mContext = mContext;
        mSettings = mContext.getSharedPreferences(Const.PREF_NAME, Context.MODE_PRIVATE);
    }

    public void putStringData(String key, String data) {
        SharedPreferences.Editor editor = mSettings.edit();
        editor.putString(key, data);
        editor.apply();
    }

    public String getStringData(String key) {
        String accountName = mContext.getSharedPreferences(Const.PREF_NAME, Context.MODE_PRIVATE)
                .getString(key, null);
        return accountName;
    }
    public String getStringData(String file, String key) {
        String accountName = mContext.getSharedPreferences(file, Context.MODE_PRIVATE)
                .getString(key, null);
        return accountName;
    }
}
