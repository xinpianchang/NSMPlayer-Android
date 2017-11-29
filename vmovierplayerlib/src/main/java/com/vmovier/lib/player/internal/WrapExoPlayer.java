package com.vmovier.lib.player.internal;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.Surface;
import android.view.SurfaceHolder;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.mediacodec.MediaCodecRenderer;
import com.google.android.exoplayer2.mediacodec.MediaCodecUtil;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.dash.DashMediaSource;
import com.google.android.exoplayer2.source.dash.DefaultDashChunkSource;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.source.smoothstreaming.DefaultSsChunkSource;
import com.google.android.exoplayer2.source.smoothstreaming.SsMediaSource;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory;
import com.google.android.exoplayer2.upstream.HttpDataSource;
import com.google.android.exoplayer2.util.Util;
import com.vmovier.lib.player.MediaError;
import com.vmovier.lib.utils.PlayerLog;

import java.io.FileDescriptor;
import java.io.IOException;
import java.util.Map;


class WrapExoPlayer extends AbstractPlayer {
    private static final String TAG = WrapExoPlayer.class.getSimpleName();

    public static final String APP_NAME = "NSMPlayer";

    private String userAgent;
    private Context mAppContext;
    private SimpleExoPlayer mInternalMediaPlayer;
    private ExoEventListener mEventListener;
    private ExoVideoListener mVideoListener;
    private Uri mUri;
    private Map<String, String> mHeaders = null;
    private int mVideoWidth;
    private int mVideoHeight;
    private int mVideoSarNum = 1;
    private int mVideoSarDen = 1;
    private Handler mHandler;
    private DataSource.Factory mediaDataSourceFactory;
    private static final DefaultBandwidthMeter BANDWIDTH_METER = new DefaultBandwidthMeter();
    private int mInternalPlayerState = com.google.android.exoplayer2.ExoPlayer.STATE_IDLE;
    private static int PLAYER_ID = 0;
    private final int mId;
    private Surface mSurface;

    WrapExoPlayer(Context context) {
        mAppContext = context.getApplicationContext();
        userAgent = Util.getUserAgent(mAppContext, APP_NAME);
        mHandler = new Handler();
        mediaDataSourceFactory = buildDataSourceFactory(true);

        PLAYER_ID ++;
        mId = PLAYER_ID;
        PlayerLog.d("Lifecycle", "WrapExoPlayer WrapClass init , Player Id is " + mId);
    }

    @Override
    protected void finalize() throws Throwable{
        super.finalize();
        PlayerLog.d("Lifecycle", "WrapExoPlayer WrapClass finalize , Player Id is " + mId);
    }

    @Override
    public void setDisplay(SurfaceHolder sh) {
        if (sh == null) {
            setSurface(null);
        } else {
            setSurface(sh.getSurface());
        }
    }

    @Override
    public void setSurface(Surface surface) {
        mSurface = surface;
        if (mInternalMediaPlayer != null) {
            mInternalMediaPlayer.setVideoSurface(surface);
        }
    }

    @Override
    public void setDataSource(Context context, Uri uri) throws IOException, IllegalArgumentException, SecurityException, IllegalStateException {
        this.mUri = uri;
    }

    @Override
    public void setDataSource(Context context, Uri uri, Map<String, String> headers) throws IOException, IllegalArgumentException, SecurityException, IllegalStateException {
        this.mUri = uri;
        this.mHeaders = headers;
    }

    @Override
    public void setDataSource(FileDescriptor fd) throws IOException, IllegalArgumentException, IllegalStateException {
       // doNothing.
    }

    @Override
    public void setDataSource(String path) throws IOException, IllegalArgumentException, SecurityException, IllegalStateException {
        this.mUri = TextUtils.isEmpty(path) ? Uri.EMPTY : Uri.parse(path);
    }

    @Override
    public String getDataSource() {
        return null;
    }

    @Override
    public void prepareAsync() throws IllegalStateException {
        TrackSelection.Factory videoTrackSelectionFactory =
                new AdaptiveTrackSelection.Factory(BANDWIDTH_METER);
        DefaultTrackSelector trackSelector = new DefaultTrackSelector(videoTrackSelectionFactory);

        mInternalMediaPlayer = InternalExoPlayer.newInstance(mAppContext, trackSelector);

        mEventListener = new ExoEventListener();
        mInternalMediaPlayer.addListener(mEventListener);
        mVideoListener = new ExoVideoListener();
        mInternalMediaPlayer.addVideoListener(mVideoListener);

        MediaSource mediaSource = buildMediaSource(mUri, mHeaders);
        mInternalMediaPlayer.setVideoSurface(mSurface);
        mInternalMediaPlayer.prepare(mediaSource);
        notifyOnPrepared();
    }

    @Override
    public void release() {
        if (mInternalMediaPlayer != null) {
            mInternalMediaPlayer.removeListener(mEventListener);
            mInternalMediaPlayer.removeVideoListener(mVideoListener);
            mInternalMediaPlayer.release();
            mInternalMediaPlayer = null;
            mEventListener = null;
            mVideoListener = null;
        }
    }

    @Override
    public void start() throws IllegalStateException {
        if (mInternalMediaPlayer == null) {
            return;
        }

        // it's workAround, exoplayer 在 complete状态下 调用start. 竟然不会自己seekTo 0
        // exoPlayer 2.0.4 版本 竟然还有没有解决这个问题。
        if (mInternalPlayerState == com.google.android.exoplayer2.ExoPlayer.STATE_ENDED) {
            mInternalMediaPlayer.seekTo(0);
        }
        mInternalMediaPlayer.setPlayWhenReady(true);
    }

    @Override
    public void stop() throws IllegalStateException {
        if (mInternalMediaPlayer == null) {
            return;
        }
        mInternalMediaPlayer.stop();
    }

    @Override
    public void pause() throws IllegalStateException {
        if (mInternalMediaPlayer == null) {
            return;
        }
        mInternalMediaPlayer.setPlayWhenReady(false);
    }

    @Override
    public int getVideoWidth() {
        return mVideoWidth;
    }

    @Override
    public int getVideoHeight() {
        return mVideoHeight;
    }

    @Override
    public boolean isPlaying() {
        if (mInternalMediaPlayer == null) {
            return false;
        }

        int state = mInternalMediaPlayer.getPlaybackState();
        switch (state) {
            case Player.STATE_BUFFERING:
            case Player.STATE_READY:
                return mInternalMediaPlayer.getPlayWhenReady();
            case Player.STATE_IDLE:
            case Player.STATE_ENDED:
            default:
                return false;
        }
    }

    @Override
    public void seekTo(long msec) throws IllegalStateException {
        if (mInternalMediaPlayer != null) {
            long duration = getDuration();
            if (((msec + 3000) >= duration) && duration >= 0) {
                msec = duration - 2000;
            }
            mInternalMediaPlayer.seekTo(msec);
        }
    }

    @Override
    public long getCurrentPosition() {
        return mInternalMediaPlayer == null ? 0 : mInternalMediaPlayer.getCurrentPosition();
    }

    @Override
    public long getDuration() {
        return mInternalMediaPlayer == null ? 0 : mInternalMediaPlayer.getDuration();
    }


    @Override
    public void reset() {
        // TODO.
    }

    @Override
    public int getBufferedPercentage() {
        return mInternalMediaPlayer == null ? 0 : mInternalMediaPlayer.getBufferedPercentage();
    }

    @Override
    public void setVolume(float volume) {
        if (mInternalMediaPlayer != null) {
            mInternalMediaPlayer.setVolume(volume);
        }
    }

    @Override
    public void setAudioStreamType(int streamtype) {
    }

    @Override
    public int getVideoSarNum() {
        return mVideoSarNum;
    }

    @Override
    public int getVideoSarDen() {
        return mVideoSarDen;
    }

    @Override
    public void setLooping(boolean looping) {
    }

    @Override
    public boolean isLooping() {
        return false;
    }

    @Override
    public int getPlayerType() {
        return PLAYERTYPE_EXO;
    }

    private class ExoVideoListener implements SimpleExoPlayer.VideoListener {
        /**
         * Called each time there's a change in the size of the video being rendered.
         *
         * @param width The video width in pixels.
         * @param height The video height in pixels.
         * @param unappliedRotationDegrees For videos that require a rotation, this is the clockwise
         *     rotation in degrees that the application should apply for the video for it to be rendered
         *     in the correct orientation. This value will always be zero on API levels 21 and above,
         *     since the renderer will apply all necessary rotations internally. On earlier API levels
         *     this is not possible. Applications that use {@link android.view.TextureView} can apply
         *     the rotation by calling {@link android.view.TextureView#setTransform}. Applications that
         *     do not expect to encounter rotated videos can safely ignore this parameter.
         * @param pixelWidthHeightRatio The width to height ratio of each pixel. For the normal case
         *     of square pixels this will be equal to 1.0. Different values are indicative of anamorphic
         *     content.
         */
        @Override
        public void onVideoSizeChanged(int width, int height, int unappliedRotationDegrees, float pixelWidthHeightRatio) {
            mVideoHeight = height;
            mVideoWidth = width;
            if (height == 0 || width == 0) {
                mVideoSarDen = 1;
                mVideoSarNum = 1;
            } else {
                if (pixelWidthHeightRatio == 1) {
                    mVideoSarDen = 1;
                    mVideoSarNum = 1;
                } else {
                    mVideoSarDen = 10;
                    mVideoSarNum = (int) (mVideoSarDen * pixelWidthHeightRatio);
                }
            }
            PlayerLog.d(TAG, "onVideoSizeChanged width is " + width + " , height is " + height
                        + " , unappliedRotationDegrees is " + unappliedRotationDegrees
                        + " , pixelWidthHeightRatio is " + pixelWidthHeightRatio
                        + " , mVideoSarDen is " + mVideoSarDen + " , mVideoSarNum is " + mVideoSarNum
            );
            notifyOnVideoSizeChanged(width, height, mVideoSarNum, mVideoSarDen);
        }

        /**
         * Called when a frame is rendered for the first time since setting the surface, and when a
         * frame is rendered for the first time since a video track was selected.
         */
        @Override
        public void onRenderedFirstFrame() {
            PlayerLog.d(TAG, "onRenderedFirstFrame");
        }
    }

    private class ExoEventListener extends Player.DefaultEventListener {
        private boolean isBuffering = false;
        /**
         * Called when the value returned from either {@link ExoPlayer#getPlayWhenReady()} or
         * {@link ExoPlayer#getPlaybackState()} changes.
         *
         * @param playWhenReady Whether playback will proceed when ready.
         * @param playbackState One of the {@code STATE} constants defined in the {@link ExoPlayer}
         *     interface.
         */
        @Override
        public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
            if (mInternalMediaPlayer == null) {
                PlayerLog.d(TAG, "onPlayerStateChanged mInternalMediaPlayer == null");
                return;
            }
            mInternalPlayerState = playbackState;
            if (isBuffering) {
                switch (playbackState) {
                    case Player.STATE_ENDED:
                    case Player.STATE_READY:
                        notifyOnInfo(IInternalPlayer.MEDIA_INFO_BUFFERING_END, getBufferedPercentage());
                        isBuffering = false;
                        break;
                }
            }
            switch (playbackState) {
                case Player.STATE_IDLE:
                    PlayerLog.d(TAG, "WrapExoPlayer.STATE_IDLE");
                    break;
                case Player.STATE_BUFFERING:
                    PlayerLog.d(TAG, "WrapExoPlayer.STATE_BUFFERING");
                    notifyOnInfo(IInternalPlayer.MEDIA_INFO_BUFFERING_START, getBufferedPercentage());
                    isBuffering = true;
                    break;
                case Player.STATE_READY:
                    PlayerLog.d(TAG, "WrapExoPlayer.STATE_READY");
                    break;
                case Player.STATE_ENDED:
                    PlayerLog.d(TAG, "WrapExoPlayer.STATE_ENDED");
                    notifyOnCompletion();
                    break;
            }
        }

        /**
         * Called when an error occurs. The playback state will transition to {@link ExoPlayer#STATE_IDLE}
         * immediately after this method is called. The player instance can still be used, and
         * {@link #release()} must still be called on the player should it no longer be required.
         *
         * @param e The error.
         */
        @Override
        public void onPlayerError(ExoPlaybackException e) {
            MediaError error = null;
            if (e == null) {
                error = new MediaError(MediaError.ERROR_UNKNOWN);
            } else {
                if (e.type == ExoPlaybackException.TYPE_RENDERER) {
                    Exception cause = e.getRendererException();
                    if (cause instanceof MediaCodecRenderer.DecoderInitializationException) {
                        // Special case for decoder initialization failures.
                        MediaCodecRenderer.DecoderInitializationException decoderInitializationException =
                                (MediaCodecRenderer.DecoderInitializationException) cause;
                        if (decoderInitializationException.decoderName == null) {
                            if (decoderInitializationException.getCause() instanceof MediaCodecUtil.DecoderQueryException) {
                                error = new MediaError(MediaError.EXO_ERROR_QUERYING_DECODERS);
                            } else if (decoderInitializationException.secureDecoderRequired) {
                                error = new MediaError(MediaError.EXO_ERROR_NO_SECURE_DECODER);
                            } else {
                                error = new MediaError(MediaError.EXO_ERROR_NO_DECODER);
                            }
                        } else {
                            error = new MediaError(MediaError.EXO_ERROR_INSTANTIATING_DECODER);
                        }
                    }
                }
            }

            if (error == null) {
                error = new MediaError(MediaError.ERROR_UNKNOWN);
            }
            notifyOnError(error);
            PlayerLog.d(TAG, "onPlayerError   " + error.toString());
        }
    }

    private MediaSource buildMediaSource(Uri uri, @Nullable Map<String, String> headers) {
        if (uri.getLastPathSegment() == null) {
            // 防止传入空URI 导致getLastPathSegment 为空 造成空指针崩溃
            return new ExtractorMediaSource(uri, mediaDataSourceFactory, new DefaultExtractorsFactory(),
                    mHandler, null);
        }
        int type = Util.inferContentType(uri.getLastPathSegment());
        switch (type) {
            case C.TYPE_SS:
                return new SsMediaSource(uri, buildDataSourceFactory(false),
                        new DefaultSsChunkSource.Factory(mediaDataSourceFactory), mHandler, null);
            case C.TYPE_DASH:
                return new DashMediaSource(uri, buildDataSourceFactory(false),
                        new DefaultDashChunkSource.Factory(mediaDataSourceFactory), mHandler, null);
            case C.TYPE_HLS:
                if (headers == null) {
                    return new HlsMediaSource(uri, mediaDataSourceFactory, mHandler, null);
                } else {
                    HttpDataSource.Factory headersFactory = buildHttpDataSourceFactory(true);
                    headersFactory.getDefaultRequestProperties().set(mHeaders);
                    return new HlsMediaSource(uri, headersFactory, mHandler, null);
                }
            case C.TYPE_OTHER:
                if (headers == null) {
                    return new ExtractorMediaSource(uri, mediaDataSourceFactory, new DefaultExtractorsFactory(),
                            mHandler, null);
                } else {
                    HttpDataSource.Factory headersFactory = buildHttpDataSourceFactory(true);
                    headersFactory.getDefaultRequestProperties().set(headers);
                    return new ExtractorMediaSource(uri, headersFactory, new DefaultExtractorsFactory(),
                            mHandler, null);
                }
            default: {
                throw new IllegalStateException("Unsupported type: " + type);
            }
        }
    }

    /**
     * Returns a new DataSource factory.
     *
     * @param useBandwidthMeter Whether to set {@link #BANDWIDTH_METER} as a listener to the new
     *     DataSource factory.
     * @return A new DataSource factory.
     */
    private DataSource.Factory buildDataSourceFactory(boolean useBandwidthMeter) {
        // Produces DataSource instances through which media data is loaded.
        return new DefaultDataSourceFactory(mAppContext, userAgent, useBandwidthMeter ? BANDWIDTH_METER : null);
    }

    /**
     * Returns a new HttpDataSource factory.
     *
     * @param useBandwidthMeter Whether to set {@link #BANDWIDTH_METER} as a listener to the new
     *     DataSource factory.
     * @return A new HttpDataSource factory.
     */
    private HttpDataSource.Factory buildHttpDataSourceFactory(boolean useBandwidthMeter) {
        return new DefaultHttpDataSourceFactory(userAgent, useBandwidthMeter ? BANDWIDTH_METER : null);
    }

}
