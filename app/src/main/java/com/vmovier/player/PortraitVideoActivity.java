package com.vmovier.player;

import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.WindowManager;

import com.magicbox.vmovierplayer.R;
import com.vmovier.lib.player.VideoViewDataSource;
import com.vmovier.lib.view.VMovieVideoView;

import butterknife.BindView;
import butterknife.ButterKnife;


public class PortraitVideoActivity extends AppCompatActivity {
    // TODO 改成你自己的竖屏视频的播放地址
    private static final String YOUR_PORTRAIT_VIDEO_URL = "http://vjs.zencdn.net/v/oceans.mp4";
    @BindView(R.id.VideoView)
    VMovieVideoView movieVideoView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_portrait_video);
        /*set it to be full screen*/
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        ButterKnife.bind(this);
        movieVideoView.setMediaDataSource(new VideoViewDataSource(Uri.parse(YOUR_PORTRAIT_VIDEO_URL)));

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        movieVideoView.stopPlayback();
    }
}
