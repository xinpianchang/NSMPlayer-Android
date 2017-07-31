package com.vmovier.lib.player;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.Surface;
import android.view.SurfaceHolder;

import com.vmovier.lib.Player;
import com.vmovier.lib.player.hsm.State;
import com.vmovier.lib.player.hsm.StateMachine;
import com.vmovier.lib.player.internal.IInternalPlayer;
import com.vmovier.lib.player.internal.InternalPlayerFactory;
import com.vmovier.lib.utils.ConnectionUtils;
import com.vmovier.lib.utils.PlayerLog;
import com.vmovier.lib.view.IVideoListener;

import java.io.IOException;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicReference;


/**
 *                                                         DefaultState
 *                           |                                  |                               |
 *                           |                                  |                               |
 *                     UnWorkingState (Temp name)           Preparing                         Prepared
 *              |                           |                                       |                            |
 *              |                           |                                       |                            |
 *             Idle                       Error                                 Played                         Paused
 *                                                                          |            |                |              |
 *                                                                          |            |                |              |
 *                                                                       Playing      Buffering        Pausing    Completed
 */

@SuppressWarnings("FieldCanBeLocal")
class VMoviePlayer extends StateMachine implements IPlayer {
    public static final String TAG = VMoviePlayer.class.getSimpleName();
    private static final boolean DEBUG = true;
    private IInternalPlayer mInternalMediaPlayer;
    private final Context mAppContext;
    private final CopyOnWriteArraySet<IVideoListener> listeners;
    private final Handler mMainHandler;
    private AudioManager mAudioManager;
    // Max 音量, 不同手机 有可能不同.
    private int mAudioMaxVolume;
    // 当前系统视频音量.
    private int mCurrentVolume;
    // State.
    private PlayerState mDefaultState;
    private PlayerState mUnWorkingState;
    private PlayerState mIdleState;
    private PlayerState mErrorState;
    private PlayerState mPreparingState;
    private PlayerState mPreparedState;
    private PlayerState mPlayedState;
    private PlayerState mPlayingState;
    private PlayerState mBufferingState;
    private PlayerState mPausedState;
    private PlayerState mPausingState;
    private PlayerState mCompletedState;


    private volatile VideoViewDataSource mMediaDataSource;
    private volatile boolean targetPlay = false;
    private volatile int mLastState; // 之前的状态
    private volatile int mState; // 当前平行状态
    private volatile boolean isAutoPlay = false;
    private volatile boolean isPreload = false;
    private volatile boolean isMuted = false;
    private volatile boolean isAllowMeteredNetwork = false;
    private volatile int mPlayerType = PLAYERTYPE_EXO;
    private volatile boolean isLoop = false;
    private volatile VideoSize mVideoSize = new VideoSize();
    private volatile Surface mSurface;

    // 暂存需要restore的 Bundle, 在调用restoreInstanceState 之后 赋值.在成功恢复到相应的状态以后 置空.
    // 在调用saveInstanceState的时候, 如果该值不为空 说明还在恢复状态过程中,那么直接返回该值.
    private final AtomicReference<Bundle> mAtomicRestoreBundle = new AtomicReference<>();
    private volatile MediaError mMediaError = null;
    // 状态机内部产生的临时变量
    private boolean isInternalBuffering = false;

    // 状态机 指令
    // 用户发出的 主动指令
    private static final int CMD_BASE = 100;
    private static final int CMD_PLAY = CMD_BASE + 1; // 开始播放
    private static final int CMD_PAUSE = CMD_BASE + 2; // 暂停播放
    private static final int CMD_SEEK = CMD_BASE + 3; // seek.

    // 状态机以及 VideoView  发出的事件指令
    private static final int EVENT_BASE = 200;
    private static final int EVENT_PLAY = EVENT_BASE + 1;
    private static final int EVENT_PAUSE = EVENT_BASE + 2;
    private static final int EVENT_RESTORE_STATE = EVENT_BASE + 4; // restore state.
    private static final int EVENT_STOP_PLAYBACK = EVENT_BASE + 5; // stop playback.
    private static final int EVENT_TRY_TO_PREPARE = EVENT_BASE + 6; // 尝试进行Prepare.
    private static final int EVENT_UPDATE_DATASOURCE = EVENT_BASE + 7; // 更新播放地址
    private static final int EVENT_UPDATE_SURFACE = EVENT_BASE + 8; // 更新SurfaceView
    private static final int EVENT_PREPARED = EVENT_BASE + 9; // 播放器准备好的信息
    private static final int EVENT_BUFFER_START = EVENT_BASE + 10; // buffer 开始
    private static final int EVENT_BUFFER_END = EVENT_BASE + 11; // buffer 结束
    private static final int EVENT_UPDATE_BUFFERING = EVENT_BASE + 12;
    private static final int EVENT_ERROR = EVENT_BASE + 13; // 出现错误.
    private static final int EVENT_COMPLETION = EVENT_BASE + 14; // 播放完成事件.
    private static final int EVENT_RESTORESTATE_PREPARE = EVENT_BASE + 15; // restore state的时候 发出的prepare命令.
    private static final int EVENT_CHANGE_MUTED = EVENT_BASE + 16; // 改变是否静音
    private static final int EVENT_CHANGE_PLAYERTYPE = EVENT_BASE + 17; //改变播放器类型
    private static final int EVENT_UPDATE_ISLOOP = EVENT_BASE + 18; // 改变Loop类型
    private static final int EVENT_CHECK_ALLOW_METEREDNETWORK = EVENT_BASE + 19; // 检查是否允许在移动网络下播放

    private static int instance = 0;
    private int instanceId;


    public VMoviePlayer(@NonNull Context context) {
        super("StateMachine", Player.getStateMachineLooper());
        instanceId = ++instance;
        PlayerLog.d("Lifecycle", "VMoviePlayer is created, My id is" + instanceId);

        this.mAppContext = context.getApplicationContext();
        this.mMainHandler = new Handler(Looper.getMainLooper());
        this.listeners = new CopyOnWriteArraySet<>();
        initAudioManager();
        startStateMachine();
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        PlayerLog.d("Lifecycle", "VMoviePlayer is GCed. My id is " + instanceId);
    }

    @Override
    public void startStateMachine() {
        mDefaultState = new DefaultState();
        mUnWorkingState = new UnWorkingState();
        mIdleState = new IdleState();
        mErrorState = new ErrorState();
        mPreparingState = new PreparingState();
        mPreparedState = new PreparedState();
        mPlayedState = new PlayedState();
        mPlayingState = new PlayingState();
        mBufferingState = new BufferingState();
        mPausedState = new PausedState();
        mPausingState = new PausingState();
        mCompletedState = new CompletedState();

        addState(mDefaultState);
            addState(mUnWorkingState, mDefaultState);
                addState(mIdleState, mUnWorkingState);
                addState(mErrorState, mUnWorkingState);
            addState(mPreparingState, mDefaultState);
            addState(mPreparedState, mDefaultState);
                addState(mPlayedState, mPreparedState);
                    addState(mPlayingState, mPlayedState);
                    addState(mBufferingState, mPlayedState);
                addState(mPausedState, mPreparedState);
                    addState(mPausingState, mPausedState);
                    addState(mCompletedState, mPausedState);

        setInitialState(mIdleState);
        super.startStateMachine();
    }

    @Override
    public void play() {
        sendMessage(CMD_PLAY);
        targetPlay = true;
        // TODO EXOPlayer 播放的时候 check Surface 是否改变过. 如果改变过需要seek下.
    }

    @Override
    public void pause() {
        sendMessage(CMD_PAUSE);
        targetPlay = false;
    }

    @Override
    public void seekTo(long seekPosition) {
        Message m = Message.obtain();
        m.what = CMD_SEEK;
        m.obj = seekPosition;
        sendMessage(m);
    }

    @Override
    public void stopPlayback() {
        PlayerLog.d(TAG, "stopPlayBack # mAtomicRestoreBundle.set(null)");
        mAtomicRestoreBundle.set(null);
        sendMessage(EVENT_STOP_PLAYBACK);
    }

    @Override
    public void setDisplay(SurfaceHolder sh) {
        pLog("setDisplay");
        if (sh == null) {
            setSurface(null);
        } else {
            setSurface(sh.getSurface());
        }
    }

    @Override
    public void setSurface(Surface surface) {
        if (surface != mSurface) {
            mSurface = surface;
            sendMessage(EVENT_UPDATE_SURFACE);
        } else {
            pLog("setSurface 设置的Surface 和现在的一致.");
        }
    }

    @Override
    public void setMediaDataSource(VideoViewDataSource mediaDataSource) {
        pLog("setMediaDataSource");
        if (mediaDataSource != mMediaDataSource) {
            this.mMediaDataSource = mediaDataSource;
            sendMessage(EVENT_UPDATE_DATASOURCE);
        } else {
            pLog("setMediaDataSource VideoViewDataSource 和现在的一致.");
        }
    }

    @Override
    public @Nullable VideoViewDataSource getMediaDataSource() {
        return mMediaDataSource;
    }

    @Override
    public void setPlayerType(int type) {
        pLog("setPlayerType");
        if (this.mPlayerType != type) {
            this.mPlayerType = type;
            sendMessage(EVENT_CHANGE_PLAYERTYPE);
        } else {
            pLog("setPlayerType playType 和现在的一致.");
        }
    }

    @Override
    public int getPlayerType() {
        return mPlayerType;
    }

    @Override
    public void setAutoPlay(boolean autoPlay) {
        pLog("setAutoPlay autoPlay is " + autoPlay);
        if (autoPlay != isAutoPlay) {
            this.isAutoPlay = autoPlay;
            sendMessage(EVENT_TRY_TO_PREPARE);
        } else {
            pLog("setAutoPlay autoPlay 和现在的一致.");
        }
    }

    @Override
    public boolean getAutoPlay() {
        return isAutoPlay;
    }

    @Override
    public void setPreload(boolean preload) {
        pLog("setPreload preload is " + preload);
        if (preload != isPreload) {
            this.isPreload = preload;
            sendMessage(EVENT_TRY_TO_PREPARE);
        } else {
            pLog("setPreload preload 和现在的一致.");
        }
    }

    @Override
    public boolean getPreload() {
        return isPreload;
    }

    public void setLoop(boolean loop) {
        if (this.isLoop != loop) {
            this.isLoop = loop;
            sendMessage(EVENT_UPDATE_ISLOOP);
        } else {
            pLog("setLoop loop 和现在的一致.");
        }
    }

    @Override
    public boolean getLoop() {
        return isLoop;
    }

    @Override
    public void setMuted(boolean muted) {
        pLog("setMuted muted is " + muted);
        if (isMuted != muted) {
            this.isMuted = muted;
            sendMessage(EVENT_CHANGE_MUTED);
        } else {
            pLog("setPreload preload 和现在的一致.");
        }
    }

    @Override
    public boolean getMuted() {
        return isMuted;
    }

    @Override
    public void setAllowMeteredNetwork(boolean allowMeteredNetwork) {
        pLog("setAllowMeteredNetwork allowMeteredNetwork is " + allowMeteredNetwork);
        if (this.isAllowMeteredNetwork != allowMeteredNetwork) {
            this.isAllowMeteredNetwork = allowMeteredNetwork;
            sendMessage(EVENT_CHECK_ALLOW_METEREDNETWORK);
        } else {
            pLog("setAllowMeteredNetwork allowMeteredNetwork 和现在的一致.");
        }
    }

    @Override
    public boolean isAllowMeteredNetwork() {
        return isAllowMeteredNetwork;
    }

    @Override
    public void setVolume(int volume) {
        if (volume < 0) {
            volume = 0;
        } else if (volume > 100) {
            volume = 100;
        }
        mCurrentVolume = volume;

        float ratio = (float) volume / 100f;
        int audioVolume = (int) (ratio * mAudioMaxVolume);
        // 变更声音
        mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, audioVolume, 0);
    }

    @Override
    public int getVolume() {
        return mCurrentVolume;
    }

    /**
     * 返回状态机当前的状态
     * 有可能返回的状态 1 STATE_IDLE. 2 STATE_ERROR 3 STATE_PLAYING 4 STATE_PAUSING 5 STATE_COMPLETED.
     * @return  Current's State Bundle
     */
    @Override
    public @NonNull Bundle saveState() {
        pLog("\n");
        pLog("--------------   VMoviePlayer start saveInstanceState   ------------");
        Bundle b;
        final Bundle tmpRestoreBundle = mAtomicRestoreBundle.get();
        if (tmpRestoreBundle != null) {
            pLog("mTempRestoreBundle != null, 现在正在恢复状态过程,那么直接返回tempRestoreBundle.");
            b = tmpRestoreBundle;
        } else {
            b = new Bundle();
            if (mState == STATE_PREPARING || mState == STATE_BUFFERING || mState == STATE_MASK_PAUSED ||
                    mState == STATE_MASK_PLAYED || mState == STATE_MASK_PREPARED){
                b.putInt(SAVE_STATE, targetPlay ? STATE_PLAYING : STATE_PAUSING);
                pLog("saveInstanceState 时发现当前状态是过渡态: " + mState + " , 根据targetPlay来决定真正存储的是" +
                        (targetPlay ? "STATE_PLAYING" : "STATE_PAUSING") + "状态");
            } else {
                b.putInt(SAVE_STATE, mState);
            }
            if (mState == STATE_COMPLETED) {
                b.putBoolean(SAVE_TARGET_PLAY, false);
                b.putLong(SAVE_POSITION, 0);
            } else if (mState == STATE_ERROR){
                b.putBoolean(SAVE_TARGET_PLAY, targetPlay);
                b.putLong(SAVE_POSITION, getCurrentPosition());
                b.putParcelable(SAVE_ERROR, mMediaError);
            } else {
                b.putBoolean(SAVE_TARGET_PLAY, targetPlay);
                b.putLong(SAVE_POSITION, getCurrentPosition());
            }

            b.putParcelable(SAVE_SOURCE, mMediaDataSource);
            b.putInt(SAVE_PLAYER_TYPE, mPlayerType);
            b.putBoolean(SAVE_AUTOPLAY, isAutoPlay);
            b.putBoolean(SAVE_PRELOAD, isPreload);
            b.putBoolean(SAVE_LOOP, isLoop);
            b.putBoolean(SAVE_MUTED, isMuted);
            b.putBoolean(SAVE_ALLOWMETEREDNETWORK, isAllowMeteredNetwork);
        }
        pLog("------------  VMoviePlayer saveInstanceState end   ------------");
        pLog("\n");
        return b;
    }

    @Override
    public void restoreState(@NonNull Bundle bundle) {
        pLog("\n");
        pLog("----------------------------   VMoviePlayer start restoreInstanceState   ----------------------------");
        if (mAtomicRestoreBundle.compareAndSet(null, bundle)) {
            pLog("restoreInstanceState start");
            targetPlay = bundle.getBoolean(SAVE_TARGET_PLAY, false);
            mMediaDataSource = bundle.getParcelable(SAVE_SOURCE);
            mPlayerType = bundle.getInt(SAVE_PLAYER_TYPE, PLAYERTYPE_EXO);
            isAutoPlay = bundle.getBoolean(SAVE_AUTOPLAY, isAutoPlay);
            isPreload = bundle.getBoolean(SAVE_PRELOAD, isPreload);
            isLoop = bundle.getBoolean(SAVE_LOOP, isLoop);
            isMuted = bundle.getBoolean(SAVE_MUTED, false);
            isAllowMeteredNetwork = bundle.getBoolean(SAVE_ALLOWMETEREDNETWORK, isAllowMeteredNetwork);

            Message message = Message.obtain();
            message.what = EVENT_RESTORE_STATE;
            message.obj = bundle;
            sendMessage(message);
        } else {
            pLog("restoreInstanceState failed, 因为现在正在restore中 ");
        }
        pLog("----------------------------  VMoviePlayer restoreInstanceState end   ----------------------------");
        pLog("\n");
    }

    // 在Preparing State 下调用源生播放器的getDuration 会报出 Attempt to call getDuration without a valid mediaplayer错误.
    @Override
    public long getDuration() {
        if (isCurrentState(STATE_MASK_PREPARED)) {
            return mInternalMediaPlayer == null ? 0 : mInternalMediaPlayer.getDuration();
        }
        return 0;
    }

    @Override
    public long getCurrentPosition() {
        if (isCurrentState(STATE_MASK_PREPARED) || isCurrentState(STATE_ERROR)) {
            return mInternalMediaPlayer == null ? 0 : mInternalMediaPlayer.getCurrentPosition();
        }
        return 0;
    }

    @Override
    public int getBufferPercentage() {
        if (isCurrentState(STATE_MASK_PREPARED)) {
            return mInternalMediaPlayer == null ? 0 : mInternalMediaPlayer.getBufferedPercentage();
        }
        return 0;
    }

    @Override
    public @NonNull VideoSize getVideoSize() {
        return mVideoSize;
    }

    @Override
    public boolean isCurrentState(int mask) {
        return (mState & mask) > 0;
    }

    @Override
    public int getCurrentPlayerState() {
        return mState;
    }

    @Override
    public boolean isPlaying() {
        return isCurrentState(IPlayer.STATE_MASK_PREPARED) && mInternalMediaPlayer != null && mInternalMediaPlayer.isPlaying();
    }

    @Nullable
    @Override
    public MediaError getMediaError() {
        return mMediaError;
    }

    @Override
    public void addVideoListener(@NonNull IVideoListener listener) {
        if (listeners.size() == 0) {
            startRegister();
        }
        listeners.add(listener);
    }

    @Override
    public void removeVideoListener(@NonNull IVideoListener listener) {
        listeners.remove(listener);
        if (listeners.size() == 0) {
            stopResister();
        }
    }

    private void onPlayerVideoSizeChanged() {
        mMainHandler.post(mNotifyVideoSizeChangeRunnable);
    }

    private void onStateChanged() {
        mMainHandler.post(mStateChangeRunnable);
    }

    private Runnable mNotifyVideoSizeChangeRunnable = new Runnable() {
        @Override
        public void run() {
            PlayerLog.d(TAG, "VideoView 收到播放器进入 PREPARED or VideoSizeChanged 状态的信息 " );
            for (IVideoListener listener : listeners) {
                listener.onVideoSizeChanged(VMoviePlayer.this, mVideoSize);
            }
        }
    };

    private Runnable mStateChangeRunnable = new Runnable() {
        @Override
        public void run() {
            if (mLastState == mState) {
                PlayerLog.d(TAG, "LastState == mState, doNothing");
                return;
            }
            for (IVideoListener listener : listeners) {
                listener.onStateChanged(mLastState, mState);
            }
            mLastState = mState;
        }
    };

    private void startRegister() {
        // 音量物理按键
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.media.VOLUME_CHANGED_ACTION");
        mAppContext.registerReceiver(mVolumeReceiver, filter);

        // 网络状态
        ConnectionUtils.register(mOnConnectionChangeListener);
    }

    private void stopResister() {
        // handler
        mMainHandler.removeCallbacksAndMessages(null);
        // 音量物理按键
        try {
            mAppContext.unregisterReceiver(mVolumeReceiver);
        } catch (IllegalArgumentException e) {
            PlayerLog.d(TAG, "the receiver was already unregistered or was not registered");
        }
        // 网络状态
        ConnectionUtils.unregister(mOnConnectionChangeListener);
    }

    private BroadcastReceiver mVolumeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null) {
                if (intent.getAction().equals("android.media.VOLUME_CHANGED_ACTION")) {
                    int audioCurrentVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                    int oldVolume = mCurrentVolume;
                    if (mAudioMaxVolume != 0) {
                        mCurrentVolume = (int) (((float) audioCurrentVolume / (float) mAudioMaxVolume) * 100);
                    }

                    for (IVideoListener listener : listeners) {
                        listener.onVolumeChanged(oldVolume, mCurrentVolume);
                    }
                }
            }
        }
    };

    private void initAudioManager() {
        mAudioManager = (AudioManager) mAppContext.getSystemService(Context.AUDIO_SERVICE);

        int mode = mAudioManager.getRingerMode();
        if (mode == AudioManager.RINGER_MODE_VIBRATE
                || mode == AudioManager.RINGER_MODE_SILENT) {
            // Normal mode will be audible and may vibrate according to user settings.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                NotificationManager n  = (NotificationManager) mAppContext.getSystemService(Context.NOTIFICATION_SERVICE);
                boolean isPolicyGranted = n.isNotificationPolicyAccessGranted();
                // fixme 7.0手机 如果没有这个权限 直接调用setRingerMode会报出 Not allowed to change Do Not Disturb state的异常.
                if (isPolicyGranted) {
                    mAudioManager.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
                }
            } else {
                mAudioManager.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
            }
        }
        mAudioMaxVolume = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        int audioCurrentVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        if (mAudioMaxVolume != 0) {
            mCurrentVolume = (int) (((float) audioCurrentVolume / (float) mAudioMaxVolume) * 100);
        }
        PlayerLog.d(TAG, "initAudioManager mCurrentVolume is " + mCurrentVolume +
                " , mAudioMaxVolume is " + mAudioMaxVolume +
                " , audioCurrentVolume is " + audioCurrentVolume);
    }


    /** ------------------------ StateMachine State ------------------------*/
    private class DefaultState extends PlayerState {
        @Override
        public void enter() {
            super.enter();
        }

        @Override
        public boolean processMessage(Message msg) {
            switch (msg.what) {
                case EVENT_ERROR:
                    // 在任何状态下 收到错误的信息 都转入 错误状态.
                    transitionTo(mErrorState);
                    return HANDLED;
                case EVENT_STOP_PLAYBACK:
                    transitionTo(mIdleState);
                    release();
                    return HANDLED;
                case EVENT_RESTORE_STATE:
                    // 只有在UnWorkingState下才接受该命令
                    mAtomicRestoreBundle.set(null);
                    return HANDLED;
            }
            return NOT_HANDLED;
        }
    }

    private class UnWorkingState extends PlayerState {
        @Override
        public boolean processMessage(Message msg) {
            switch (msg.what) {
                case EVENT_RESTORE_STATE: // 只有在UNWorkingState下 才处理restore的命令
                    pLog("start restoreState");
                    Bundle restoreBundle = (Bundle) msg.obj;
                    if (restoreBundle == null) {
                        pLog("restoreBundle == null.");
                        return NOT_HANDLED;
                    }

                    final MediaError mediaError = restoreBundle.getParcelable(SAVE_ERROR);
                    final int restoreState = restoreBundle.getInt(SAVE_STATE);
                    switch (restoreState) {
                        case STATE_IDLE:
                            pLog("希望恢复的State 为IDLE State.");
                            if (mState != STATE_IDLE) {
                                transitionTo(mIdleState);
                            }
                            // 恢复成功
                            mAtomicRestoreBundle.set(null);
                            break;
                        case STATE_ERROR:
                            sendMessage(EVENT_ERROR, mediaError);
                            // 恢复成功
                            mAtomicRestoreBundle.set(null);
                            break;
                        case STATE_PLAYING:
                        case STATE_PAUSING:
                        case STATE_COMPLETED:
                            pLog("希望恢复的State 为Working State");
                            if (mMediaDataSource != null) {
                                transitionTo(mPreparingState);
                                sendMessage(EVENT_RESTORESTATE_PREPARE, restoreBundle);
                            }
                            break;
                    }
                    return HANDLED;
                case EVENT_CHANGE_MUTED: //在不能工作状态下的 忽略改变音量的操作
                case CMD_PLAY:
                case CMD_PAUSE:
                case CMD_SEEK:
                case EVENT_CHANGE_PLAYERTYPE: // UnWorkingState 下 收到改变播放器状态的命令什么都不做
                case EVENT_UPDATE_SURFACE:    // UnWorkingState下 无论如何更新Surface 我们都不关心,仅做记录.
                case EVENT_CHECK_ALLOW_METEREDNETWORK:
                    return HANDLED;
            }
            return NOT_HANDLED;
        }
    }


    private class IdleState extends PlayerState {
        @Override
        public void enter() {
            super.enter();
            mState = STATE_IDLE;
            onStateChanged();
        }

        @Override
        public boolean processMessage(Message msg) {
            switch (msg.what) {
                case CMD_PLAY:
                    // IDLE状态下 收到Play的 指令 检查是否已经有地址了
                    if (mMediaDataSource != null) {
                        // 如果有 那么就开始加载播放器, 并且在播放完毕以后开始播放
                        targetPlay = true;
                        transitionTo(mPreparingState);
                        sendMessage(EVENT_TRY_TO_PREPARE);
                        mAtomicRestoreBundle.set(null);
                    } else {
                        // 是否进入错误状态呢.
                    }
                    return HANDLED;
                case EVENT_TRY_TO_PREPARE:
                case EVENT_UPDATE_DATASOURCE:
                    if (mMediaDataSource == null) {
                        return HANDLED;
                    }
                    if (isAutoPlay) {
                        targetPlay = true;
                        transitionTo(mPreparingState);
                        sendMessage(EVENT_TRY_TO_PREPARE);
                    } else if (isPreload) {
                        transitionTo(mPreparingState);
                        sendMessage(EVENT_TRY_TO_PREPARE);
                    } else {
                    }
                    return HANDLED;
            }
            return NOT_HANDLED;
        }
    }

    private class ErrorState extends PlayerState {
        @Override
        public void enter() {
            super.enter();
            Message msg = getCurrentMessage();
            if (msg != null) {
                mMediaError = (MediaError) msg.obj;
                if (mMediaError.getRestoreBundle() == null) {
                    mMediaError.setRestoreBundle(saveState());
                }
                // 清掉需要恢复的bundle 如果有.
                mAtomicRestoreBundle.set(null);
            }
            mState = STATE_ERROR;
            onStateChanged();
        }

        @Override
        public void exit() {
            super.exit();
            // 清空 MediaError.
            mMediaError = null;
        }

        @Override
        public boolean processMessage(Message msg) {
            switch (msg.what) {
                case EVENT_UPDATE_DATASOURCE:
                    if (mMediaDataSource == null) {
                        return HANDLED;
                    }
                    if (isAutoPlay) {
                        targetPlay = true;
                        transitionTo(mPreparingState);
                        sendMessage(EVENT_TRY_TO_PREPARE);
                    } else if (isPreload) {
                        transitionTo(mPreparingState);
                        sendMessage(EVENT_TRY_TO_PREPARE);
                    } else {
                    }
                    return HANDLED;
                case EVENT_ERROR:
                    pLog("Error 状态对EVENT_ERROR 不做处理");
                    return HANDLED;
            }
            return super.processMessage(msg);
        }
    }

    private class PreparingState extends PlayerState {
        @Override
        public void enter() {
            super.enter();
            mState = STATE_PREPARING;
            onStateChanged();
        }

        @Override
        public boolean processMessage(Message msg) {
            switch (msg.what) {
                case EVENT_UPDATE_DATASOURCE:
                case EVENT_TRY_TO_PREPARE:
                    if (mMediaDataSource == null) {
                        transitionTo(mIdleState);
                    } else {
                        if (needCareMeteredNetwork()) {
                            pLog("现在是移动网络并且用户不同意在这种情况下播放");
                            MediaError error = new MediaError(MediaError.ERROR_METERED_NETWORK);
                            error.setRestoreBundle(saveState());
                            sendMessage(EVENT_ERROR, error);
                        } else {
                            prepareIfSourceReady();
                        }
                    }
                    return HANDLED;
                case EVENT_UPDATE_SURFACE:
                    if (mInternalMediaPlayer != null) {
                        mInternalMediaPlayer.setSurface(mSurface);
                    }
                    return HANDLED;
                case EVENT_PREPARED: // 收到Prepared的之后 判断应该去什么状态。
                    // 去一个根节点的时候 转化Event信息.
                    transitionTo(mPreparedState);
                    final Bundle tmpRestoreBundle = mAtomicRestoreBundle.getAndSet(null);
                    if (tmpRestoreBundle != null) {
                        int restoreState = tmpRestoreBundle.getInt(SAVE_STATE);
                        // 如果是恢复出来的状态,检查下是否需要恢复到播放完成状态. 如果不是则 根据targetPlay恢复到最新的状态
                        // 注意: 这里 targetPlay 有可能与Bundle中 存储的不一样, 因为在恢复过程中 有可能外部发了play 或者pause的指令/
                        switch (restoreState) {
                            case STATE_COMPLETED:
                                sendMessage(EVENT_COMPLETION);
                                return HANDLED;
                        }
                    }
                    if (targetPlay) {
                        sendMessage(EVENT_PLAY);
                    } else {
                        sendMessage(EVENT_PAUSE);
                    }
                    return HANDLED;
                case EVENT_RESTORESTATE_PREPARE:
                    if (mMediaDataSource == null) {
                        transitionTo(mIdleState);
                    } else {
                        if (needCareMeteredNetwork()) {
                            pLog("现在是移动网络并且用户不同意在这种情况下播放");
                            MediaError error = new MediaError(MediaError.ERROR_METERED_NETWORK);
                            error.setRestoreBundle(saveState());
                            mLastState = STATE_PREPARING;
                            // 这样做是因为线程问题,EventBus还没有把PLAYEREVENT_PREPARING的信息送到,mLastState还是上次的Error状态. 但是其实已经应该是Preparing状态了
                            sendMessage(EVENT_ERROR, error);
                            pLog("mLastState is " + mLastState + " , State is " + mState);
                        } else {
                            prepareIfSourceReady();
                            if (msg.obj != null) {
                                // 取出恢复bundle 看是否需要seek 以及静音
                                Bundle restoreBundle = (Bundle) msg.obj;
                                final long seekPosition = restoreBundle.getLong(SAVE_POSITION, 0);
                                if (seekPosition != 0) {
                                    sendMessage(CMD_SEEK, seekPosition);
                                }
                            }
                        }
                    }
                    return HANDLED;
                case EVENT_CHECK_ALLOW_METEREDNETWORK:
                    if (needCareMeteredNetwork()) {
                        pLog("现在是在播放器加载状态中, 当前是移动网络并且用户不同意在这种情况下播放");
                        MediaError error = new MediaError(MediaError.ERROR_METERED_NETWORK);
                        error.setRestoreBundle(saveState());
                        release();

                        mLastState = STATE_PREPARING;
                        // 这样做是因为线程问题,EventBus还没有把PLAYEREVENT_PREPARING的信息送到,mLastState还是上次的Error状态. 但是其实已经应该是Preparing状态了
                        sendMessage(EVENT_ERROR, error);
                        pLog("mLastState is " + mLastState + " , State is " + mState);
                    } else {
                        pLog("现在是在播放器加载状态中, 发生了需要检查移动网络播放的情况, 检查后发现并不影响,继续加载播放器 ");
                    }
                    return HANDLED;
                case CMD_PLAY:
                case CMD_PAUSE: // 在PreparingState下 接受到暂停和播放的信息 什么都不做.
                    return HANDLED;
                case CMD_SEEK: //"PreparingState 下 无法处理Seek  但是不希望丢失掉该信息, defer it!"
                    removeDeferredMessage(msg.what);
                    deferMessage(msg);
                case EVENT_CHANGE_MUTED:
                case EVENT_CHANGE_PLAYERTYPE: // PreparingState 下 收到改变播放器状态的命令什么都不做"
                    return HANDLED;
            }
            return NOT_HANDLED;
        }
    }

    private class PreparedState extends PlayerState {
        @Override
        public void enter() {
            super.enter();
            onStateChanged();
            if (mInternalMediaPlayer != null) {
                mInternalMediaPlayer.setVolume(isMuted ? 0 : 1);
            }
        }

        @Override
        public void exit() {
            super.exit();
        }

        @Override
        public boolean processMessage(Message msg) {
            switch (msg.what) {
                case EVENT_PLAY:
                    transitionTo(mPlayingState);
                    if (mInternalMediaPlayer != null) {
                        mInternalMediaPlayer.start();
                    }
                    return HANDLED;
                case EVENT_PAUSE:
                    transitionTo(mPausingState);
                    if (mInternalMediaPlayer != null && mInternalMediaPlayer.isPlaying()) {
                        // 如果不check下是否已经播放了,直接调用暂停会报出 MediaPlayer error: pause called in state 64.
                        mInternalMediaPlayer.pause();
                    }
                    return HANDLED;
                case CMD_SEEK:
                    if (mInternalMediaPlayer != null) {
                        long seekPosition = (long) msg.obj;
                        mInternalMediaPlayer.seekTo(seekPosition);
                    }
                    return HANDLED;
                case EVENT_COMPLETION:
                    if (isLoop) {
                        transitionTo(mPlayingState);
                        if (mInternalMediaPlayer != null) {
                            mInternalMediaPlayer.start();
                        }
                    } else {
                        transitionTo(mCompletedState);
                    }
                    return HANDLED;
                case EVENT_UPDATE_SURFACE:
                    if (mInternalMediaPlayer != null) {
                        mInternalMediaPlayer.setSurface(mSurface);
//                        if (mPlayerType == PLAYERTYPE_EXO && mSurface != null) {
//                            //FIXME WorkAround, 解决ExoPlayer change RenderView的时候 有可能出现的黑屏现象.
//                            long duration = mInternalMediaPlayer.getDuration();
//                            long position = mInternalMediaPlayer.getCurrentPosition();
//                            if (duration > 0) {
//                                if (position - 20 >= 0) {
//                                    mInternalMediaPlayer.seekTo(position - 20);
//                                } else if (position + 20 < duration) {
//                                    mInternalMediaPlayer.seekTo(duration + 20);
//                                }
//                            }
//                        }
                    }
                    return HANDLED;
                case EVENT_BUFFER_START:
                    isInternalBuffering = true;
                    sendMessage(EVENT_UPDATE_BUFFERING);
                    return HANDLED;
                case EVENT_BUFFER_END:
                    isInternalBuffering = false;
                    sendMessage(EVENT_UPDATE_BUFFERING);
                    return HANDLED;
                case EVENT_CHANGE_MUTED: // 修改播放器音量
                    if (mInternalMediaPlayer != null) {
                        mInternalMediaPlayer.setVolume(isMuted ? 0 : 1);
                    }
                    return HANDLED;
                case EVENT_CHANGE_PLAYERTYPE: // 修改播放器类型
                case EVENT_UPDATE_DATASOURCE: // 修改播放器地址
                    transitionTo(mPreparingState);
                    sendMessage(EVENT_TRY_TO_PREPARE);
                    return HANDLED;
                case EVENT_CHECK_ALLOW_METEREDNETWORK: // 网络发生变化
                    // FIXME. 如果是播放完成状态 收到变化 也发生响应 是否不合理?
                    if (needCareMeteredNetwork()) {
                        MediaError error = new MediaError(MediaError.ERROR_METERED_NETWORK);
                        error.setRestoreBundle(saveState());
                        release();
                        sendMessage(EVENT_ERROR, error);
                    } else {
                        pLog("播放器运转过程中发生网络变化, 但不是转变到移动网络 或者 用户允许在移动网络下播放");
                    }
                    return HANDLED;
            }
            return NOT_HANDLED;
        }
    }


    private class PlayedState extends PlayerState {
        @Override
        public void enter() {
            // 重定向.
            if (isInternalBuffering) {
                transitionTo(mBufferingState);
            } else {
                transitionTo(mPlayingState);
            }
        }

        @Override
        public boolean processMessage(Message msg) {
            switch (msg.what) {
                case EVENT_UPDATE_BUFFERING:
                    if (isInternalBuffering) {
                        transitionTo(mBufferingState);
                    } else {
                        transitionTo(mPlayingState);
                    }
                    return HANDLED;
                case CMD_PAUSE:
                    transitionTo(mPausingState);
                    if (mInternalMediaPlayer != null) {
                        mInternalMediaPlayer.pause();
                    }
                    return HANDLED;
                case CMD_PLAY:
                    return HANDLED;
            }
            return NOT_HANDLED;
        }
    }

    private class PlayingState extends PlayerState {
        @Override
        public void enter() {
            super.enter();
            mState = STATE_PLAYING;
            onStateChanged();
        }
    }

    private class BufferingState extends PlayerState {
        @Override
        public void enter() {
            super.enter();
            mState = STATE_BUFFERING;
            onStateChanged();
        }
    }

    private class PausedState extends PlayerState {
        @Override
        public boolean processMessage(Message msg) {
            switch (msg.what) {
                case CMD_PLAY:
                    transitionTo(mPlayingState);
                    if (mInternalMediaPlayer != null) {
                        mInternalMediaPlayer.start();
                    }
                    return HANDLED;
                case CMD_SEEK:
                    transitionTo(mPlayingState);
                    targetPlay = true;
                    if (mInternalMediaPlayer != null) {
                        long seekPosition = (long) msg.obj;
                        mInternalMediaPlayer.seekTo(seekPosition);
                        mInternalMediaPlayer.start();
                    }
                case CMD_PAUSE:
                case EVENT_UPDATE_BUFFERING:
                    return HANDLED;
            }
            return NOT_HANDLED;
        }
    }

    private class PausingState extends PlayerState {
        @Override
        public void enter() {
            super.enter();
            mState = STATE_PAUSING;
            onStateChanged();
        }
    }

    private class CompletedState extends PlayerState {
        @Override
        public void enter() {
            super.enter();
            mState = STATE_COMPLETED;
            onStateChanged();
        }

        @Override
        public boolean processMessage(Message msg) {
            switch (msg.what) {
                case CMD_PLAY:
                    transitionTo(mPlayingState);
                    if (mInternalMediaPlayer != null) {
                        mInternalMediaPlayer.start();
                    }
                    return HANDLED;
                case EVENT_UPDATE_ISLOOP:
                    if (isLoop) {
                        transitionTo(mPlayingState);
                        if (mInternalMediaPlayer != null) {
                            mInternalMediaPlayer.start();
                        }
                    }
                    return HANDLED;
                case EVENT_UPDATE_BUFFERING:
                    return HANDLED;
            }
            return NOT_HANDLED;
        }
    }

    /**
     * 如果播放地址和SurfaceHolder 都准备还了 就开始prepare 否则就什么都不做.
     */
    private void prepareIfSourceReady() {
        pLog("prepareIfSourceReady");

        release();

        AudioManager am = (AudioManager) mAppContext.getSystemService(Context.AUDIO_SERVICE);
        am.requestAudioFocus(null, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);

        try {
            pLog("createPlayer type is " + mPlayerType);
            mInternalMediaPlayer = InternalPlayerFactory.newInstance(mAppContext, mPlayerType);
            mInternalMediaPlayer.setOnPreparedListener(mPreparedListener);
            mInternalMediaPlayer.setOnVideoSizeChangedListener(mSizeChangedListener);
            mInternalMediaPlayer.setOnCompletionListener(mCompletionListener);
            mInternalMediaPlayer.setOnErrorListener(mErrorListener);
            mInternalMediaPlayer.setOnInfoListener(mInfoListener);
            mInternalMediaPlayer.setOnSeekCompleteListener(mSeekCompleteListener);

            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
                mInternalMediaPlayer.setDataSource(mAppContext, mMediaDataSource.uri, mMediaDataSource.headers);
            } else {
                mInternalMediaPlayer.setDataSource(mMediaDataSource.uri.toString());
            }
            mInternalMediaPlayer.setSurface(mSurface); // maybe Null.
            mInternalMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mInternalMediaPlayer.prepareAsync();
        } catch (IOException ex) {
            pLog("Unable to open content: " + mMediaDataSource.uri);
            MediaError e = new MediaError(MediaError.ERROR_PREPARE);
            sendMessage(EVENT_ERROR, e);
            return;
        } catch (IllegalArgumentException ex) {
            pLog("Unable to open content: " + mMediaDataSource.uri);
            MediaError e = new MediaError(MediaError.ERROR_PREPARE);
            sendMessage(EVENT_ERROR, e);
            return;
        }
    }

    /**
     * release the media player in any state
     */
    private void release() {
        pLog("release");
        if (mInternalMediaPlayer != null) {
            isInternalBuffering = false;
            mInternalMediaPlayer.release();
            mInternalMediaPlayer.setOnPreparedListener(null);
            mInternalMediaPlayer.setOnVideoSizeChangedListener(null);
            mInternalMediaPlayer.setOnCompletionListener(null);
            mInternalMediaPlayer.setOnErrorListener(null);
            mInternalMediaPlayer.setOnInfoListener(null);
            mInternalMediaPlayer.setOnSeekCompleteListener(null);
            mInternalMediaPlayer = null;
            AudioManager am = (AudioManager) mAppContext.getSystemService(Context.AUDIO_SERVICE);
            am.abandonAudioFocus(null);
        }
    }

    /**
     * 检查是否需要提醒当前是在移动网络播放
     */
    private boolean needCareMeteredNetwork() {
        return ConnectionUtils.isMobileConnected() && !isAllowMeteredNetwork;
    }

    private IInternalPlayer.OnPreparedListener mPreparedListener = new IInternalPlayer.OnPreparedListener() {
        @Override
        public void onPrepared(IInternalPlayer mp) {
            sendMessage(EVENT_PREPARED);
        }
    };

    private IInternalPlayer.OnCompletionListener mCompletionListener = new IInternalPlayer.OnCompletionListener() {
        public void onCompletion(IInternalPlayer mp) {
            pLog("onCompletion");
            sendMessage(EVENT_COMPLETION);
        }
    };

    private IInternalPlayer.OnVideoSizeChangedListener mSizeChangedListener = new IInternalPlayer.OnVideoSizeChangedListener() {
        public void onVideoSizeChanged(IInternalPlayer mp, int width, int height, int sarNum, int sarDen) {
            pLog("onVideoSizeChanged");
            mVideoSize.videoHeight = height;
            mVideoSize.videoWidth = width;
            mVideoSize.videoSarDen = sarDen;
            mVideoSize.videoSarNum = sarNum;
            onPlayerVideoSizeChanged();
        }
    };

    private IInternalPlayer.OnErrorListener mErrorListener = new IInternalPlayer.OnErrorListener() {
        @Override
        public boolean onError(IInternalPlayer mp, MediaError error) {
            sendMessage(EVENT_ERROR, error);
            return true;
        }
    };

    private IInternalPlayer.OnInfoListener mInfoListener = new IInternalPlayer.OnInfoListener() {
        @Override
        public boolean onInfo(IInternalPlayer mp, int what, int extra) {
            switch (what) {
                case IInternalPlayer.MEDIA_INFO_VIDEO_TRACK_LAGGING:
                    pLog("onInfo# MEDIA_INFO_VIDEO_TRACK_LAGGING:");
                    break;
                case IInternalPlayer.MEDIA_INFO_VIDEO_RENDERING_START:
                    pLog("onInfo# MEDIA_INFO_VIDEO_RENDERING_START:");
                    break;
                //缓冲视频开始.
                case IInternalPlayer.MEDIA_INFO_BUFFERING_START:
                    pLog("onInfo# MEDIA_INFO_BUFFERING_START:");
                    sendMessage(EVENT_BUFFER_START);
                    break;
                //缓冲视频结束.
                case IInternalPlayer.MEDIA_INFO_BUFFERING_END:
                    pLog("onInfo# MEDIA_INFO_BUFFERING_END:");
                    sendMessage(EVENT_BUFFER_END);
                    break;
                case IInternalPlayer.MEDIA_INFO_NETWORK_BANDWIDTH:
                    pLog("onInfo# MEDIA_INFO_NETWORK_BANDWIDTH: " + extra);
                    break;
                case IInternalPlayer.MEDIA_INFO_BAD_INTERLEAVING:
                    pLog("onInfo# MEDIA_INFO_BAD_INTERLEAVING:");
                    break;
                case IInternalPlayer.MEDIA_INFO_NOT_SEEKABLE:
                    pLog("onInfo# MEDIA_INFO_NOT_SEEKABLE:");
                    break;
                case IInternalPlayer.MEDIA_INFO_METADATA_UPDATE:
                    pLog("onInfo# MEDIA_INFO_METADATA_UPDATE:");
                    break;
                case IInternalPlayer.MEDIA_INFO_UNSUPPORTED_SUBTITLE:
                    pLog("onInfo# MEDIA_INFO_UNSUPPORTED_SUBTITLE:");
                    break;
                case IInternalPlayer.MEDIA_INFO_SUBTITLE_TIMED_OUT:
                    pLog("onInfo# MEDIA_INFO_SUBTITLE_TIMED_OUT:");
                    break;
                case IInternalPlayer.MEDIA_INFO_VIDEO_ROTATION_CHANGED:
                    pLog("onInfo# MEDIA_INFO_VIDEO_ROTATION_CHANGED: " + extra);
                    break;
                case IInternalPlayer.MEDIA_INFO_AUDIO_RENDERING_START:
                    pLog("onInfo# MEDIA_INFO_AUDIO_RENDERING_START:");
                    break;
            }
            return true;
        }
    };

    private IInternalPlayer.OnSeekCompleteListener mSeekCompleteListener = new IInternalPlayer.OnSeekCompleteListener() {
        @Override
        public void onSeekComplete(IInternalPlayer mp) {
            pLog("onSeekComplete");
        }
    };


    private ConnectionUtils.OnConnectionChangeListener mOnConnectionChangeListener = new ConnectionUtils.OnConnectionChangeListener() {
        @Override
        public void onConnectionChange(Intent connectivityIntent) {
            sendMessage(EVENT_CHECK_ALLOW_METEREDNETWORK);
        }
    };

    private void pLog(String content) {
        if (DEBUG) {
            PlayerLog.d(TAG, content);
        }
    }

    private class PlayerState extends State {
    }
}
