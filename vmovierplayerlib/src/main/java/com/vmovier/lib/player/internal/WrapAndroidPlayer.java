package com.vmovier.lib.player.internal;

import android.annotation.TargetApi;
import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;

import com.vmovier.lib.player.MediaError;
import com.vmovier.lib.utils.PlayerLog;

import java.io.FileDescriptor;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.Map;

class WrapAndroidPlayer extends AbstractPlayer {
    private final String TAG = WrapAndroidPlayer.class.getSimpleName();
    private final MediaPlayer mInternalMediaPlayer;
    private final AndroidMediaPlayerListenerHolder mInternalListenerAdapter;
    private String mDataSource;

    private boolean mIsReleased;
    private int mBufferedPercentage = 0;

    private static int PLAYER_ID = 0;
    private final int mId;

    WrapAndroidPlayer() {
        mInternalMediaPlayer = new InternalAndroidPlayer();
        mInternalMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mInternalListenerAdapter = new AndroidMediaPlayerListenerHolder(this);

        PLAYER_ID ++;
        mId = PLAYER_ID;
        PlayerLog.d("Lifecycle", "WrapAndroidPlayer wrapClass init , Player Id is " + mId);
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        PlayerLog.d("Lifecycle", "WrapAndroidPlayer wrapClass finalize , Player Id is " + mId);
    }

    @Override
    public void setDisplay(SurfaceHolder sh) {
        Log.d(TAG, "setDisplay# mIsReleased -->" + mIsReleased);
        if (!mIsReleased) {
            /**
             * 设置进来的Surface 只有准备好或者为null的时候 才能为源生的播放器 setDisPlay.
             * 否则会报IllegalStateException, the surfaceView has be released.
             */
            if (sh == null || sh.getSurface().isValid()) {
                mInternalMediaPlayer.setDisplay(sh);
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    @Override
    public void setSurface(Surface surface) {
        mInternalMediaPlayer.setSurface(surface);
    }

    @Override
    public void setDataSource(Context context, Uri uri)
            throws IOException, IllegalArgumentException, SecurityException, IllegalStateException {
        mInternalMediaPlayer.setDataSource(context, uri);
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    @Override
    public void setDataSource(Context context, Uri uri, Map<String, String> headers)
            throws IOException, IllegalArgumentException, SecurityException, IllegalStateException {
        mInternalMediaPlayer.setDataSource(context, uri, headers);
    }

    @Override
    public void setDataSource(FileDescriptor fd)
            throws IOException, IllegalArgumentException, IllegalStateException {
        mInternalMediaPlayer.setDataSource(fd);
    }

    @Override
    public void setDataSource(String path) throws IOException,
            IllegalArgumentException, SecurityException, IllegalStateException {
        mDataSource = path;

        Uri uri = Uri.parse(path);
        String scheme = uri.getScheme();
        if (!TextUtils.isEmpty(scheme) && scheme.equalsIgnoreCase("file")) {
            mInternalMediaPlayer.setDataSource(uri.getPath());
        } else {
            mInternalMediaPlayer.setDataSource(path);
        }
    }

    @Override
    public String getDataSource() {
        return mDataSource;
    }

    @Override
    public void prepareAsync() {
        Log.d(TAG, "prepareAsync");
        try {
            attachInternalListeners();
            mInternalMediaPlayer.prepareAsync();
        } catch (IllegalStateException e) {
            // donothing.
        }

    }

    @Override
    public void start() {
        Log.d(TAG, "start");
        try {
            mInternalMediaPlayer.start();
        } catch (IllegalStateException e) {
            // do nothing.
        }

    }

    @Override
    public void stop() {
        Log.d(TAG, "suspend");
        try {
            mInternalMediaPlayer.stop();
        } catch (IllegalStateException e){
            // do nothing.
        }

    }

    @Override
    public void pause() {
        Log.d(TAG, "pause");
        try {
            mInternalMediaPlayer.pause();
        } catch (IllegalStateException e) {
            // do nothing.
        }

    }

    @Override
    public int getVideoWidth() {
        return mInternalMediaPlayer.getVideoWidth();
    }

    @Override
    public int getVideoHeight() {
        return mInternalMediaPlayer.getVideoHeight();
    }

    @Override
    public int getVideoSarNum() {
        return 1;
    }

    @Override
    public int getVideoSarDen() {
        return 1;
    }

    @Override
    public boolean isPlaying() {
        try {
            return mInternalMediaPlayer.isPlaying();
        } catch (IllegalStateException e) {
            return false;
        }
    }

    @Override
    public void seekTo(long msec) throws IllegalStateException {
        long duration = getDuration();
        if (((msec + 3000) >= duration) && duration != 0) {
            msec = duration - 2000;
        }
        mInternalMediaPlayer.seekTo((int) msec);
    }

    @Override
    public long getCurrentPosition() {
        try {
            return mInternalMediaPlayer.getCurrentPosition();
        } catch (IllegalStateException e) {
            return 0;
        }
    }

    @Override
    public long getDuration() {
        try {
            return mInternalMediaPlayer.getDuration();
        } catch (IllegalStateException e) {
            return 0;
        }
    }

    @Override
    public int getBufferedPercentage() {
        return mBufferedPercentage;
    }

    @Override
    public void release() {
        PlayerLog.d(TAG, "release");
        mIsReleased = true;
        mInternalMediaPlayer.release();
        resetListeners();
    }

    @Override
    public void reset() {
        PlayerLog.d(TAG, "reset");
        try {
            mInternalMediaPlayer.reset();
        } catch (IllegalStateException e) {
        }
        resetListeners();
        attachInternalListeners();
    }

    @Override
    public void setLooping(boolean looping) {
        mInternalMediaPlayer.setLooping(looping);
    }

    @Override
    public boolean isLooping() {
        return mInternalMediaPlayer.isLooping();
    }

    @Override
    public void setVolume(float volume) {
        mInternalMediaPlayer.setVolume(volume, volume);
    }

    @Override
    public void setAudioStreamType(int streamtype) {
        mInternalMediaPlayer.setAudioStreamType(streamtype);
    }

    @Override
    public int getPlayerType() {
        return PLAYERTYPE_ANDROIDMEDIA;
    }

    /*--------------------
         * Listeners adapter
         */
    private void attachInternalListeners() {
        mInternalMediaPlayer.setOnPreparedListener(mInternalListenerAdapter);
        mInternalMediaPlayer
                .setOnBufferingUpdateListener(mInternalListenerAdapter);
        mInternalMediaPlayer.setOnCompletionListener(mInternalListenerAdapter);
        mInternalMediaPlayer
                .setOnSeekCompleteListener(mInternalListenerAdapter);
        mInternalMediaPlayer
                .setOnVideoSizeChangedListener(mInternalListenerAdapter);
        mInternalMediaPlayer.setOnErrorListener(mInternalListenerAdapter);
        mInternalMediaPlayer.setOnInfoListener(mInternalListenerAdapter);
    }

    private class AndroidMediaPlayerListenerHolder implements
            MediaPlayer.OnPreparedListener, MediaPlayer.OnCompletionListener,
            MediaPlayer.OnBufferingUpdateListener,
            MediaPlayer.OnSeekCompleteListener,
            MediaPlayer.OnVideoSizeChangedListener,
            MediaPlayer.OnErrorListener, MediaPlayer.OnInfoListener {
        public final WeakReference<WrapAndroidPlayer> mWeakMediaPlayer;

        public AndroidMediaPlayerListenerHolder(WrapAndroidPlayer mp) {
            mWeakMediaPlayer = new WeakReference<>(mp);
        }

        @Override
        public boolean onInfo(MediaPlayer mp, int what, int extra) {
            WrapAndroidPlayer self = mWeakMediaPlayer.get();
            PlayerLog.d(TAG, "onInfo#   what -->" + what + "   extra -->" + extra);
            return self != null && notifyOnInfo(what, extra);
        }

        @Override
        public boolean onError(MediaPlayer mp, int what, int extra) {
            WrapAndroidPlayer self = mWeakMediaPlayer.get();
            /** workAround, some time it's not a error */
            if (extra == MediaError.INFO_FORMAT_CHANGED || extra == MediaError.INFO_DISCONTINUITY
                    || extra == MediaError.INFO_OUTPUT_BUFFERS_CHANGED) {
                PlayerLog.d(TAG, "onError but it's not technically an error and the errorCode is " + extra);
                return false;
            }
            PlayerLog.d(TAG, "onError what is " + what + " , extra is " + extra);
            return self != null && notifyOnError(new MediaError(extra));
        }

        @Override
        public void onVideoSizeChanged(MediaPlayer mp, int width, int height) {
            WrapAndroidPlayer self = mWeakMediaPlayer.get();
            if (self == null) {
                return;
            }

            notifyOnVideoSizeChanged(width, height, 1, 1);
        }

        @Override
        public void onSeekComplete(MediaPlayer mp) {
            WrapAndroidPlayer self = mWeakMediaPlayer.get();
            if (self == null) {
                return;
            }

            notifyOnSeekComplete();
        }

        @Override
        public void onBufferingUpdate(MediaPlayer mp, int percent) {
            WrapAndroidPlayer self = mWeakMediaPlayer.get();
            if (self == null) {
                return;
            }
            mBufferedPercentage = percent;
            notifyOnBufferingUpdate(percent);
        }

        @Override
        public void onCompletion(MediaPlayer mp) {
            WrapAndroidPlayer self = mWeakMediaPlayer.get();
            if (self == null){
                return;
            }
            self.seekTo(0);
            // workAround MediaPlayer 在有的机器上onCompletion调用start,会播放一瞬间最后一帧的画面,然后再次回调
            // onCompletion, 再调用start 就没问题了,暂时先这样解决
            notifyOnCompletion();
        }

        @Override
        public void onPrepared(MediaPlayer mp) {
            WrapAndroidPlayer self = mWeakMediaPlayer.get();
            if (self == null) {
                return;
            }

            notifyOnPrepared();
        }
    }
}
