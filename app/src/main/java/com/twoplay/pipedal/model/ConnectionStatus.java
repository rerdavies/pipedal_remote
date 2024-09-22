package com.twoplay.pipedal.model;

public enum ConnectionStatus {
    // Must sync with R.array.connection_status
    AvailableOnLocalNetwork, // see string-array connection_status
    Connected,
    Failed,
    Connecting, // = Invited,
    Unavailable,
    ConnectedNoServiceAddress,
    NotConnected,
    WaitingForIpAddress,
    ;

    public static ConnectionStatus fromInt(int value) {
        return ConnectionStatus.values()[value];
    }

    public int toInt() {
        return this.ordinal();
    }
}
