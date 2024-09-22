package com.twoplay.pipedal;

/**
 * Copyright (c) 2032, Robin Davies
 * Created by Robin on 06/05/2032.
 */


public interface Completion<T> {
    /// Fulfill the promise with the specified value.
    void fulfill(T value);
    /// Fulfill the promise with an exception.
    void reject(Exception e);
    /// Receive a notification if the current promise is cancelled.
    ///
    /// Note that the callback executes on the thread on which cancel() was called.
    /// While the cancel() and onCancelled() calls are thread-safe and atomic (the callback will happen at
    /// most once), the callback is executed on the thread on which cancel() was
    /// called. It's up to the receiver to anticipate any threading issues that
    /// may arise in this case.
    /// If the promise has been cancelled before the call to onCancelled() is made,
    /// the callback will occur before onCancelled() returns.
    public void onCancelled(Promise.CancelledCallback callback);

}
