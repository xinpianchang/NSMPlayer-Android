package com.vmovier.lib.player;

import android.os.Bundle;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.Surface;
import android.view.SurfaceHolder;

import com.vmovier.lib.player.internal.IInternalPlayer;
import com.vmovier.lib.view.IVideoStateListener;
import com.vmovier.lib.view.IVideoSizeListener;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;


/**
 * 外层播放器 行为接口
 */
@SuppressWarnings("unused")
public interface IPlayer {
    // 存储状态 keys
    String SAVE_STATE = "save_state";
    String SAVE_POSITION = "save_position";
    String SAVE_SOURCE = "save_source";
    String SAVE_ERROR   = "save_error";
    String SAVE_PLAYER_TYPE = "save_player_type";
    String SAVE_TARGET_PLAY = "save_targetPlay";
    String SAVE_AUTOPLAY = "save_autoplay";
    String SAVE_PRELOAD = "save_preload";
    String SAVE_LOOP = "save_loop";
    String SAVE_MUTED = "save_muted";
    String SAVE_ALLOWMETEREDNETWORK = "save_allowmeterednetwork";

    // 播放器类型
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({PLAYERTYPE_ANDROIDMEDIA, PLAYERTYPE_EXO})
    @interface PlayerType{}

    int PLAYERTYPE_ANDROIDMEDIA = IInternalPlayer.PLAYERTYPE_ANDROIDMEDIA;
    int PLAYERTYPE_EXO = IInternalPlayer.PLAYERTYPE_EXO;

    /** 平行状态 **/
    int STATE_DEFAULT = 0x01 << 1; // 2
    /** 闲置/未初始化状态 **/
    int STATE_IDLE = 0x01 << 2;    // 4
    /** 错误状态 */
    int STATE_ERROR = 0x01 << 3; // 8
    /** 初始化播放器中 */
    int STATE_PREPARING = 0x01 << 4; // 16
    /** 正在播放 */
    int STATE_PLAYING = 0x01 << 5; // 32
    /** 正在缓冲中 */
    int STATE_BUFFERING = 0x01 << 6; // 64
    /** 正在暂停状态 */
    int STATE_PAUSING = 0x01 << 7; // 128
    /** 播放完成状态 */
    int STATE_COMPLETED = 0x01 << 8; // 256

    /** 播放器正在不能工作的状态下 MASK*/
    int STATE_MASK_UNWORKING = STATE_IDLE | STATE_ERROR;
    /** 播放器正在播放或者马上可以开始播放的状态下 MASK */
    int STATE_MASK_PLAYED = STATE_PLAYING | STATE_BUFFERING;
    /** 播放器正在停止播放状态 MASK */
    int STATE_MASK_PAUSED = STATE_PAUSING | STATE_COMPLETED;
    /** 播放器处于初始化完成的状态下 MASK */
    int STATE_MASK_PREPARED = STATE_MASK_PLAYED | STATE_MASK_PAUSED;

    void play();

    void pause();

    void stopPlayback();

    void seekTo(long position);

    void setMediaDataSource(@Nullable VideoViewDataSource mediaDataSource);

    @Nullable
    VideoViewDataSource getMediaDataSource();

    void setPlayerType(@PlayerType int type);

    int getPlayerType();

    void setPreload(boolean preload);

    boolean getPreload();

    void setAutoPlay(boolean autoPlay);

    boolean getAutoPlay();

    void setLoop(boolean loop);

    boolean getLoop();

    /**
     * 设置播放视频的音量
     * @param volume 范围为0-100
     *               如果低于0 则按静音处理
     *               如果大于100 也按100处理
     */
    void setVolume(int volume);

    /**
     * 获取视频的音量
     * @return volume 范围为0-100
     */
    int getVolume();

    void setMuted(boolean muted);

    boolean getMuted();

    long getDuration();

    long getCurrentPosition();

    int getBufferPercentage();

    @NonNull VideoSize getVideoSize();

    boolean isCurrentState(int mask);

    int getCurrentPlayerState();

    boolean isPlaying();

    @Nullable
    MediaError getMediaError();

    void setAllowMeteredNetwork(boolean allowMeteredNetwork);

    boolean isAllowMeteredNetwork();

    @NonNull Bundle saveState();

    void restoreState(@NonNull Bundle bundle);

    void setDisplay(SurfaceHolder holder);

    void setSurface(Surface surface);

    void addVideoStateListener(@NonNull IVideoStateListener listener);

    void removeVideoStateListener(@NonNull IVideoStateListener listener);

    void addVideoSizeListener(@NonNull IVideoSizeListener listener);

    void removeVideoSizeListener(@NonNull IVideoSizeListener listener);
}
