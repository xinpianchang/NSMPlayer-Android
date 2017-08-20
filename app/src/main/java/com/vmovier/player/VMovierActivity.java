package com.vmovier.player;

import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.widget.ImageView;

import com.magicbox.vmovierplayer.R;
import com.vmovier.lib.player.IPlayer;
import com.vmovier.lib.player.VideoViewDataSource;
import com.vmovier.lib.view.IVideoStateListener;
import com.vmovier.lib.view.VMovieVideoView;
import com.vmovier.lib.view.VMovierTimeBar;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

import static com.magicbox.vmovierplayer.R.id.timeBar;


public class VMovierActivity extends AppCompatActivity {

    @BindView(timeBar)
    VMovierTimeBar mSeekBar;

    @BindView(R.id.VMovieVideoView)
    VMovieVideoView mVMovieVideoView;

    @BindView(R.id.image)
    ImageView mImageView;

    boolean isVideoReady = false;

    Runnable updateProgressRunnable = new Runnable() {
        @Override
        public void run() {
            updateProgress();
        }
    };

    Runnable shotscreenRunnable = new Runnable() {
        @Override
        public void run() {
            screenShot();
        }
    };

    Handler mHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vmovier);
        ButterKnife.bind(this);
        mHandler = new Handler();
        mVMovieVideoView.setAutoPlay(true);
        // http://vjs.zencdn.net/v/oceans.mp4
        VideoViewDataSource dataSource = new VideoViewDataSource(Uri.parse("https://cdn-video.xinpianchang.com/5984533f8c2ef.mp4"));
        mVMovieVideoView.setMediaDataSource(dataSource);
        mVMovieVideoView.addVideoStateListener(new IVideoStateListener() {
            @Override
            public void onStateChanged(int oldState, int newState) {
                updateProgress();
                switch (newState) {
                    case IPlayer.STATE_PLAYING:
                    case IPlayer.STATE_PAUSING:
                        if (!isVideoReady) {
                            isVideoReady = true;
                            mSeekBar.setDuration(mVMovieVideoView.getDuration());
                        }
                        break;
                }
            }

            @Override
            public void onVolumeChanged(int startVolume, int finalVolume) {

            }
        });
        mSeekBar.setListener(new VMovierTimeBar.OnScrubListener() {
            @Override
            public void onScrubStart(VMovierTimeBar timeBar, long position) {

            }

            @Override
            public void onScrubMove(VMovierTimeBar timeBar, long position) {

            }

            @Override
            public void onScrubStop(VMovierTimeBar timeBar, long position, boolean canceled) {
                mVMovieVideoView.seekTo(position);
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mHandler.removeCallbacksAndMessages(null);
        mVMovieVideoView.stopPlayback();
    }

    @OnClick(R.id.hide)
    void hide() {
        mSeekBar.hideThumb();
    }

    @OnClick(R.id.show)
    void show() {
        mSeekBar.showThumb();
    }

    @OnClick(R.id.screenShot)
    void screenShot() {
        Bitmap bitmap = mVMovieVideoView.getScreenShot();
        if (bitmap != null) {
            mImageView.setImageBitmap(bitmap);
        }
        mHandler.postDelayed(shotscreenRunnable, 20);
    }

    private void updateProgress() {
        long duration = mVMovieVideoView == null ? 0 : mVMovieVideoView.getDuration();
        long position = mVMovieVideoView == null ? 0 : mVMovieVideoView.getCurrentPosition();
        int bufferPercentage = mVMovieVideoView == null ? 0 : mVMovieVideoView.getBufferPercentage();
        mSeekBar.setPosition(position);
        mSeekBar.setBufferedPosition(bufferPercentage);
        mSeekBar.setDuration(duration);
        mHandler.removeCallbacks(updateProgressRunnable);
        // Schedule an update if necessary.
        int playState = mVMovieVideoView == null ? IPlayer.STATE_IDLE : mVMovieVideoView.getCurrentPlayerState();
        if (playState == IPlayer.STATE_PLAYING) {
            mHandler.postDelayed(updateProgressRunnable, 1000);
        }
    }
}
