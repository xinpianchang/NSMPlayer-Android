package com.vmovier.lib.view;

import android.content.Context;
import android.view.GestureDetector;

import com.vmovier.lib.player.IPlayer;

/**
 * 默认GestureDetector生成器
 */
class OnDefaultGenerateGestureDetectorListener implements OnGenerateGestureDetectorListener {
    @Override
    public GestureDetector generateGestureDetector(Context context, IPlayer player, IPlayerControlView controlView) {
        GestureDetector gestureDetector = new GestureDetector(context.getApplicationContext(),
                new OnDefaultGestureListener(player, controlView));
        //解决长按屏幕后无法拖动的现象
        gestureDetector.setIsLongpressEnabled(false);
        return gestureDetector;
    }
}
