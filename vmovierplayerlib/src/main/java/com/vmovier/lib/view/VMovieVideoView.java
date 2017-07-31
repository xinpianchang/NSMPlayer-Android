package com.vmovier.lib.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.SparseArray;

import com.vmovier.lib.player.IPlayer;
import com.vmovier.lib.player.IPlayerFactory;
import com.vmovier.lib.player.MediaError;
import com.vmovier.lib.player.VideoSize;
import com.vmovier.lib.player.VideoViewDataSource;
import com.vmovier.lib.utils.PlayerLog;
import com.vmovier.player.R;

import java.util.Formatter;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArraySet;

@SuppressWarnings("unused, WeakerAccess")
public class VMovieVideoView extends BasicVideoView {
    private static final String TAG = VMovieVideoView.class.getSimpleName();
    private static int instance = 0;
    private int instanceId;

    // 进入SUSPENDED STATE 之前的状态
    private Bundle mSuspendVideoStateBundle;
    // 进入ERROR STATE 之前的 状态
    private Bundle mErrorVideoStateBundle;
    private static final String SAVE_RENDERTYPE = "save_render_type";
    private static final String SAVE_SCALETYPE = "save_scale_type";
    private static final String SAVE_USECONTROLLER = "save_use_controller";
    private static final String SAVE_CONTROLLER_SHOWTIME ="save_controller_showtime";
    private static final String SAVE_NEEDSHOWPOSTER = "save_need_show_poster";
    private static final String SAVE_POSTER_ANIMATOR_DURATION = "save_poster_animator_duration";


    private static final String SUSPENDED_BUNDLE = "suspended_bundle";
    private static final String ERROR_BUNDLE = "error_bundle";
    private CopyOnWriteArraySet<IVMovieVideoViewListener> mMovieVideoViewListeners = new CopyOnWriteArraySet<>();

    public VMovieVideoView(Context context) {
        super(context);
    }

    public VMovieVideoView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public VMovieVideoView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public VMovieVideoView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    protected void initVideoView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        PlayerLog.d(TAG, "initVideoView");
        instanceId = ++instance;
        PlayerLog.d("Lifecycle", "VMovieVideoView is created, My id is" + instanceId);

        boolean isAutoPlay = false;
        boolean isPreload = false;
        boolean isLoop = false;
        boolean isMuted = false;
        int playerType = IPlayer.PLAYERTYPE_EXO;
        VideoViewDataSource dataSource = null;

        if (attrs != null) {
            TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.VMovieVideoView, 0, 0);
            try {
                // view
                mRenderType = a.getInteger(R.styleable.VMovieVideoView_renderViewType, RENDER_SURFACE_VIEW);
                mScaleType = a.getInteger(R.styleable.VMovieVideoView_scaleType, SCALE_FIT_PARENT);
                mUseController = a.getBoolean(R.styleable.VMovieVideoView_useController, false);
                mNeedShowPosterView = a.getBoolean(R.styleable.VMovieVideoView_needShowPosterView, false);
                mPosterAnimatorDuration = a.getInteger(R.styleable.VMovieVideoView_posterAnimatorDuration, DEFAULT_POSTER_ANIMATOR_DURATION);


                // player
                playerType = a.getInteger(R.styleable.VMovieVideoView_playerType, IPlayer.PLAYERTYPE_EXO);
                isAutoPlay = a.getBoolean(R.styleable.VMovieVideoView_autoPlay, false);
                isPreload = a.getBoolean(R.styleable.VMovieVideoView_preload, false);
                isLoop = a.getBoolean(R.styleable.VMovieVideoView_loop, false);
                isMuted = a.getBoolean(R.styleable.VMovieVideoView_muted, false);
                String data = a.getString(R.styleable.VMovieVideoView_dataSource);
                if (!TextUtils.isEmpty(data)) {
                    dataSource = new VideoViewDataSource(Uri.parse(data));
                }
            } finally {
                a.recycle();
            }
        }

        mVideoListener = new VMovieVideoListener();

        // 初始化渲染View
        setRender(mRenderType);
        // 初始化海报View
        initPosterView(context);
        // 初始化控制View
        initControlView(context, attrs);

        setKeepScreenOn(true);
        setBackground(new ColorDrawable(Color.BLACK));

        mPlayer = IPlayerFactory.newInstance(context);
        mPlayer.setAutoPlay(isAutoPlay);
        mPlayer.setPreload(isPreload);
        mPlayer.setPlayerType(playerType);
        mPlayer.setLoop(isLoop);
        mPlayer.setMuted(isMuted);
        mPlayer.setMediaDataSource(dataSource);

        mOnGenerateGestureDetectorListener = new OnDefaultGenerateGestureDetectorListener();
        super.setPlayer(mPlayer);

        mFormatBuilder = new StringBuilder();
        mFormatter = new Formatter(mFormatBuilder, Locale.getDefault());
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        PlayerLog.d("Lifecycle", "VMovieVideoView is GCed. My id is " + instanceId);
    }

    public void play() {
        mPlayer.play();
    }

    public void pause() {
        mPlayer.pause();
    }

    public void resume() {
        PlayerLog.d(TAG, "resume");
        if (mSuspendVideoStateBundle != null) {
            PlayerLog.d(TAG, "SuspendVideoStateBundle != null ");
            restoreInstanceState(mSuspendVideoStateBundle);
            mSuspendVideoStateBundle = null;
        } else {
            PlayerLog.d(TAG, "resume failed, SuspendVideoStateBundle is null");
        }
    }

    public void suspend() {
        PlayerLog.d(TAG, "suspend");
        if (mSuspendVideoStateBundle == null) {
            PlayerLog.d(TAG, "suspend SuspendVideoStateBundle == null");
            mSuspendVideoStateBundle = saveInstanceState();
        } else {
            PlayerLog.d(TAG, "suspend SuspendVideoStateBundle != null");
        }
        mPlayer.stopPlayback();
    }

    public void retry() {
        PlayerLog.d(TAG, "retry");
        if (mErrorVideoStateBundle != null) {
            PlayerLog.d(TAG, "mErrorVideoStateBundle != null, start restoreErrorBundle");
            mPlayer.restoreState(mErrorVideoStateBundle);
            mErrorVideoStateBundle = null;
        } else {
            PlayerLog.d(TAG, "retry failed, ErrorVideoStateBundle == null");
        }
    }

    public void addVMovieVideoViewListener(@NonNull IVMovieVideoViewListener listener) {
        mMovieVideoViewListeners.add(listener);
    }

    public void removeVMovieVideoViewListener(@NonNull IVMovieVideoViewListener listener) {
        mMovieVideoViewListeners.remove(listener);
    }

    // 彻底销毁播放器 不保存任何状态
    public void stopPlayback() {
        mPlayer.stopPlayback();
    }

    public boolean isPlaying() {
        return mPlayer.isPlaying();
    }

    public void seekTo(long position) {
        mPlayer.seekTo(position);
    }

    public void setMediaDataSource(@Nullable VideoViewDataSource dataSource) {
        if (mPlayer.getMediaDataSource() == dataSource) return;
        PlayerLog.d(TAG, "setDataSource");
        // 更换播放地址的时候 清除之前保留的bundle.
        if (mErrorVideoStateBundle != null && (mPlayer.getAutoPlay() || mPlayer.getPreload())) {
            mErrorVideoStateBundle = null;
            PlayerLog.d(TAG, "setDataSource # 清除ErrorBundle");
        }
        if (mSuspendVideoStateBundle != null && (mPlayer.getAutoPlay() ||  mPlayer.getPreload())) {
            mSuspendVideoStateBundle = null;
            PlayerLog.d(TAG, "setDataSource # 清除SuspendBundle");
        }
        mPlayer.setMediaDataSource(dataSource);
    }

    @Nullable
    public VideoViewDataSource getMediaDataSource() {
        return mPlayer.getMediaDataSource();
    }

    public void setAllowMeteredNetwork(boolean allowMeteredNetwork) {
        PlayerLog.d(TAG, "setAllowMeteredNetwork allowMeteredNetwork is " + allowMeteredNetwork);
        if (allowMeteredNetwork) {
            if (mErrorVideoStateBundle != null && mPlayer.getMediaError() != null && mPlayer.getMediaError().getErrorCode() == MediaError.ERROR_METERED_NETWORK) {
                PlayerLog.d(TAG, "setAllowMeteredNetwork 开始恢复播放器");
                mErrorVideoStateBundle.putBoolean(IPlayer.SAVE_ALLOWMETEREDNETWORK, true);
                mPlayer.restoreState(mErrorVideoStateBundle);
                mErrorVideoStateBundle = null;
            }
        }
        mPlayer.setAllowMeteredNetwork(allowMeteredNetwork);
    }

    public boolean isAllowMeteredNetwork() {
        return mPlayer.isAllowMeteredNetwork();
    }

    public void setPlayerType(int type) {
        mPlayer.setPlayerType(type);
    }

    public int getPlayerType() {
        return mPlayer.getPlayerType();
    }

    public void setPreload(boolean preload) {
        mPlayer.setPreload(preload);
    }

    public boolean getPreload() {
        return mPlayer.getPreload();
    }

    public void setAutoPlay(boolean autoPlay) {
        mPlayer.setAutoPlay(autoPlay);
    }

    public boolean getAutoPlay() {
        return mPlayer.getAutoPlay();
    }

    public void setLoop(boolean loop) {
        mPlayer.setLoop(loop);
    }

    public boolean getLoop() {
        return mPlayer.getLoop();
    }

    public void setVolume(int volume) {
        mPlayer.setVolume(volume);
    }

    public int getVolume() {
        return mPlayer.getVolume();
    }

    public void setMuted(boolean muted) {
        mPlayer.setMuted(muted);
    }

    public boolean getMuted() {
        return mPlayer.getMuted();
    }

    public long getCurrentPosition() {
        return mPlayer.getCurrentPosition();
    }

    public long getDuration() {
        return mPlayer.getDuration();
    }

    public int getBufferPercentage() {
        return mPlayer.getBufferPercentage();
    }

    @NonNull
    public VideoSize getVideoSize() {
        return mVideoSize;
    }

    public boolean isCurrentState(int mask) {
        return mPlayer.isCurrentState(mask);
    }

    public int getCurrentPlayerState() {
        return mPlayer.getCurrentPlayerState();
    }

    @Nullable
    public MediaError getMediaError() {
        return mPlayer.getMediaError();
    }

    @Override
    @Deprecated
    public void setPlayer(IPlayer player) {
//        super.setPlayer(player);
        // 暂时不支持切换
    }

    @Override
    public void setScreenMode(int screenMode) {
        if (mScreenMode == screenMode) return;
        mScreenMode = screenMode;

        if (mControlView != null) {
            mControlView.setScreenMode(screenMode);
        }
    }

    private void restoreInstanceState(Bundle restoreBundle) {
        PlayerLog.d(TAG, "----------------------------   VideoView start restoreInstanceState     ----------------------------");
        if (restoreBundle == null) {
            PlayerLog.d(TAG, "restoreBundle == null");
        } else {
            mNeedShowPosterView = restoreBundle.getBoolean(SAVE_NEEDSHOWPOSTER, mNeedShowPosterView);
            mPosterAnimatorDuration = restoreBundle.getInt(SAVE_POSTER_ANIMATOR_DURATION, mPosterAnimatorDuration);
            mRenderType = restoreBundle.getInt(SAVE_RENDERTYPE, mRenderType);
            mScaleType = restoreBundle.getInt(SAVE_SCALETYPE ,mScaleType);
            mUseController = restoreBundle.getBoolean(SAVE_USECONTROLLER, mUseController);

            Bundle suspendBundle = restoreBundle.getBundle(SUSPENDED_BUNDLE);
            if (suspendBundle != null) {
                mSuspendVideoStateBundle = suspendBundle;
            } else {
                PlayerLog.d(TAG, "restoreState# suspendBundle is null");
            }

            Bundle errorBundle = restoreBundle.getBundle(ERROR_BUNDLE);
            // 这样做是为了防止现场被破坏.
            if (errorBundle != null) {
                mErrorVideoStateBundle = errorBundle;
            } else {
                PlayerLog.d(TAG, "restoreState# errorBundle is null");
            }

            // restore MediaPlayer.
            if (mPlayer != null) {
                mPlayer.restoreState(restoreBundle);
            }
        }
        PlayerLog.d(TAG, "----------------------------   VideoView restoreInstanceState end    ----------------------------");
    }

    private Bundle saveInstanceState() {
        PlayerLog.d(TAG, "-------------   VideoView start saveInstanceState     -------------");
        Bundle bundle;
        // save PlayerState
        if (mPlayer != null) {
            bundle = mPlayer.saveState();
        }  else {
            bundle = new Bundle();
        }

        bundle.putBoolean(SAVE_NEEDSHOWPOSTER, mNeedShowPosterView);
        bundle.putInt(SAVE_POSTER_ANIMATOR_DURATION, mPosterAnimatorDuration);
        bundle.putInt(SAVE_RENDERTYPE, mRenderType);
        bundle.putInt(SAVE_SCALETYPE, mScaleType);
        bundle.putBoolean(SAVE_USECONTROLLER, mUseController);

        if (mSuspendVideoStateBundle != null) {
            bundle.putBundle(SUSPENDED_BUNDLE, mSuspendVideoStateBundle);
        } else {
            PlayerLog.d(TAG, "SuspendVideoStateBundle == null");
        }
        if (mErrorVideoStateBundle != null) {
            bundle.putBundle(ERROR_BUNDLE, mErrorVideoStateBundle);
        } else {
            PlayerLog.d(TAG, "ErrorVideoStateBundle == null");
        }

        PlayerLog.d(TAG, "--------------   VideoView saveInstanceState end  -------------");
        return bundle;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        PlayerLog.d(TAG, "onRestoreInstanceState");
        if (!(state instanceof VideoViewSaveState)) {
            super.onRestoreInstanceState(state);
            return;
        }

        VideoViewSaveState videoViewSaveState = (VideoViewSaveState) state;
        super.onRestoreInstanceState(videoViewSaveState.getSuperState());
        // videoViewSaveState.mVideoViewStateBundle.setClassLoader(getClass().getClassLoader());
        restoreInstanceState(videoViewSaveState.mVideoViewStateBundle);
    }

    // void onRestoreInstanceState (Bundle savedInstanceState)
    // This method is called between onStart() and onPostCreate(Bundle).
    // void onSaveInstanceState (Bundle outState)
    // If called, this method will occur before onStop(). There are no guarantees about whether it will occur before or after onPause().
    @Override
    protected Parcelable onSaveInstanceState() {
        PlayerLog.d(TAG, "onSaveInstanceState start saveState");
        Parcelable parcelable = super.onSaveInstanceState();
        Bundle saveBundle = saveInstanceState();
        VideoViewSaveState ss = new VideoViewSaveState(parcelable);
        ss.mVideoViewStateBundle = saveBundle;
        return ss;
    }

    /**
     * Override to prevent freezing of any views created by the adapter.
     */
    @Override
    protected void dispatchSaveInstanceState(SparseArray<Parcelable> container) {
        dispatchFreezeSelfOnly(container);
    }

    /**
     * Override to prevent thawing of any views created by the adapter.
     */
    @Override
    protected void dispatchRestoreInstanceState(SparseArray<Parcelable> container) {
        dispatchThawSelfOnly(container);
    }

    public static class VideoViewSaveState extends BaseSavedState {
        Bundle mVideoViewStateBundle;

        public VideoViewSaveState(Parcelable parcelable) {
            super(parcelable);
        }

        public VideoViewSaveState(Parcel source) {
            super(source);
            // Android has two different classloaders: the framework classloader (which knows how to load Android classes)
            // and the APK classloader (which knows how to load your code).
            // The APK classloader has the framework classloader set as its parent, meaning it can also load Android classes.
            mVideoViewStateBundle = source.readBundle(getClass().getClassLoader());
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeBundle(mVideoViewStateBundle);
        }

        public static final Creator<VideoViewSaveState> CREATOR = new Creator<VideoViewSaveState>() {
            @Override
            public VideoViewSaveState createFromParcel(Parcel source) {
                return new VideoViewSaveState(source);
            }

            @Override
            public VideoViewSaveState[] newArray(int size) {
                return new VideoViewSaveState[size];
            }
        };
    }

    private class VMovieVideoListener extends BasicVideoListener {
        @Override
        public void onStateChanged(int oldState, int newState) {
            switch (newState) {
                case IPlayer.STATE_ERROR:
                    if (mErrorVideoStateBundle == null) {
                        PlayerLog.d(TAG, "ErrorVideoStateBundle == null 说明是刚发生错误. 需要记录下来");
                        MediaError mediaError = mPlayer.getMediaError();
                        if (mediaError != null) {
                            mErrorVideoStateBundle = mediaError.getRestoreBundle();
                        }
                    } else {
                        PlayerLog.d(TAG, "mErrorVideoStateBundle != null");
                    }
                    break;
                case IPlayer.STATE_PLAYING:
                    // 进入正在播放的状态之后 清掉suspend Bundle. 和 error Bundle.
                    mErrorVideoStateBundle = null;
                    mSuspendVideoStateBundle = null;
                    break;
            }
            super.onStateChanged(oldState, newState);
            for (IVMovieVideoViewListener listener : mMovieVideoViewListeners) {
                listener.onStateChanged(oldState, newState);
            }
        }

        @Override
        public void onVolumeChanged(int startVolume, int finalVolume) {
            PlayerLog.d(TAG, "onVolumeChanged startVolume is " + startVolume + " , finalVolume is " + finalVolume);
            for (IVMovieVideoViewListener listener : mMovieVideoViewListeners) {
                listener.onVolumeChanged(startVolume, finalVolume);
            }
        }
    }

    public interface IVMovieVideoViewListener {
        /**
         * state值
         * @param oldState 老状态
         * @param newState 新状态
         */
        void onStateChanged(int oldState, int newState);

        void onVolumeChanged(int oldVolume, int newVolume);
    }
}
