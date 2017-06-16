package com.vmovier.lib.player.internal;

import android.content.Context;
import android.support.annotation.NonNull;

public class InternalPlayerFactory {

    public static IInternalPlayer newInstance(@NonNull Context context, int playerType) {
        IInternalPlayer mediaPlayer;
        switch (playerType) {
            case IInternalPlayer.PLAYERTYPE_ANDROIDMEDIA:
                mediaPlayer = new WrapAndroidPlayer();
                break;
            case IInternalPlayer.PLAYERTYPE_EXO: {
                mediaPlayer = new WrapExoPlayer(context);
                break;
            }
            default:
                mediaPlayer = new WrapExoPlayer(context);
                break;
        }
        return mediaPlayer;
    }
}
