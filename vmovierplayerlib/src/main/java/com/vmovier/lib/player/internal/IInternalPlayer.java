package com.vmovier.lib.player.internal;

import android.annotation.TargetApi;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.view.Surface;
import android.view.SurfaceHolder;

import com.vmovier.lib.player.MediaError;

import java.io.FileDescriptor;
import java.io.IOException;
import java.util.Map;

/**
 * 底层播放器的基本行为接口.
 */
@SuppressWarnings("unused")
public interface IInternalPlayer {
    /* Do not change these values without updating their counterparts in native */
    int MEDIA_INFO_UNKNOWN = 1;
    int MEDIA_INFO_STARTED_AS_NEXT = 2;
    int MEDIA_INFO_VIDEO_RENDERING_START = 3;
    int MEDIA_INFO_VIDEO_TRACK_LAGGING = 700;
    int MEDIA_INFO_BUFFERING_START = 701;
    int MEDIA_INFO_BUFFERING_END = 702;
    int MEDIA_INFO_NETWORK_BANDWIDTH = 703;
    int MEDIA_INFO_BAD_INTERLEAVING = 800;
    int MEDIA_INFO_NOT_SEEKABLE = 801;
    int MEDIA_INFO_METADATA_UPDATE = 802;
    int MEDIA_INFO_TIMED_TEXT_ERROR = 900;
    int MEDIA_INFO_UNSUPPORTED_SUBTITLE = 901;
    int MEDIA_INFO_SUBTITLE_TIMED_OUT = 902;

    int MEDIA_INFO_VIDEO_ROTATION_CHANGED = 10001;
    int MEDIA_INFO_AUDIO_RENDERING_START = 10002;

    int PLAYERTYPE_ANDROIDMEDIA = 1;
    int PLAYERTYPE_EXO = 2;

    void setDisplay(SurfaceHolder sh);

    void setSurface(Surface surface);

    void setDataSource(Context context, Uri uri)
            throws IOException, IllegalArgumentException, SecurityException, IllegalStateException;

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    void setDataSource(Context context, Uri uri, Map<String, String> headers)
            throws IOException, IllegalArgumentException, SecurityException, IllegalStateException;

    void setDataSource(FileDescriptor fd)
            throws IOException, IllegalArgumentException, IllegalStateException;

    void setDataSource(String path)
            throws IOException, IllegalArgumentException, SecurityException, IllegalStateException;

    String getDataSource();

    void prepareAsync() throws IllegalStateException;

    void start() throws IllegalStateException;

    void stop() throws IllegalStateException;

    void pause() throws IllegalStateException;

    int getVideoWidth();

    int getVideoHeight();

    boolean isPlaying();

    void seekTo(long msec) throws IllegalStateException;

    long getCurrentPosition();

    long getDuration();

    void release();

    void reset();

    int getBufferedPercentage();

    void setVolume(float volume);

    void setOnPreparedListener(OnPreparedListener listener);

    void setOnCompletionListener(OnCompletionListener listener);

    void setOnBufferingUpdateListener(
            OnBufferingUpdateListener listener);

    void setOnSeekCompleteListener(
            OnSeekCompleteListener listener);

    void setOnVideoSizeChangedListener(
            OnVideoSizeChangedListener listener);

    void setOnErrorListener(OnErrorListener listener);

    void setOnInfoListener(OnInfoListener listener);

    interface OnPreparedListener {
        void onPrepared(IInternalPlayer mp);
    }

    interface OnCompletionListener {
        void onCompletion(IInternalPlayer mp);
    }

    interface OnBufferingUpdateListener {
        void onBufferingUpdate(IInternalPlayer mp, int percent);
    }

    interface OnSeekCompleteListener {
        void onSeekComplete(IInternalPlayer mp);
    }

    interface OnVideoSizeChangedListener {
        void onVideoSizeChanged(IInternalPlayer mp, int width, int height,
                                int sar_num, int sar_den);
    }

    interface OnErrorListener {
        boolean onError(IInternalPlayer mp, MediaError error);
    }

    interface OnInfoListener {
        boolean onInfo(IInternalPlayer mp, int what, int extra);
    }

    /*--------------------
     * Optional
     */

    void setAudioStreamType(int streamtype);

    /**
     * SAR，Sample Aspect Ratio 采样纵横比。即视频横向对应的像素个数比上视频纵向的像素个数。即为我们通常提到的分辨率。
     * PAR，Pixel Aspect Ratio 像素宽高比。如果把像素想象成一个长方形，PAR即为这个长方形的长与宽的比。
     *                         大多数情况为1:1,就是一个正方形像素，否则为长方形像素
     * DAR，Display Aspect Ratio 显示宽高比。即最终播放出来的画面的宽与高之比。
     * 这三者的关系PAR x SAR = DAR或者PAR = DAR/SAR.
     * 在这里 SAR = SarNum / SarDen
     * @return SarNum
     */
    int getVideoSarNum();

    int getVideoSarDen();

    void setLooping(boolean looping);

    boolean isLooping();

    int getPlayerType();
}
