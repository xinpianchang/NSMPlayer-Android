package com.vmovier.lib.view;

import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.view.View;

/**
 * Created by tangye on 16/8/5.
 * View Tag 数据获取器
 */
@SuppressWarnings("WeakerAccess, unused")
public class PlayerViewTagUtils {

    private PlayerViewTagUtils() {}

    /**
     * 获取Tag对象, 如果不存在或者类型不匹配, 直接返回null
     * @param view 指定的view对象
     * @param clazz 指定的获取类型
     * @param <T> 类型T
     * @return 返回Tag对象
     */
    public static <T> T getTag(@NonNull View view, Class<T> clazz) {
        Object o = view.getTag();
        return cast(o, clazz);
    }

    /**
     * 获取Tag对象, 如果不存在或者类型不匹配, 直接返回null
     * @param view 指定的view对象
     * @param id 指定Tag ID
     * @param clazz 指定的获取类型
     * @param <T> 类型T
     * @return 返回Tag对象
     */
    public static <T> T getTag(@NonNull View view, @IdRes int id, Class<T> clazz) {
        Object o = view.getTag(id);
        return cast(o, clazz);
    }

    private static <T> T cast(Object o, Class<T> clazz) {
        if (clazz.isInstance(o)) {
            @SuppressWarnings("unchecked")
            T t = (T) o;
            return t;
        }
        return null;
    }
}
