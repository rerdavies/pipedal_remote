package com.twoplay.pipedal.model;

import androidx.lifecycle.ViewModel;

/**
 * Copyright (c) 2015, sRobin Davies
 * Created by Robin on 11/05/2022.
 */
public class TerminatingViewModel extends ViewModel {
    @Override
    protected void onCleared() {
        super.onCleared();
    }
}
