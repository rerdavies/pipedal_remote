package com.twoplay.pipedal;

/**
 * Copyright (c) 2015, sRobin Davies
 * Created by Robin on 18/09/2024.
 */
import android.content.Context;
import android.content.res.Configuration;
import androidx.appcompat.app.AppCompatDelegate;

public class ThemeUtils {

    public static boolean isDarkModeEnabled(Context context) {
        int currentNightMode = context.getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK;
        return currentNightMode == Configuration.UI_MODE_NIGHT_YES;
    }

    public enum ColorTheme { // must match DarkMode.tsx:ColorTheme
        Light,
        Dark,
        System;

        int toInt() {
            switch (this)
            {
                case Light: return 0;
                case Dark: return 1;
                default:
                case System: return 2;
            }
        }
        static ColorTheme fromInt(int value)
        {
            switch (value) {
                case 0:
                    return Light;
                case 1:
                    return Dark;
                case 2:
                default:
                    return System;
            }
        }
    };

    public static void loadUserPreferredTheme(Context context)
    {
        ColorTheme theme =  Preferences.getNightMode(context);
        setUserPreferredTheme(theme);
    }
    public static int getUserPreferredThemeResourceId() {
        switch (AppCompatDelegate.getDefaultNightMode()) {
            case AppCompatDelegate.MODE_NIGHT_NO:
                return R.style.Theme_PiPedal_LightMode; // Your light theme
            case AppCompatDelegate.MODE_NIGHT_YES:
                return R.style.Theme_PiPedal_DarkMode;  // Your dark theme
            case AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM:
            default:
                return R.style.Theme_PiPedal; // Your default theme
        }
    }

    public static ColorTheme getUserPreferedTheme() {
        switch (AppCompatDelegate.getDefaultNightMode()) {
            case AppCompatDelegate.MODE_NIGHT_NO:
                return ColorTheme.Light;
            case AppCompatDelegate.MODE_NIGHT_YES:
                return ColorTheme.Dark;  // Your dark theme
            case AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM:
            default:
                return ColorTheme.System; // Your default theme
        }
    }
    public static interface UserPreferredThemeChangedListener {
        public void onThemeChanged(ColorTheme theme);
    };
    private static UserPreferredThemeChangedListener listener;
    public static void setUserPreferredThemeChangeListener(
            UserPreferredThemeChangedListener listener
    )
    {
        ThemeUtils.listener = listener;
    }
    public static void setUserPreferredTheme(ColorTheme nightMode) {
        if (nightMode == getUserPreferedTheme())
        {
            return;
        }
        Preferences.setNightMode(PiPedalApplication.getContext(),nightMode);

        switch (nightMode) {
            case Light: // Light mode
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
            case Dark: // Dark mode
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
            case System: // System default
            default:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                break;
        }
        if (listener != null)
        {
            listener.onThemeChanged(nightMode);
        }
    }
}