package com.twoplay.pipedal;

import android.app.Application;
import android.content.Context;

import com.google.android.material.color.DynamicColors;

/**
 * Copyright (c) 2022, Robin Davies
 * Created by Robin on 10/04/2022.
 */
public class PiPedalApplication extends Application {
    private static Context context;
    @Override
    public void onCreate() {
        super.onCreate();
        context = getApplicationContext();
        //DynamicColors.applyToActivitiesIfAvailable(this);
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        context = null;
    }

    public static Context getContext() {
        return context;
    }
}
