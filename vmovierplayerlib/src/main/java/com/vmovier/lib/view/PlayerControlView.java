package com.vmovier.lib.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.annotation.NonNull;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.vmovier.lib.player.IPlayer;
import com.vmovier.player.R;

import java.util.Formatter;
import java.util.Locale;


public class PlayerControlView extends FrameLayout implements IPlayerControlView {
    // 显示时长
    public static final int DEFAULT_SHOW_TIMEOUT_MS = 2000;
    // 更新Progress的频率
    private static final int DEFAULT_UPDATE_PROGRESS_MS = 1000;
    // Progressbar的 max
    private static final int PROGRESS_BAR_MAX = 1000;

    private int mCurrentViewMode = PLAYERSCREENMODE_PORTRAIT_INSET;

    // 竖屏小屏模式
    public static final int PLAYERSCREENMODE_PORTRAIT_INSET = BasicVideoView.PLAYERSCREENMODE_PORTRAIT_INSET;
    // 竖屏全屏模式
    public static final int PLAYERSCREENMODE_PORTRAIT_FULLSCREEN = BasicVideoView.PLAYERSCREENMODE_PORTRAIT_FULLSCREEN;
    // 横屏全屏模式
    public static final int PLAYERSCREENMODE_LANDSCAPE_FULLSCREEN = BasicVideoView.PLAYERSCREENMODE_LANDSCAPE_FULLSCREEN;

    private ImageView mPlayView, mPauseView;
    private TextView mPositionView, mDurationView;
    private SeekBar mProgressBar;
    private View mMaskLayout, mTopLayout, mBottomLayout, mCenterLayout, mLockLayout;
    private View mLockView, mUnLockView;

    private int mShowTimeoutMs;
    private IPlayer mPlayer;
    private boolean isAttachedToWindow;
    private final ComponentListener componentListener;
    private final StringBuilder mFormatBuilder;
    private final Formatter mFormatter;
    private boolean dragging;
    /** 横屏控制view */
    private final View mLandscapeView;
    /** 竖屏 Inset 控制view */
    private final View mPortraitInsetView;
    /** 竖屏全屏view */
    private final View mPortraitFullScreenView;

    private int mControlViewWidth;
    private int mControlViewHeight;

    private PlayerVisibilityUtils.VisibilityAnimateProvider mTopProvider;
    private PlayerVisibilityUtils.VisibilityAnimateProvider mBottomProvider;
    private OnControlViewListener mOnControlViewListener;

    private boolean mIsLocking = false;

    private final Runnable updateProgressAction = new Runnable() {
        @Override
        public void run() {
            updateProgress();
        }
    };

    private final Runnable hideAction = new Runnable() {
        @Override
        public void run() {
            hide();
        }
    };

    public PlayerControlView(Context context) {
        this(context, null);
    }

    public PlayerControlView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PlayerControlView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        int defaultControlId = R.layout.vmovie_player_control_view;
        int portraitInsetLayoutId = defaultControlId;
        int portraitFullScreenLayoutId = defaultControlId;
        int landScapeLayoutId = defaultControlId;

        mShowTimeoutMs = DEFAULT_SHOW_TIMEOUT_MS;
        if (attrs != null) {
            TypedArray a = context.getTheme().obtainStyledAttributes(attrs,
                    R.styleable.PlayerControlView, 0, 0);
            try {
                defaultControlId = a.getResourceId(R.styleable.PlayerControlView_defaultControllerLayoutId, defaultControlId);

                mShowTimeoutMs = a.getInt(R.styleable.PlayerControlView_controllerShowTimeoutMs, mShowTimeoutMs);
                portraitInsetLayoutId = a.getResourceId(R.styleable.PlayerControlView_portraitInsetViewControllerLayoutId, defaultControlId);
                portraitFullScreenLayoutId = a.getResourceId(R.styleable.PlayerControlView_portraitFullScreenViewControllerLayoutId, defaultControlId);
                landScapeLayoutId = a.getResourceId(R.styleable.PlayerControlView_landscapeViewControllerLayoutId, defaultControlId);
                mCurrentViewMode = a.getInt(R.styleable.PlayerControlView_defaultControlViewMode, mCurrentViewMode);
            } finally {
                a.recycle();
            }
        }

        mLandscapeView = View.inflate(context, landScapeLayoutId, null);
        mPortraitInsetView = View.inflate(context, portraitInsetLayoutId, null);
        mPortraitFullScreenView = View.inflate(context, portraitFullScreenLayoutId, null);

        setDescendantFocusability(FOCUS_AFTER_DESCENDANTS);

        mFormatBuilder = new StringBuilder();
        mFormatter = new Formatter(mFormatBuilder, Locale.getDefault());
        componentListener = new ComponentListener();

        PlayerVisibilityUtils.setVisibilityAnimateProvider(this, sMaskProvider);
        mTopProvider = sTopProvider;
        mBottomProvider = sBottomProvider;

        initControllerViewByOrientation(true);
        setVisibility(VISIBLE);
    }

    private void initControllerViewByOrientation() {
        initControllerViewByOrientation(false);
    }

    private View mControlView;

    private void initControllerViewByOrientation(boolean firstInit) {
        switch (mCurrentViewMode) {
            case PLAYERSCREENMODE_PORTRAIT_INSET:
                mControlView = mPortraitInsetView;
                break;
            case PLAYERSCREENMODE_PORTRAIT_FULLSCREEN:
                mControlView = mPortraitFullScreenView;
                break;
            case PLAYERSCREENMODE_LANDSCAPE_FULLSCREEN:
                mControlView = mLandscapeView;
                break;
            default:
                mControlView = mPortraitInsetView;
                break;
        }
        // 移除全部View
        removeAllViews();
        // 加上controlView
        addView(mControlView);

        computeControlViewSize();

        mMaskLayout = mControlView.findViewById(R.id.player_control_mask_layout);
        if (mMaskLayout != null) {
            PlayerVisibilityUtils.setVisibilityAnimateProvider(mMaskLayout, sMaskProvider);
            if (firstInit) {
                PlayerVisibilityUtils.setTargetVisibility(mMaskLayout, GONE, 0);
            } else {
                PlayerVisibilityUtils.setTargetVisibility(mMaskLayout, VISIBLE);
            }
        }

        mTopLayout = mControlView.findViewById(R.id.player_control_top_layout);
        if (mTopLayout != null) {
            PlayerVisibilityUtils.setVisibilityAnimateProvider(mTopLayout, mTopProvider);
            if (firstInit) {
                PlayerVisibilityUtils.setTargetVisibility(mTopLayout, GONE, 0);
            } else {
                PlayerVisibilityUtils.setTargetVisibility(mTopLayout, VISIBLE);
            }
        }

        mBottomLayout = mControlView.findViewById(R.id.player_control_bottom_layout);
        if (mBottomLayout != null) {
            PlayerVisibilityUtils.setVisibilityAnimateProvider(mBottomLayout, mBottomProvider);
            if (firstInit) {
                PlayerVisibilityUtils.setTargetVisibility(mBottomLayout, GONE, 0);
            } else {
                PlayerVisibilityUtils.setTargetVisibility(mBottomLayout, VISIBLE);
            }
        }

        mCenterLayout = mControlView.findViewById(R.id.player_control_center_layout);
        if (mCenterLayout != null) {
            if (firstInit) {
                PlayerVisibilityUtils.setTargetVisibility(mCenterLayout, GONE, 0);
            } else {
                PlayerVisibilityUtils.setTargetVisibility(mCenterLayout, VISIBLE);
            }
        }

        mLockLayout = mControlView.findViewById(R.id.player_control_lock_layout);
        if (mLockLayout != null) {
            if (firstInit) {
                PlayerVisibilityUtils.setTargetVisibility(mLockLayout, GONE, 0);
            } else {
                PlayerVisibilityUtils.setTargetVisibility(mLockLayout, VISIBLE);
            }
        }

        mPlayView = mControlView.findViewById(R.id.player_control_play);
        if (mPlayView != null) {
            mPlayView.setOnClickListener(componentListener);
        }

        mPauseView = mControlView.findViewById(R.id.player_control_pause);
        if (mPauseView != null) {
            mPauseView.setOnClickListener(componentListener);
        }

        mPositionView = mControlView.findViewById(R.id.player_control_position);
        if (mPositionView != null) {
            mPositionView.setOnClickListener(componentListener);
        }

        mDurationView = mControlView.findViewById(R.id.player_control_duration);
        if (mDurationView != null) {
            mDurationView.setOnClickListener(componentListener);
        }

        mProgressBar = mControlView.findViewById(R.id.player_control_progress);
        if (mProgressBar != null) {
            mProgressBar.setOnSeekBarChangeListener(componentListener);
            mProgressBar.setMax(PROGRESS_BAR_MAX);
        }

        mIsLocking = false; // reset
        mLockView = mControlView.findViewById(R.id.player_control_lock);
        if (mLockView != null) {
            mLockView.setOnClickListener(componentListener);
            PlayerVisibilityUtils.setTargetVisibility(mLockView, GONE);
        }

        mUnLockView = mControlView.findViewById(R.id.player_control_unlock);
        if (mUnLockView != null) {
            mUnLockView.setOnClickListener(componentListener);
            PlayerVisibilityUtils.setTargetVisibility(mUnLockView, VISIBLE);
        }
    }


    private void computeControlViewSize() {
        int screenWidth = getScreenWidth(getContext());
        int screenHeight = getScreenHeight(getContext());

        switch (mCurrentViewMode) {
            case PLAYERSCREENMODE_PORTRAIT_INSET:
                mControlViewWidth = screenWidth;
                mControlViewHeight = (screenWidth / 16 * 9);
                break;
            case PLAYERSCREENMODE_PORTRAIT_FULLSCREEN:
            case PLAYERSCREENMODE_LANDSCAPE_FULLSCREEN:
                mControlViewWidth = screenWidth;
                mControlViewHeight = screenHeight;
                break;
        }
    }

    private static int getScreenWidth(Context context) {
        if (context == null)
            return 0;

        int screenWidth;
        DisplayMetrics dm = context.getResources().getDisplayMetrics();
        if (dm != null) {
            screenWidth = dm.widthPixels;
            return screenWidth;
        }
        return 0;
    }

    private static int getScreenHeight(Context context) {
        if (context == null)
            return 0;

        int screenHeight;
        DisplayMetrics dm = context.getResources().getDisplayMetrics();
        if (dm != null) {
            screenHeight = dm.heightPixels;
            return screenHeight;
        }
        return 0;
    }

    @Override
    public void setPlayer(IPlayer player) {
        if (this.mPlayer == player) {
            return;
        }
        if (this.mPlayer != null) {
            this.mPlayer.removeVideoStateListener(componentListener);
        }
        this.mPlayer = player;
        if (player != null) {
            player.addVideoStateListener(componentListener);
        }
        updateAll();
    }

    @Override
    public IPlayer getPlayer() {
        return mPlayer;
    }

    @Override
    public int getVideoViewHeight() {
        return mControlViewHeight;
    }

    @Override
    public int getVideoViewWidth() {
        return mControlViewWidth;
    }

    @Override
    public void setScreenMode(int screenMode) {
        if (this.mCurrentViewMode == screenMode) return;
        this.mCurrentViewMode = screenMode;
        initControllerViewByOrientation();
        hide();
    }

    @Override
    public void show() {
        showMaskView();
        if (!mIsLocking) {
            showControlView();
            updateAll();
        }
        showLockView();
        if (mOnControlViewListener != null) {
            mOnControlViewListener.onVisibilityChange(true);
        }
        hideAfterTimeout();
    }

    /**
     * Hides the controller.
     */
    @Override
    public void hide() {
        // 如果允许自己做动画 就自己做,否则就跟随整体一起消失就好了
        if (!mIsLocking) {
            hideControlView();
        }
        hideLockView();
        hideMaskView();
        if (mOnControlViewListener != null) {
            mOnControlViewListener.onVisibilityChange(false);
        }
        removeCallbacks(updateProgressAction);
        removeCallbacks(hideAction);
    }

    @Override
    public void hideAfterTimeout() {
        removeCallbacks(hideAction);
        if (mShowTimeoutMs > 0) {
                postDelayed(hideAction, mShowTimeoutMs);
        }
    }

    public int getControllerShowTimeoutMs() {
        return mShowTimeoutMs;
    }

    public void setControllerShowTimeoutMs(int controllerShowTimeoutMs) {
        if (this.mShowTimeoutMs == controllerShowTimeoutMs) {
            return;
        }
        this.mShowTimeoutMs = controllerShowTimeoutMs;
    }

    /**
     * 设置PlayControlView的 出现 消失动画
     */
    @Override
    public void setAnimateProvider(PlayerVisibilityUtils.VisibilityAnimateProvider provider) {
        PlayerVisibilityUtils.setVisibilityAnimateProvider(this, provider);
    }

    /**
     * 设置PlayControlView topLayout 的 出现 消失动画
     */
    @Override
    public void setTopAnimateProvider(PlayerVisibilityUtils.VisibilityAnimateProvider topProvider) {
        this.mTopProvider = topProvider;
        if (mTopLayout != null) {
            PlayerVisibilityUtils.setVisibilityAnimateProvider(mTopLayout, topProvider);
        }
    }

    /**
     * 设置PlayControlView的 bottomLayout 的出现 消失动画
     */
    @Override
    public void setBottomAnimateProvider(PlayerVisibilityUtils.VisibilityAnimateProvider bottomProvider) {
        this.mBottomProvider = bottomProvider;
        if (mBottomLayout != null) {
            PlayerVisibilityUtils.setVisibilityAnimateProvider(mBottomLayout, bottomProvider);
        }
    }

    /**
     * Returns whether the controller is currently visible.
     */
    @Override
    public boolean isVisible() {
        boolean isVisible = false;
        switch (mCurrentViewMode) {
            case PLAYERSCREENMODE_PORTRAIT_INSET:
            case PLAYERSCREENMODE_PORTRAIT_FULLSCREEN:
                isVisible = mMaskLayout != null && PlayerVisibilityUtils.isTargetVisible(mMaskLayout);
                break;
            case PLAYERSCREENMODE_LANDSCAPE_FULLSCREEN:
                if (mIsLocking) {
                    isVisible = mLockLayout != null && PlayerVisibilityUtils.isTargetVisible(mLockLayout);
                } else {
                    isVisible = mMaskLayout != null && PlayerVisibilityUtils.isTargetVisible(mMaskLayout);
                }
                break;
        }
        return isVisible;
    }


    @Override
    public boolean isLocking() {
        return mIsLocking;
    }

    @Override
    public void setOnControlViewListener(OnControlViewListener listener) {
        if (mOnControlViewListener == listener) {
            return;
        }
        this.mOnControlViewListener = listener;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        isAttachedToWindow = true;
        updateAll();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        isAttachedToWindow = false;
        removeCallbacks(updateProgressAction);
        removeCallbacks(hideAction);
        hide();
    }

    private void showMaskView() {
        if (mMaskLayout != null) {
            PlayerVisibilityUtils.setTargetVisibility(mMaskLayout, VISIBLE);
        }
    }

    private void hideMaskView() {
        if (mMaskLayout != null) {
            PlayerVisibilityUtils.setTargetVisibility(mMaskLayout, GONE);
        }
    }

    private void showLockView() {
        if (mLockLayout != null) {
            PlayerVisibilityUtils.setTargetVisibility(mLockLayout, VISIBLE);
        }
    }

    private void hideLockView() {
        if (mLockLayout != null) {
            PlayerVisibilityUtils.setTargetVisibility(mLockLayout, GONE);
        }
    }

    private void showControlView() {
        if (mTopLayout != null) {
            // 如果允许自己做动画 就动画变成Visible.
            PlayerVisibilityUtils.setTargetVisibility(mTopLayout, VISIBLE);
        }
        if (mBottomLayout != null) {
            PlayerVisibilityUtils.setTargetVisibility(mBottomLayout, VISIBLE);
        }
        if (mCenterLayout != null) {
            PlayerVisibilityUtils.setTargetVisibility(mCenterLayout, VISIBLE);
        }
    }

    private void hideControlView() {
        if (mTopLayout != null) {
            // 如果允许自己做动画 就动画变成Visible.
            PlayerVisibilityUtils.setTargetVisibility(mTopLayout, GONE);
        }
        if (mBottomLayout != null) {
            PlayerVisibilityUtils.setTargetVisibility(mBottomLayout, GONE);
        }
        if (mCenterLayout != null) {
            PlayerVisibilityUtils.setTargetVisibility(mCenterLayout, GONE);
        }
    }

    private void updateAll() {
        updatePlayPauseButton();
        updateProgress();
    }

    private void updatePlayPauseButton() {
        if (!isVisible() || !isAttachedToWindow) {
            return;
        }
        boolean playing = mPlayer != null && mPlayer.isPlaying();
        if (mPlayView != null) {
            mPlayView.setVisibility(playing ? View.GONE : View.VISIBLE);
        }
        if (mPauseView != null) {
            mPauseView.setVisibility(!playing ? View.GONE : View.VISIBLE);
        }
    }

    private void updateProgress() {
        if (!isVisible() || !isAttachedToWindow) {
            return;
        }
        long duration = mPlayer == null ? 0 : mPlayer.getDuration();
        long position = mPlayer == null ? 0 : mPlayer.getCurrentPosition();
        int bufferPercentage = mPlayer == null ? 0 : mPlayer.getBufferPercentage();
        if (mDurationView != null) {
            mDurationView.setText(stringForTime(duration));
        }
        if (mPositionView != null && !dragging) {
            mPositionView.setText(stringForTime(position));
        }
        if (mProgressBar != null) {
            if (duration > 0) {
                long pos = 1000L * position / duration;
                mProgressBar.setProgress((int) pos);
            }
            mProgressBar.setSecondaryProgress(bufferPercentage * 10);
        }
        removeCallbacks(updateProgressAction);
        // Schedule an update if necessary.
        int playState = mPlayer == null ? IPlayer.STATE_IDLE : mPlayer.getCurrentPlayerState();
        switch (playState) {
            case IPlayer.STATE_PLAYING:
                postDelayed(updateProgressAction, DEFAULT_UPDATE_PROGRESS_MS);
                break;
        }
    }


    private String stringForTime(long timeMs) {
        mFormatBuilder.setLength(0);
        if (timeMs < 0) {
            return (mFormatter.format("%02d:%02d", 0, 0).toString());
        }
        long totalSeconds = timeMs / 1000;
        long seconds = totalSeconds % 60;
        long minutes = (totalSeconds / 60) % 60;
        long hours = totalSeconds / 3600;

        if (hours > 0) {
            return (mFormatter.format("%d:%02d:%02d", hours, minutes, seconds).toString());
        } else {
            return (mFormatter.format("%02d:%02d", minutes, seconds).toString());
        }
    }

    private long positionValue(int progress) {
        long duration = mPlayer == null ? 0 : mPlayer.getDuration();
        return duration == 0 ? 0 : ((duration * progress) / PROGRESS_BAR_MAX);
    }


    private final class ComponentListener implements IVideoStateListener,
            SeekBar.OnSeekBarChangeListener, OnClickListener {
        @Override
        public void onClick(View v) {
            // 点击重置隐藏时长
            if (mPlayer != null) {
                if (mPlayView == v) {
                    mPlayer.play();
                    hide();
                } else if (mPauseView == v) {
                    mPlayer.pause();
                    hideAfterTimeout();
                } else if (mLockView == v) {
                    //  进入非锁屏状态
                    mIsLocking = false;
                    if (mUnLockView != null) {
                        PlayerVisibilityUtils.setTargetVisibility(mUnLockView, VISIBLE);
                    }
                    if (mLockView != null) {
                        PlayerVisibilityUtils.setTargetVisibility(mLockView, GONE);
                    }
                    show();
                    if (mOnControlViewListener != null) {
                        mOnControlViewListener.onLockStateChange(mIsLocking);
                    }
                } else if (mUnLockView == v) {
                    // 进入锁屏状态
                    mIsLocking = true;
                    if (mUnLockView != null) {
                        PlayerVisibilityUtils.setTargetVisibility(mUnLockView, GONE);
                    }
                    if (mLockView != null) {
                        PlayerVisibilityUtils.setTargetVisibility(mLockView, VISIBLE);
                    }
                    hideControlView();
                    hideAfterTimeout();
                    if (mOnControlViewListener != null) {
                        mOnControlViewListener.onLockStateChange(mIsLocking);
                    }
                }
            }
        }

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            if (fromUser && mPositionView != null) {
                mPositionView.setText(stringForTime(positionValue(progress)));
            }
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            removeCallbacks(hideAction);
            dragging = true;
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            dragging = false;
            if (mPlayer != null) {
                mPlayer.seekTo(positionValue(seekBar.getProgress()));
            }
            hideAfterTimeout();
        }

        @Override
        public void onStateChanged(int oldState, int newState) {
            updatePlayPauseButton();
            updateProgress();
        }


        @Override
        public void onVolumeChanged(int startVolume, int finalVolume) {

        }
    }

    private static final PlayerVisibilityUtils.VisibilityAnimateProvider sMaskProvider = new PlayerVisibilityUtils.VisibilityAnimateProvider() {
        @Override
        @NonNull
        public Object onAppear(@NonNull View view) {
            if (view.getVisibility() != View.VISIBLE) {
                ViewCompat.setAlpha(view, 0);
            }
            return view.animate().setDuration(250).alpha(1);
        }

        @Override
        @NonNull
        public Object onDisappear(@NonNull View view) {
            return view.animate().setDuration(250).alpha(0);
        }
    };

    private static final PlayerVisibilityUtils.VisibilityAnimateProvider sTopProvider = new PlayerVisibilityUtils.VisibilityAnimateProvider() {
        @NonNull
        @Override
        public Object onAppear(@NonNull View view) {
            return view.animate().setDuration(200).translationY(0);
        }

        @NonNull
        @Override
        public Object onDisappear(@NonNull View view) {
            return view.animate().setDuration(200).translationY(-view.getHeight());
        }
    };

    private static final PlayerVisibilityUtils.VisibilityAnimateProvider sBottomProvider = new PlayerVisibilityUtils.VisibilityAnimateProvider() {
        @NonNull
        @Override
        public Object onAppear(@NonNull View view) {
            return view.animate().setDuration(200).translationY(0);
        }

        @NonNull
        @Override
        public Object onDisappear(@NonNull View view) {
            return view.animate().setDuration(200).translationY(view.getHeight());
        }
    };


}

