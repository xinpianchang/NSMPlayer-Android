package com.vmovier.player;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import com.magicbox.vmovierplayer.R;
import com.vmovier.player.recycler.BasicViewRecyclerActivity;
import com.vmovier.player.recycler.VMovieVideoViewRecyclerActivity;

import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Created by bb on 16/11/23.
 */

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
    }

    @OnClick (R.id.BasicVideoView)
    void basicVideoView() {
        startActivity(new Intent(MainActivity.this, BasicVideoViewActivity.class));
    }

    @OnClick (R.id.VMovieVideoView)
    void VMovieVideoView() {
        startActivity(new Intent(MainActivity.this, VMovieVideoViewActivity.class));
    }

    @OnClick(R.id.VideoViewRecyclerView)
    void VideoViewRecyclerView() {
        startActivity(new Intent(MainActivity.this, VMovieVideoViewRecyclerActivity.class));
    }

    @OnClick(R.id.BasicVideoViewRecyclerView)
    void BasicViewRecyclerView() {
        startActivity(new Intent(MainActivity.this, BasicViewRecyclerActivity.class));
    }

    @OnClick(R.id.Portrait)
    void Portrait() {
        startActivity(new Intent(MainActivity.this, PortraitVideoActivity.class));
    }

    @OnClick(R.id.LocalVideo)
    void LocalVideo() {
        startActivity(new Intent(MainActivity.this, LocalVideoActivity.class));
        // TODO 申请权限.
//        ui.requestPermission(Manifest.permission.READ_EXTERNAL_STORAGE).done(new DoneResolver<PermissionModule.Result>() {
//            @Override
//            public void callback(Exception exception, PermissionModule.Result result) {
//                if (exception == null && result.granted) {
//                }
//            }
//        });
    }

    @OnClick(R.id.Vmovier)
    void Vmovier() {
        startActivity(new Intent(MainActivity.this, VMovierActivity.class));
    }

    @OnClick(R.id.Essay)
    void Essay() {
        startActivity(new Intent(MainActivity.this, EssayDetailActivity.class));
    }
}
