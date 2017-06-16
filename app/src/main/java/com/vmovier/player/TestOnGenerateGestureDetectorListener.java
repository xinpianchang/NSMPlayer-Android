package com.vmovier.player;

import android.content.Context;
import android.view.GestureDetector;

import com.vmovier.lib.player.IPlayer;
import com.vmovier.lib.view.IPlayerControlView;
import com.vmovier.lib.view.OnGenerateGestureDetectorListener;

/**
 * Created by bb on 2017/5/5.
 */

public class TestOnGenerateGestureDetectorListener implements OnGenerateGestureDetectorListener {
    @Override
    public GestureDetector generateGestureDetector(Context context, IPlayer player, IPlayerControlView controlView) {
        GestureDetector gestureDetector = new GestureDetector(context.getApplicationContext(),
                new TestOnDefaultGestureListener(player, controlView));
        //解决长按屏幕后无法拖动的现象
        gestureDetector.setIsLongpressEnabled(false);
        return gestureDetector;
    }

}
