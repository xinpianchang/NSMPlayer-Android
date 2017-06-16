package com.vmovier.lib.player.internal;


import android.support.annotation.CallSuper;

import com.vmovier.lib.player.MediaError;

@SuppressWarnings("WeakerAccess")
abstract class AbstractPlayer implements IInternalPlayer {
    private OnPreparedListener mOnPreparedListener;
    private OnCompletionListener mOnCompletionListener;
    private OnBufferingUpdateListener mOnBufferingUpdateListener;
    private OnSeekCompleteListener mOnSeekCompleteListener;
    private OnVideoSizeChangedListener mOnVideoSizeChangedListener;
    private OnErrorListener mOnErrorListener;
    private OnInfoListener mOnInfoListener;

    @CallSuper
    public void setOnPreparedListener(OnPreparedListener listener) {
        mOnPreparedListener = listener;
    }

    @CallSuper
    public void setOnCompletionListener(OnCompletionListener listener) {
        mOnCompletionListener = listener;
    }

    @CallSuper
    public void setOnBufferingUpdateListener(OnBufferingUpdateListener listener) {
        mOnBufferingUpdateListener = listener;
    }

    public void setOnSeekCompleteListener(OnSeekCompleteListener listener) {
        mOnSeekCompleteListener = listener;
    }

    @CallSuper
    public void setOnVideoSizeChangedListener(OnVideoSizeChangedListener listener) {
        mOnVideoSizeChangedListener = listener;
    }

    @CallSuper
    public void setOnErrorListener(OnErrorListener listener) {
        mOnErrorListener = listener;
    }

    @CallSuper
    public void setOnInfoListener(OnInfoListener listener) {
        mOnInfoListener = listener;
    }

    @CallSuper
    public void resetListeners() {
        mOnPreparedListener = null;
        mOnBufferingUpdateListener = null;
        mOnCompletionListener = null;
        mOnSeekCompleteListener = null;
        mOnVideoSizeChangedListener = null;
        mOnErrorListener = null;
        mOnInfoListener = null;
    }

    protected final void notifyOnPrepared() {
        if (mOnPreparedListener != null) {
            mOnPreparedListener.onPrepared(this);
        }
    }

    protected final void notifyOnCompletion() {
        if (mOnCompletionListener != null) {
            mOnCompletionListener.onCompletion(this);
        }
    }

    protected final void notifyOnBufferingUpdate(int percent) {
        if (mOnBufferingUpdateListener != null) {
            mOnBufferingUpdateListener.onBufferingUpdate(this, percent);
        }
    }

    protected final void notifyOnSeekComplete() {
        if (mOnSeekCompleteListener != null) {
            mOnSeekCompleteListener.onSeekComplete(this);
        }
    }

    protected final void notifyOnVideoSizeChanged(int width, int height,
                                                  int sarNum, int sarDen) {
        if (mOnVideoSizeChangedListener != null) {
            mOnVideoSizeChangedListener.onVideoSizeChanged(this, width, height,
                    sarNum, sarDen);
        }
    }

    protected final boolean notifyOnError(MediaError error) {
        return mOnErrorListener != null && mOnErrorListener.onError(this, error);
    }

    protected final boolean notifyOnInfo(int what, int extra) {
        return mOnInfoListener != null && mOnInfoListener.onInfo(this, what, extra);
    }

}
