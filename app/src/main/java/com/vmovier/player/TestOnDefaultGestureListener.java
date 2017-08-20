package com.vmovier.player;

import android.view.MotionEvent;

import com.vmovier.lib.player.IPlayer;
import com.vmovier.lib.view.IPlayerControlView;
import com.vmovier.lib.view.OnDefaultGestureListener;

/**
 * Created by bb on 2017/5/5.
 */

public class TestOnDefaultGestureListener extends OnDefaultGestureListener {

    public TestOnDefaultGestureListener(IPlayer p, IPlayerControlView c) {
        super(p, c);
    }

    @Override
    protected boolean onTouchDown(MotionEvent e) {
        return super.onTouchDown(e);
    }

    @Override
    protected void onPositionChanged(long startPosition, long finalPosition) {
        super.onPositionChanged(startPosition, finalPosition);
    }

    @Override
    protected boolean onAxisLeftYChanged(MotionEvent e1, MotionEvent e2, float dy) {
        // do yourself
        return true;
    }
}
