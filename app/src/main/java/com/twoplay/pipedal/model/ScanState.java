package com.twoplay.pipedal.model;

/**
 * Copyright (c) 2022, Robin Davies
 * Created by Robin on 19/03/2022.
 */

public enum ScanState {
    Uninitialized,
    Searching,
    ErrorState,
    ScanComplete,
    WebViewLoading,
    ViewWeb,

    ConnectionLost,
    ChooseNewDevice,
    SearchingForInstance,
}
