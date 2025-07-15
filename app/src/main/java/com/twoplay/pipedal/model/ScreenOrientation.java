package com.twoplay.pipedal.model;

import android.content.pm.ActivityInfo;


public enum ScreenOrientation {
    // values must match pipedal/ScreenDefault.tsx
    SystemDefault,
    Landscape,
    Portrait;

    public int toInt() {
        switch (this) {
            case Landscape:
                return 1;
            case Portrait:
                return 2;
            case SystemDefault:
            default:
                return 0;
        }
    }

    public static ScreenOrientation fromInt(int value) {
        switch (value) {
            case 0:
                return ScreenOrientation.SystemDefault;
            case 1:
                return ScreenOrientation.Landscape;
            case 2:
                return ScreenOrientation.Portrait;
            default:
                return ScreenOrientation.SystemDefault;
        }
    }
    public int getSystemFlags()
    {
        switch (this)
        {
            case Landscape:
                return ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE;
            case Portrait:
                return ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT;
            case SystemDefault:
            default:
                return ActivityInfo.SCREEN_ORIENTATION_FULL_USER;
        }
    }
}
