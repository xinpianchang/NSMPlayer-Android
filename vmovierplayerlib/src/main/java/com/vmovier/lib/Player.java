package com.vmovier.lib;

import android.content.Context;
import android.os.HandlerThread;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.vmovier.lib.utils.ConnectionUtils;



/**
 * 使用播放器模块  需要主动调用该类init方法
 */
public class Player {
    private static Looper mStateMachineLooper;

    public static void init(@NonNull Context context) {
        init(context, null);
    }

    public static void init (@NonNull Context context,
                             @Nullable Looper looper) {
        ConnectionUtils.init(context);
        // 初始化播放器所需要的Looper.
        if (looper == null) {
            HandlerThread handlerThread = new HandlerThread("player");
            handlerThread.start();
            mStateMachineLooper = handlerThread.getLooper();
        } else {
            mStateMachineLooper = looper;
        }

    }

    public static @NonNull Looper getStateMachineLooper() {
        if (mStateMachineLooper == null) {
            throw new IllegalArgumentException("You must init first.");
        }
        return mStateMachineLooper;
    }

}
