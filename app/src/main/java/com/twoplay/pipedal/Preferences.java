package com.twoplay.pipedal;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import com.twoplay.pipedal.ThemeUtils;

import com.twoplay.pipedal.model.Model;

import androidx.annotation.NonNull;

/**
 * Copyright (c) 2015, sRobin Davies
 * Created by Robin on 23/04/2022.
 */
public class Preferences {
    @SuppressWarnings("deprecation")
    private static SharedPreferences getSharedPreferences(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context /* Activity context */);
    }
    private static final String KEY_DONT_SHOW_HELP_AGAIN = "dontShowHelpAgain";
    private static final String KEY_SELECTED_SERVER_INSTANCE = "selectedServerInstance";
    private static final String KEY_SELECTED_SERVER_NAME = "selectedServerName";
    private static final String KEY_SELECTED_SERVER_P2P_BSID = "selectedServerP2pBsid";
    private static final String KEY_SELECTED_SERVER_PORT = "selectedServerPort";
    private static final String KEY_P2P_UPNP_WORKING = "p2pupnpWorking2"; // ignore old setting values.
    private static final String KEY_P2P_NIGHT_MODE = "nightMode";

    public static ThemeUtils.ColorTheme getNightMode(Context context)
    {
        return ThemeUtils.ColorTheme.fromInt(getSharedPreferences(context).getInt(KEY_P2P_NIGHT_MODE, ThemeUtils.ColorTheme.System.toInt()));
    }
    public static void setNightMode(Context context, ThemeUtils.ColorTheme colorTheme)
    {
        getSharedPreferences(context).edit().putInt(KEY_P2P_NIGHT_MODE,colorTheme.toInt()).apply();
    }
    public static boolean getDontshowHelpAgain(Context context) {
        return getSharedPreferences(context).getBoolean(KEY_DONT_SHOW_HELP_AGAIN,false);
    }
    public static void setDontShowHelpAgain(Context context, boolean value) {
        getSharedPreferences(context).edit().putBoolean(KEY_DONT_SHOW_HELP_AGAIN,value).apply();
    }
    public static @NonNull String getSelectedWifiDirectBsd(Context context)
    {
        return getSharedPreferences(context).getString(KEY_SELECTED_SERVER_P2P_BSID,"");
    }
    public static @NonNull String getSelectedServerInstanceId (Context context){
        return getSharedPreferences(context).getString(KEY_SELECTED_SERVER_INSTANCE, "");
    }
    public static @NonNull
    String getSelectedServerName(Context context){
        return getSharedPreferences(context).getString(KEY_SELECTED_SERVER_NAME, "");
    }
    public static int getSelectedServerPort(Context context){
        return getSharedPreferences(context).getInt(KEY_SELECTED_SERVER_PORT, -1);
    }

    public static void removeSelectedServer(Context context) {
        getSharedPreferences(context).edit()
                .putString(KEY_SELECTED_SERVER_INSTANCE, "")
                .putString(KEY_SELECTED_SERVER_NAME,"")
                .putString(KEY_SELECTED_SERVER_P2P_BSID,"")
                .putInt(KEY_SELECTED_SERVER_PORT,-1)
                .apply();
    }

    public static void setSelectedServer(Context context, String name,String instanceId, int portNumber){
        getSharedPreferences(context).edit()
                .putString(KEY_SELECTED_SERVER_INSTANCE, instanceId)
                .putString(KEY_SELECTED_SERVER_NAME,name)
                .putInt(KEY_SELECTED_SERVER_PORT,portNumber)
                .apply();
    }

    public static boolean isP2pUpnpWorking(Context context)
    {
        return getSharedPreferences(context).getBoolean(KEY_P2P_UPNP_WORKING,false);
    }
    public static void setIsP2pUpnpWorking(Context context, boolean working) {
        if (isP2pUpnpWorking(context) != working)
        {
            getSharedPreferences(context).edit().putBoolean(KEY_P2P_UPNP_WORKING,working).apply();
        }
    }

}
