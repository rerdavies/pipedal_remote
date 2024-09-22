package com.twoplay.pipedal;

import android.os.Handler;

import com.twoplay.pipedal.Completion;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class Promise<T> {

    private Handler handler;
    private boolean cancelled = false;
    private CancelledCallback cancelledCallback;


    public interface CancelledCallback {
        void onCancelled();
    }

    Promise<T> predecessor;

    public void cancel() {
        boolean wasCancelled = false;

        CancelledCallback callback = null;
        synchronized (sync) {
            if (!this.cancelled) {
                wasCancelled = true;
                this.cancelled = true;
                callback = this.cancelledCallback;
                this.cancelledCallback = null;
                this.listener = null;

            }
        }
        if (callback != null) {
            callback.onCancelled();
        }
        if (wasCancelled && predecessor != null) {
            predecessor.cancel();
        }
    }

    public boolean isCancelled() {
        synchronized (sync) {
            return this.cancelled;
        }
    }

    public static interface PromiseFunction<T> {
        void execute(Completion<T> result);
    }

    static Promise<Void> withHandler(Handler handler) {
        Promise<Void> result = new Promise<Void>(
                handler,
                (continuation) -> {
                    continuation.fulfill(null);
                });
        return result;
    }

    public static <U> Promise<U> asyncExec(Handler handler,PromiseFunction<U> fn) {
        return new Promise<U>(handler,(completion)->{
            final ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.execute(() -> {
                // Code to run in background
                fn.execute(completion);
                handler.post(()->{
                    // Don't forget to shutdown the executor
                    executor.shutdown();
                });
            });
        });
    }

//    public static Promise<T> asyncExec(Handler handler, PromiseFunction<T> fn) {
//        Promise<T> result = new Promise<T>(
//                handler,
//                (asyncCompletion)->{
//
//                });
//        return result;
//    }

    public interface Listener<T> {
        void onComplete(Exception exception, T value);
    }

    private Object sync = new Object();
    private Listener<T> listener;
    private boolean hasResult;
    private Exception exception;
    T result;


    void setListener(Listener<T> listener) {
        boolean triggered;
        synchronized (sync) {
            this.listener = listener;
            triggered = hasResult && !cancelled;
        }
        if (triggered) {
            this.listener.onComplete(this.exception, this.result);
        }
    }

    private class CompletionImpl implements Completion<T> {
        @Override
        public void fulfill(T value) {
            boolean notify = false;
            Listener<T> myListener;
            synchronized (sync) {
                if (hasResult) return;
                if (cancelled) return;
                hasResult = true;
                result = value;
                notify = listener != null;
                myListener = listener;
            }
            if (notify) {
                if (handler != null) {
                    handler.post(() -> {
                        Listener<T> myListener2 = null;
                        synchronized (sync) {
                            if (cancelled) return;
                            myListener2 = listener;
                        }
                        if (myListener2 != null) {
                            myListener2.onComplete(exception, result);
                        }
                    });
                } else {
                    myListener.onComplete(exception, result);
                }
            }
        }


        public void onCancelled(CancelledCallback callback) {
            boolean doCallback = false;
            synchronized (sync) {
                cancelledCallback = callback;
                if (cancelled) {
                    doCallback = true;
                }
            }
            if (doCallback) {
                callback.onCancelled();
            }
        }

        @Override
        public void reject(Exception e) {
            boolean notify = false;
            Handler myHandler;
            Listener<T> myListener = null;
            synchronized (sync) {
                myHandler = handler;
                if (hasResult) return;
                if (cancelled) return;
                hasResult = true;
                exception = e;
                notify = listener != null;
                myListener = listener;
            }
            ;
            if (notify) {
                if (myHandler != null) {
                    myHandler.post(() -> {
                        Listener<T> myListener2 = null;

                        synchronized (sync) {
                            if (cancelled) {
                                return;
                            }
                            myListener2 = listener;
                        }
                        if (myListener2 != null) {
                            myListener2.onComplete(exception, result);
                        }
                    });
                } else {
                    if (myListener != null) {
                        myListener.onComplete(exception, result);
                    }
                }
            }
        }
    }

    public Promise(PromiseFunction<T> fn) {
        fn.execute(new CompletionImpl());
    }

    public Promise(Handler handler, PromiseFunction<T> fn) {
        this.handler = handler;
        fn.execute(new CompletionImpl());
    }

    public static <U> Promise<U> EmptyPromise() {
        return new Promise<U>((completion) -> {
            completion.fulfill(null);
        });
    }


    // A promise that is never fulfilled. Like all broken promises, this method is completely useless.
    // Exists purely for the humor value.
    public static <U> Promise<U> BrokenPromise() {
        return new Promise<U>((completion) -> {
        });
    }

    public interface Action<T> {
        void call(T value);
    }

    public interface Function<V, RET> {
        RET call(V value);
    }

    public interface PromiseReturnFunction<V, RET> {
        Promise<RET> call(V value);
    }

    public <RET> Promise<RET> then(PromiseReturnFunction<T, RET> action) {
        Promise<RET> promise = new Promise<RET>(handler, (completion) -> {
            Listener<T> listener = new Listener<T>() {
                @Override
                public void onComplete(Exception exception, T value) {
                    Promise<RET> retVal = action.call(value);
                    retVal.andThen((rv) -> {
                        completion.fulfill(rv);
                    }).andCatch((Exception e) -> {
                        completion.reject(e);
                    });
                }

            };
            setListener(listener);
        });
        return promise;
    }

    public Promise<Void> andThen(Action<T> action) {
        Promise<Void> promise = new Promise<Void>(handler, (completion) -> {
            Listener<T> listener = new Listener<T>() {
                @Override
                public void onComplete(Exception exception, T value) {
                    if (exception != null) {
                        completion.reject(exception);
                    } else {
                        action.call(value);
                        completion.fulfill(null);
                    }
                }

                ;
            };

            boolean triggered = false;
            synchronized (sync) {
                this.listener = listener;
                triggered = hasResult;
            }
            if (triggered) {
                this.listener.onComplete(this.exception, this.result);
            }
        });
        return promise;
    }

    public <RETURN> Promise<RETURN> andThen(Function<T, RETURN> action) {
        Promise<RETURN> promise = new Promise<RETURN>(this.handler, (completion) -> {
            Listener<T> listener = new Listener<T>() {
                @Override
                public void onComplete(Exception exception, T value) {
                    if (exception != null) {
                        completion.reject(exception);
                    } else {
                        RETURN retVal = action.call(value);
                        completion.fulfill(retVal);
                    }
                }
            };
            setListener(listener);
        });
        return promise;
    }

    public interface ExceptionHandler {
        void onException(Exception e);
    }

    public Promise<T> andCatch(ExceptionHandler exceptionHandler) {
        Promise<T> promise = new Promise<T>((completion) -> {
            Listener<T> listener = new Listener<T>() {
                @Override
                public void onComplete(Exception exception, T value) {
                    if (exception != null) {
                        exceptionHandler.onException(exception);
                    } else {
                        completion.fulfill(value);
                    }
                }
            };

            setListener(listener);
        });
        return promise;
    }
}
