package com.vmovier.player;

import android.content.Context;
import android.util.DisplayMetrics;

import com.vmovier.lib.utils.PlayerLog;


public class BaseUtil {
    public final static String TAG = "BaseUtil";

    /**
     * 根据手机的分辨率从 px(像素) 的单位 转成为 dp
     * @param context
     * @param pxValue    像素值
     * @return  dp值
     */
    public static int px2dip(Context context, float pxValue) {
        float scale = context.getResources().getDisplayMetrics().density;
        return (int) (pxValue / scale + 0.5f);
    }

    /**
     *
     * @param context
     * @param dipValue   dp值
     * @return  像素值
     */
    public static int dip2px(Context context, float dipValue){
        float scale = context.getResources().getDisplayMetrics().density;
        return (int)(dipValue * scale + 0.5f);
    }

    /**
     * 获取屏幕宽度
     * @param context
     * @return  返回宽度
     */
    public static int getScreenWidth(Context context){
        if (context == null)
            return 0;

        int screenWidth = 0;
        DisplayMetrics dm = context.getResources().getDisplayMetrics();
        if (dm != null){
            screenWidth = dm.widthPixels;
            PlayerLog.d(TAG, "screenWidth: " + screenWidth);
            return screenWidth;
        }

        return 0;
    }

    /**
     * 获取屏幕高度
     * @param context
     * @return  返回高度
     */
    public static int getScreenHeight(Context context){
        if (context == null)
            return 0;

        int screenHeight = 0;
        DisplayMetrics dm = context.getResources().getDisplayMetrics();
        if (dm != null){
            screenHeight = dm.heightPixels;
            PlayerLog.d(TAG, "screenHeight: " + screenHeight);
            return screenHeight;
        }
        return 0;
    }

}