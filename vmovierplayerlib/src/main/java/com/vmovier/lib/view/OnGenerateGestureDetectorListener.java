package com.vmovier.lib.view;

import android.content.Context;
import android.view.GestureDetector;

import com.vmovier.lib.player.IPlayer;


public interface OnGenerateGestureDetectorListener {
    GestureDetector generateGestureDetector(Context context, IPlayer player, IPlayerControlView controlView);
}
