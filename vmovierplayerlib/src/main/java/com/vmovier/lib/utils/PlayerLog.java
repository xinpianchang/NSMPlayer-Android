package com.vmovier.lib.utils;

import android.util.Log;

@SuppressWarnings("WeakerAccess, unused")
public class PlayerLog {
    public static final int VERBOSE = Log.VERBOSE;
    public static final int DEBUG = Log.DEBUG;
    public static final int INFO = Log.INFO;
    public static final int WARN = Log.WARN;
    public static final int ERROR = Log.ERROR;

    private static final String DTAG = "VPlayer";
    private static boolean mSwitch = true;

    private static int logLevel = VERBOSE;

    public static void setLogLevel(int level) {
        logLevel = level;
    }

    /**
     * 输出信息时使用
     *
     * @param TAG 类名
     * @param msg 消息内容
     */
    public static void i(String TAG, String msg) {
        if (mSwitch && logLevel <= INFO)
            Log.i(TAG, msg);
    }

    /**
     * Debug时使用
     *
     * @param TAG 类名
     * @param msg 消息内容
     */
    public static void d(String TAG, String msg) {
        if (mSwitch && logLevel <= DEBUG)
            Log.d(TAG, msg);
    }

    /**
     * Verbose时使用
     *
     * @param TAG 类名
     * @param msg 消息内容
     */
    public static void v(String TAG, String msg) {
        if (mSwitch && logLevel <= VERBOSE)
            Log.v(TAG, msg);
    }

    /**
     * Warning时使用
     *
     * @param TAG 类名
     * @param msg 消息内容
     */
    public static void w(String TAG, String msg) {
        if (mSwitch && logLevel <= WARN)
            Log.w(TAG, msg);
    }

    /**
     * 输出错误信息时使用
     *
     * @param TAG 类名
     * @param msg 消息内容
     */
    public static void e(String TAG, String msg) {
        if (mSwitch && logLevel <= ERROR)
            Log.e(TAG, msg);
    }

    /**
     * 输出错误信息时使用
     *
     * @param TAG 类名
     * @param msg 消息内容
     * @param t   异常
     */
    public static void e(String TAG, String msg, Throwable t) {
        if (mSwitch && logLevel <= ERROR)
            Log.e(TAG, msg, t);
    }

    /**
     * @param msg 消息内容
     */
    public static void v(String msg) {
        v(DTAG, msg);
    }

    /**
     * @param msg 消息内容
     */
    public static void d(String msg) {
        d(DTAG, msg);
    }

    /**
     * @param msg 消息内容
     */
    public static void i(String msg) {
        i(DTAG, msg);
    }

    /**
     * @param msg 消息内容
     */
    public static void w(String msg) {
        w(DTAG, msg);
    }

    /**
     * @param msg 消息内容
     */
    public static void e(String msg) {
        e(DTAG, msg);
    }

    /**
     * @param msg 消息内容
     * @param t   异常
     */
    public static void e(String msg, Throwable t) {
        e(DTAG, msg, t);
    }

    public static void println(int level, String tag, String msg) {
        if (logLevel <= level) Log.println(level, tag, msg);
    }

    /**
     * 开启LOG
     */
    public static void switchOn() {
        mSwitch = true;
    }

    /**
     * 关闭LOG
     */
    public static void switchOff() {
        mSwitch = false;
    }

    /**
     * 使用Android Debug Log打印当前调用此log的方法名称
     */
    public static void logMethod() {
        if (mSwitch) {
            StackTraceElement[] stes = Thread.currentThread().getStackTrace();
            if (stes.length > 4) {
                StackTraceElement ste = stes[3];
                d(DTAG, "[StackTrace] "
                        + ste.getClassName() + "#" + ste.getMethodName());
            }
        }
    }

    public static void logMethod(String tag) {
        if (mSwitch) {
            StackTraceElement[] stes = Thread.currentThread().getStackTrace();
            if (stes.length > 4) {
                StackTraceElement ste = stes[3];
                d(tag, "[StackTrace] "
                        + ste.getClassName() + "#" + ste.getMethodName());
            }
        }
    }

    /**
     * 使用Android Debug Log打印当前调用程序栈
     */
    public static void printStackTrace() {
        if (mSwitch) {
            Log.d(DTAG, Log.getStackTraceString(new Throwable()));
        }
    }

    /**
     * 使用Android Debug Log打印当前调用程序栈
     */
    public static void printStackTrace(String tag) {
        if (mSwitch) {
            Log.d(tag, Log.getStackTraceString(new Throwable()));
        }
    }
}
