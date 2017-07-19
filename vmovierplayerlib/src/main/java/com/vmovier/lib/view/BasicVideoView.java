package com.vmovier.lib.view;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.vmovier.lib.player.IPlayer;
import com.vmovier.lib.player.VideoSize;
import com.vmovier.lib.utils.PlayerLog;
import com.vmovier.lib.view.render.IRenderView;
import com.vmovier.lib.view.render.SurfaceRenderView;
import com.vmovier.lib.view.render.TextureRenderView;
import com.vmovier.player.R;


@SuppressWarnings("unused")
public class BasicVideoView extends FrameLayout {
    private static final String TAG = BasicVideoView.class.getSimpleName();

    // 是否需要显示海报View
    protected boolean mNeedShowPosterView;
    // 渲染View 裁剪模式
    protected int mScaleType;
    // 海报动画时间
    protected int mPosterAnimatorDuration;
    // 渲染模式
    protected int mRenderType;
    // 控制层View 显示时间
    protected int mControllerShowTimeoutMs;
    // 是否使用默认的Controller
    protected boolean mUseController = true;

    protected IPlayer mPlayer;
    protected ImageView mPosterView;
    protected String mPosterUrl;
    protected VideoSize mVideoSize = new VideoSize();
    protected IRenderView mRenderView;
    protected IRenderView.ISurfaceHolder mSurfaceHolder;
    protected IVideoListener mVideoListener;
    protected PlayerControlView mControlView;
    protected GestureDetector mGestureDetector;
    protected int mScreenMode = PLAYERSCREENMODE_PORTRAIT_INSET;

    // 竖屏小屏模式
    public static final int PLAYERSCREENMODE_PORTRAIT_INSET = 1;
    // 竖屏全屏模式
    public static final int PLAYERSCREENMODE_PORTRAIT_FULLSCREEN = 2;
    // 横屏全屏模式
    public static final int PLAYERSCREENMODE_LANDSCAPE_FULLSCREEN = 3;

    public static final int SCALE_FIT_PARENT = IRenderView.SCALE_FIT_PARENT; // without clip
    public static final int SCALE_FILL_PARENT = IRenderView.SCALE_FILL_PARENT; // may clip
    public static final int SCALE_WRAP_CONTENT = IRenderView.SCALE_WRAP_CONTENT;
    public static final int SCALE_MATCH_PARENT = IRenderView.SCALE_MATCH_PARENT;
    public static final int SCALE_16_9_FIT_PARENT = IRenderView.SCALE_16_9_FIT_PARENT;
    public static final int SCALE_4_3_FIT_PARENT = IRenderView.SCALE_4_3_FIT_PARENT;

    public static final int RENDER_NONE = 0;
    public static final int RENDER_SURFACE_VIEW = 1;
    public static final int RENDER_TEXTURE_VIEW = 2;

    // 默认海报动画时间
    protected static final int DEFAULT_POSTER_ANIMATOR_DURATION = 650;
    // 默认显示控制面板时间
    protected static final int DEFAULT_SHOWCONTROLLER_TIMEOUT_MS = 5000;

    private static int instance = 0;
    private int instanceId;
    protected OnGenerateGestureDetectorListener mOnGenerateGestureDetectorListener;

    public BasicVideoView(Context context) {
        this(context, null);
    }

    public BasicVideoView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BasicVideoView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initVideoView(context, attrs, defStyleAttr, 0);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public BasicVideoView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initVideoView(context, attrs, defStyleAttr, defStyleRes);
    }

    protected void initVideoView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        PlayerLog.d(TAG, "initVideoView");
        instanceId = ++instance;
        PlayerLog.d("Lifecycle", "BasicVideoView is created, My id is" + instanceId);

        if (attrs != null) {
            TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.BasicVideoView, 0, 0);
            try {
                mRenderType = a.getInteger(R.styleable.BasicVideoView_renderViewType, RENDER_SURFACE_VIEW);
                mScaleType = a.getInteger(R.styleable.BasicVideoView_scaleType, SCALE_FIT_PARENT);
                mUseController = a.getBoolean(R.styleable.BasicVideoView_useController, false);
                mControllerShowTimeoutMs = a.getInteger(R.styleable.BasicVideoView_controllerShowTimeoutMs, DEFAULT_SHOWCONTROLLER_TIMEOUT_MS);
                mNeedShowPosterView = a.getBoolean(R.styleable.BasicVideoView_needShowPosterView, false);
                mPosterAnimatorDuration = a.getInteger(R.styleable.BasicVideoView_posterAnimatorDuration, DEFAULT_POSTER_ANIMATOR_DURATION);
            } finally {
                a.recycle();
            }
        }

        mVideoListener = new BasicVideoListener();

        // 初始化渲染View
        setRender(mRenderType);
        // 初始化海报View
        initPosterView(context);
        // 初始化控制View
        initControlView(context, attrs);

        mOnGenerateGestureDetectorListener = new OnDefaultGenerateGestureDetectorListener();
        mGestureDetector = mOnGenerateGestureDetectorListener.generateGestureDetector(context, null, mControlView);

        setKeepScreenOn(true);
        setBackground(new ColorDrawable(Color.BLACK));
    }



    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        PlayerLog.d("Lifecycle", "BasicVideoView is GCed. My id is " + instanceId);
    }

    protected void setRender(int render) {
        switch (render) {
            case RENDER_NONE:
                setRenderView(null);
                break;
            case RENDER_SURFACE_VIEW:  {
                SurfaceRenderView renderView = new SurfaceRenderView(getContext());
                setRenderView(renderView);
                break;
            }
            case RENDER_TEXTURE_VIEW: {
                TextureRenderView renderView = new TextureRenderView(getContext());
                if (mPlayer != null) {
                    renderView.getSurfaceHolder().bindToMediaPlayer(mPlayer);
                    renderView.setVideoSize(mVideoSize.videoWidth, mVideoSize.videoHeight);
                    renderView.setVideoSampleAspectRatio(mVideoSize.videoSarNum, mVideoSize.videoSarDen);
                    renderView.setScaleType(mScaleType);
                }
                setRenderView(renderView);
                break;
            }
            default:
                break;
        }
    }

    protected void setRenderView(IRenderView renderView) {
        PlayerLog.d(TAG, "setRenderView");
        if (mRenderView != null) {
            if (mPlayer != null) {
                mPlayer.setDisplay(null);
            }
            View renderUIView = mRenderView.getView();
            mRenderView.removeRenderCallback(mSHCallback);
            mRenderView = null;
            removeView(renderUIView);
        }

        if (renderView == null) {
            return;
        }

        mRenderView = renderView;
        // 给渲染View 设置比例.
        renderView.setScaleType(mScaleType);
        if (mVideoSize.videoWidth > 0 && mVideoSize.videoHeight > 0) {
            renderView.setVideoSize(mVideoSize.videoWidth, mVideoSize.videoHeight);
        }
        if (mVideoSize.videoSarNum > 0 && mVideoSize.videoSarDen > 0) {
            renderView.setVideoSampleAspectRatio(mVideoSize.videoSarNum, mVideoSize.videoSarDen);
        }

        View renderUIView = mRenderView.getView();
        LayoutParams lp = new LayoutParams(
                LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT,
                Gravity.CENTER);
        renderUIView.setLayoutParams(lp);
        addView(renderUIView);

        mRenderView.addRenderCallback(mSHCallback);
    }

    protected void initControlView(Context context, AttributeSet attrs) {
        mControlView = new PlayerControlView(context, attrs);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                                          ViewGroup.LayoutParams.MATCH_PARENT,
                                          ViewGroup.LayoutParams.MATCH_PARENT);
        mControlView.setLayoutParams(lp);
        addView(mControlView);
    }

    protected void initPosterView(Context context) {
        mPosterView = new ImageView(context);
        LayoutParams lp = new LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT,
                Gravity.CENTER);
        mPosterView.setLayoutParams(lp);
        mPosterView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        addView(mPosterView);

        if (!mNeedShowPosterView) {
            hidePosterView(-1);
        }
    }

    protected IRenderView.IRenderCallback mSHCallback = new IRenderView.IRenderCallback() {
        @Override
        public void onSurfaceCreated(@NonNull IRenderView.ISurfaceHolder holder, int width, int height) {
            PlayerLog.d(TAG, "onSurfaceCreated  + " + this.toString());
            if (holder.getRenderView() != mRenderView) {
                PlayerLog.e(TAG, "onSurfaceCreated: unmatched render callback\n");
                return;
            }
            mSurfaceHolder = holder;
            bindSurfaceHolder(mPlayer, holder);
        }

        @Override
        public void onSurfaceChanged(@NonNull IRenderView.ISurfaceHolder holder, int format, int width, int height) {
            PlayerLog.d(TAG, "onSurfaceChanged");
        }

        @Override
        public void onSurfaceDestroyed(@NonNull IRenderView.ISurfaceHolder holder) {
            PlayerLog.d(TAG, "onSurfaceDestroyed  + " + this.toString());
            if (holder.getRenderView() != mRenderView) {
                PlayerLog.e(TAG, "onSurfaceDestroyed: unmatched render callback\n");
                return;
            }
            mSurfaceHolder = null;
            bindSurfaceHolder(mPlayer, holder);
        }
    };

    protected void bindSurfaceHolder(IPlayer mp, IRenderView.ISurfaceHolder holder) {
        if (mp == null)
            return;

        if (holder == null) {
            mp.setDisplay(null);
            return;
        }

        holder.bindToMediaPlayer(mp);
    }

    private void openSurfaceViewContent() {
        if (mRenderView != null) {
            View view = mRenderView.getView();
            view.setBackgroundColor(0x00000000);
        }
    }

    private void closeSurfaceViewContent() {
        if (mRenderView != null) {
            View view = mRenderView.getView();
            view.setBackgroundColor(0xFF000000);
        }
    }

    private Runnable mOpenSurfaceRunnable = new Runnable() {
        @Override
        public void run() {
            openSurfaceViewContent();
        }
    };

    protected void showPosterView() {
        showPosterView(mPosterAnimatorDuration);
    }

    protected void showPosterView(int duration) {
        if (mPosterView != null) {
            PlayerLog.d(TAG, "showPosterView PosterView != null");
            PlayerVisibilityUtils.fadeIn(mPosterView, duration);
        }
    }

    protected void hidePosterView() {
        hidePosterView(mPosterAnimatorDuration);
    }

    private void hidePosterView(int duration) {
        if (mPosterView != null) {
            PlayerVisibilityUtils.fadeOut(mPosterView, duration);
        }
    }

    public @Nullable ImageView getPosterView() {
        return mPosterView;
    }


    public boolean isNeedShowPosterView() {
        return mNeedShowPosterView;
    }

    public void setNeedShowPosterView(boolean needShowPosterView) {
        if (this.mNeedShowPosterView == needShowPosterView) {
            return;
        }
        this.mNeedShowPosterView = needShowPosterView;
    }

    public int getScaleType() {
        return mScaleType;
    }

    public void setScaleType(int scaleType) {
        if (this.mScaleType == scaleType) {
            return;
        }
        this.mScaleType = scaleType;
        if (mRenderView != null) {
            mRenderView.setScaleType(scaleType);
        }
    }

    public int getPosterAnimatorDuration() {
        return mPosterAnimatorDuration;
    }

    public void setPosterAnimatorDuration(int posterAnimatorDuration) {
        if (this.mPosterAnimatorDuration == posterAnimatorDuration) {
            return;
        }
        this.mPosterAnimatorDuration = posterAnimatorDuration;
    }

    public int getRenderType() {
        return mRenderType;
    }

    public void setRenderType(int renderType) {
        if (this.mRenderType == renderType) {
            return;
        }
        this.mRenderType = renderType;
        setRender(mRenderType);
    }

    public int getControllerShowTimeoutMs() {
        return mControllerShowTimeoutMs;
    }

    public void setControllerShowTimeoutMs(int controllerShowTimeoutMs) {
        if (this.mControllerShowTimeoutMs == controllerShowTimeoutMs) {
            return;
        }
        this.mControllerShowTimeoutMs = controllerShowTimeoutMs;
    }

    public boolean isControlViewVisible() {
        return mControlView != null && mControlView.isVisible();
    }

    public void setScreenMode(int screenMode) {
        if (mScreenMode == screenMode) return;
        mScreenMode = screenMode;

        if (mControlView == null) return;
        mControlView.setScreenMode(screenMode);
    }

    public void setAnimateProvider(PlayerVisibilityUtils.VisibilityAnimateProvider provider) {
        if (mControlView != null) {
            mControlView.setAnimateProvider(provider);
        }
    }

    public void setTopAnimateProvider(PlayerVisibilityUtils.VisibilityAnimateProvider topProvider) {
        if (mControlView != null) {
            mControlView.setTopAnimateProvider(topProvider);
        }
    }

    public void setBottomAnimateProvider(PlayerVisibilityUtils.VisibilityAnimateProvider bottomProvider) {
        if (mControlView != null) {
            mControlView.setBottomAnimateProvider(bottomProvider);
        }
    }

    public void setControllerVisibilityListener(OnControlViewVisibilityListener listener) {
        if (mControlView != null) {
            mControlView.setOnVisibilityListener(listener);
        }
    }

    public boolean isUseController() {
        return mUseController;
    }

    public void setUseController(boolean useController) {
        if (mControlView == null) return;
        if (this.mUseController == useController) return;
        this.mUseController = useController;
        if (useController) {
            mControlView.setPlayer(mPlayer);
        } else {
            mControlView.hide();
            mControlView.setPlayer(null);
        }

    }

    public void setPosterUrl(@NonNull String posterUrl) {
        this.mPosterUrl = posterUrl;
        if (mPosterView != null) {
            Glide.with(getContext()).load(posterUrl).centerCrop().into(mPosterView);
        }
    }

    public String getPosterUrl() {
        return mPosterUrl;
    }

    public @Nullable IPlayer getPlayer() {
        return mPlayer;
    }

    public void setPlayer(IPlayer player) {
        if (mPlayer == player) return;
        if (this.mPlayer != null) {
            this.mPlayer.removeVideoListener(mVideoListener);
            this.mPlayer.setDisplay(null);
            this.mPlayer = null;
        }

        this.mPlayer = player;
        if (mUseController && mControlView != null) {
            mControlView.setPlayer(player);
        }

        if (mOnGenerateGestureDetectorListener != null) {
            mGestureDetector = mOnGenerateGestureDetectorListener.generateGestureDetector(getContext(), mPlayer, mControlView);
        }

        if (mPlayer != null) {
            bindSurfaceHolder(mPlayer, mSurfaceHolder);
            mPlayer.addVideoListener(mVideoListener);
            if (mPlayer.isCurrentState(IPlayer.STATE_MASK_PREPARED)) {
                hidePosterView();
            }
            showController();
        } else {
            hideController();
        }
    }

    protected void maybeShowController() {
        if (!mUseController || mPlayer == null || mControlView == null) return;
        mControlView.show();
    }

    public void showController() {
        if (mUseController) {
            maybeShowController();
        }
    }

    public void hideController() {
        if (mControlView != null) {
            mControlView.hide();
        }
    }

    public void setOnGenerateGestureDetectorListener(@Nullable OnGenerateGestureDetectorListener listener) {
        if (mOnGenerateGestureDetectorListener == listener) return;
        mOnGenerateGestureDetectorListener = listener;
        if (mOnGenerateGestureDetectorListener == null) {
            mGestureDetector = null;
        } else {
            mGestureDetector = mOnGenerateGestureDetectorListener.generateGestureDetector(getContext(), mPlayer, mControlView);
        }
    }


    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        boolean handled = false;
        PlayerLog.d(TAG, "dispatchTouchEvent");
        if (mControlView != null) {
            handled = mControlView.dispatchTouchEvent(ev);
            if (handled) {
                // 重置计时器
                mControlView.hideAfterTimeout();
            }
            PlayerLog.d(TAG, "ControlView dispatchTouchEvent  handled:" + handled);
        }
        if (!handled && mGestureDetector != null) {
            handled = mGestureDetector.onTouchEvent(ev);
        }
        return handled;
    }

    public @Nullable Bitmap getScreenShot() {
        return mRenderView == null ? null : mRenderView.getScreenShot();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        PlayerLog.d("Lifecycle", "onAttachedToWindow");
        if (mControlView != null) {
            mControlView.setPlayer(mPlayer);
        }
        if (mPlayer != null) {
            mPlayer.addVideoListener(mVideoListener);
            if (mSurfaceHolder != null) {
                mSurfaceHolder.bindToMediaPlayer(mPlayer);
            }
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        PlayerLog.d("Lifecycle", "onDetachedFromWindow");
        if (mControlView != null) {
            mControlView.setPlayer(null);
        }
        if (mPlayer != null) {
            mPlayer.setDisplay(null);
            mPlayer.removeVideoListener(mVideoListener);
        }
    }

    protected class BasicVideoListener implements IVideoListener {
        @Override
        @CallSuper
        public void onStateChanged(int oldState, int newState) {
            PlayerLog.d(TAG, "onStateChanged oldState is " + oldState + " , newState is " + newState);
            switch (newState) {
                case IPlayer.STATE_COMPLETED:
                    showPosterView(0);
                    break;
                case IPlayer.STATE_PLAYING:
                    hidePosterView();
                    postDelayed(mOpenSurfaceRunnable, 500);
                    break;
                case IPlayer.STATE_PAUSING:
                    hidePosterView();
                    postDelayed(mOpenSurfaceRunnable, 500);
                    break;
                case IPlayer.STATE_PREPARING:
                    closeSurfaceViewContent();
                    break;
                case IPlayer.STATE_ERROR:
                    postDelayed(mOpenSurfaceRunnable, 500);
                    break;
            }
        }


        @Override
        public void onVolumeChanged(int startVolume, int finalVolume) {
            PlayerLog.d(TAG, "onVolumeChanged startVolume is " + startVolume + " , finalVolume is " + finalVolume);
        }

        @Override
        @CallSuper
        public void onVideoSizeChanged(IPlayer mp, VideoSize videoSize) {
            mVideoSize = videoSize;
            if (mVideoSize.videoWidth  != 0 && mVideoSize.videoHeight != 0) {
                if (mRenderView != null) {
                    mRenderView.setVideoSize(mVideoSize.videoWidth, mVideoSize.videoHeight);
                    mRenderView.setVideoSampleAspectRatio(mVideoSize.videoSarNum, mVideoSize.videoSarDen);
                }
            }
        }
    }
}
