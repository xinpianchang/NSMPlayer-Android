package com.vmovier.lib.player.internal;

import android.content.Context;

import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.LoadControl;
import com.google.android.exoplayer2.RenderersFactory;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.vmovier.lib.utils.PlayerLog;

class InternalExoPlayer extends SimpleExoPlayer {
    private static int PLAYER_ID = 0;
    private final int mId;

    InternalExoPlayer(RenderersFactory renderersFactory, TrackSelector trackSelector, LoadControl loadControl) {
        super(renderersFactory, trackSelector, loadControl);
        PLAYER_ID ++;
        mId = PLAYER_ID;
        PlayerLog.d("Lifecycle", "InternalExoPlayer init , Player Id is " + mId);
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        PlayerLog.d("Lifecycle", "InternalExoPlayer finalize , Player Id is " + mId);
    }

    static InternalExoPlayer newInstance(Context context, TrackSelector trackSelector) {
        return newInstance(new DefaultRenderersFactory(context), trackSelector);
    }

    static InternalExoPlayer newInstance(RenderersFactory renderersFactory, TrackSelector trackSelector) {
        return newInstance(renderersFactory, trackSelector, new DefaultLoadControl());
    }

    static InternalExoPlayer newInstance(RenderersFactory renderersFactory, TrackSelector trackSelector, LoadControl loadControl) {
        return new InternalExoPlayer(renderersFactory, trackSelector, loadControl);
    }
}
