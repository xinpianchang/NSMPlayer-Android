package com.vmovier.lib.utils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.annotation.MainThread;

import java.util.HashSet;
import java.util.Set;

public class ConnectionUtils {

    private static Context ctx;

    private static Set<OnConnectionChangeListener> listeners;

    /**
     * 需要在程序启动时, 第一时间初始化该工具类
     * @param context ApplicationContext
     */
    public static void init(Context context) {
        if (ctx == null) {
            ctx = context;
            listeners = new HashSet<>();
            ctx.registerReceiver(mNetworkBroadcastReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
        }
    }

    private static void checkInitialization() {
        if (ctx == null || listeners == null) throw new IllegalStateException("ConnectionUtils not initialized");
    }

    public static boolean isNetworkConnected() {
        checkInitialization();
        ConnectivityManager cm = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm != null) {
            NetworkInfo info = cm.getActiveNetworkInfo();
            return info != null && info.isConnected();
        }
        return false;
    }

    public static NetworkInfo getActiveNetworkInfo() {
        checkInitialization();
        ConnectivityManager cm = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm != null) {
            return cm.getActiveNetworkInfo();
        }
        return null;
    }

    public static boolean isWifiConnected() {
        checkInitialization();
        ConnectivityManager cm = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm != null) {
            NetworkInfo info = cm.getActiveNetworkInfo();
            return info != null && info.isConnected() &&
                    (info.getType() == ConnectivityManager.TYPE_WIFI ||
                            info.getType() == ConnectivityManager.TYPE_ETHERNET ||
                            info.getType() == ConnectivityManager.TYPE_WIMAX);
        }
        return false;
    }

    public static boolean isMobileConnected() {
        checkInitialization();
        ConnectivityManager cm = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm != null) {
            NetworkInfo info = cm.getActiveNetworkInfo();
            return info != null && info.isConnected() &&
                    (info.getType() == ConnectivityManager.TYPE_MOBILE ||
                            info.getType() == ConnectivityManager.TYPE_MOBILE_DUN ||
                            info.getType() == ConnectivityManager.TYPE_VPN);
        }
        return false;
    }

    /**
     * 仅当监听到网络连接成功后触发一次,只触发唯一一次
     * @param listener 触发监听器
     */
    @MainThread
    public static void triggerOnceUponConnected(final OnConnectedListener listener) {
        register(new OnConnectionChangeListener() {
            @Override
            public void onConnectionChange(Intent connectivityIntent) {
                if (isNetworkConnected()) {
                    unregister(this);
                    listener.onConnected();
                }
            }
        });
    }

    @MainThread
    public static void register(OnConnectionChangeListener listener) {
        listeners.add(listener);
    }

    @MainThread
    public static void unregister(OnConnectionChangeListener listener) {
        listeners.remove(listener);
    }

    public interface OnConnectedListener {
        @MainThread
        void onConnected();
    }

    /**
     * 当网络发生改变时, 使用的监听者对象, 需要通过注册使用, 使用完后需要注销
     */
    public interface OnConnectionChangeListener {
        /**
         * 网络情况更改时提示
         * @param connectivityIntent {@link ConnectivityManager#CONNECTIVITY_ACTION}
         */
        @MainThread
        void onConnectionChange(Intent connectivityIntent);
    }

    @MainThread
    private static void sendNetworkChanged(Intent intent) {
        Set<OnConnectionChangeListener> copyListener = new HashSet<>(listeners);
        for (OnConnectionChangeListener listener : copyListener) {
            listener.onConnectionChange(intent);
        }
    }

    private static BroadcastReceiver mNetworkBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getBooleanExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY, false)) {
                NetworkInfo ni = ConnectionUtils.getActiveNetworkInfo();
                if (ni == null || !ni.isConnectedOrConnecting()) {
                    sendNetworkChanged(intent);
                }
                return;
            }
            if (intent.getBooleanExtra(ConnectivityManager.EXTRA_IS_FAILOVER, false)) {
                sendNetworkChanged(intent);
                return;
            }

            // FIXME some event should not be sent out 优化部分event才需要
            sendNetworkChanged(intent);

        }
    };
}
