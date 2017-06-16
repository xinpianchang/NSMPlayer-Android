package com.vmovier.lib.player.internal;

import android.media.MediaPlayer;

import com.vmovier.lib.utils.PlayerLog;


class InternalAndroidPlayer extends MediaPlayer {
    private static int PLAYER_ID = 0;
    private final int mId;

    InternalAndroidPlayer() {
        super();
        PLAYER_ID ++;
        mId = PLAYER_ID;
        PlayerLog.d("Lifecycle", "InternalAndroidPlayer init , Player Id is " + mId);
    }

    @Override
    protected void finalize() {
        super.finalize();
        PlayerLog.d("Lifecycle", "InternalAndroidPlayer finalize , Player Id is " + mId);
    }
}
