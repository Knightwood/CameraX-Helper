package com.kiylx.camerax_lib.utils;

import android.os.Handler;
import android.os.Message;

import java.lang.ref.WeakReference;

/**
 * how to use: Declare a static inner class to inherit this class in activity
 */
public abstract class CStaticHandler<T> extends Handler {
    private WeakReference<T> mTargets;

    public CStaticHandler(T target) {
        mTargets = new WeakReference<>(target);
    }

    @Override
    public void handleMessage(Message msg) {
        super.handleMessage(msg);
        T target = mTargets.get();
        if (target != null) {
            handle(target, msg);
        }
    }

    public abstract void handle(T target, Message msg);
}

