package com.vmovier.lib.view;

import android.content.Context;
import android.support.annotation.Nullable;
import com.vmovier.lib.player.IPlayer;

public interface IPlayerControlView {
    void show();
    void hide();
    void hideAfterTimeout();
    void setScreenMode(int screenMode);
    void setPlayer(IPlayer player);
    @Nullable IPlayer getPlayer();
    void setAnimateProvider(PlayerVisibilityUtils.VisibilityAnimateProvider provider);
    void setTopAnimateProvider(PlayerVisibilityUtils.VisibilityAnimateProvider topProvider);
    void setBottomAnimateProvider(PlayerVisibilityUtils.VisibilityAnimateProvider bottomProvider);
    boolean isVisible();
    void setOnVisibilityListener(OnControlViewVisibilityListener listener);
    int getVideoViewHeight();
    int getVideoViewWidth();
    Context getContext();
}
