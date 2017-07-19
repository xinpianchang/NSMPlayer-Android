package com.vmovier.player;

import android.animation.ObjectAnimator;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.SeekBar;

import com.magicbox.vmovierplayer.R;
import com.vmovier.lib.player.VideoViewDataSource;
import com.vmovier.lib.view.VMovieVideoView;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;


public class VMovierActivity extends AppCompatActivity {

    @BindView(R.id.VolumeSeekBar)
    SeekBar mSeekBar;

    @BindView(R.id.VMovieVideoView)
    VMovieVideoView mVMovieVideoView;

    @BindView(R.id.image)
    ImageView mImageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vmovier);
        ButterKnife.bind(this);
        mSeekBar.setPadding(0, 0, 0, 0);
        mVMovieVideoView.setAutoPlay(true);
        mVMovieVideoView.setPosterUrl("http://cs.vmoiver.com/Uploads/Magic/post/2016-12-05/584513723605e.jpg");
        VideoViewDataSource dataSource = new VideoViewDataSource(Uri.parse("http://125.39.21.11/xdispatch/7ryl2t.com1.z0.glb.clouddn.com/5857c31897c89.mp4"));
        mVMovieVideoView.setMediaDataSource(dataSource);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mVMovieVideoView.stopPlayback();
    }

    @OnClick(R.id.hide)
    void hide() {
        ObjectAnimator animator = null;
        Object tag = mSeekBar.getTag(R.id.progress_animation);
        if (tag != null) {
            animator = (ObjectAnimator) tag;
            animator.cancel();
        }
        animator = ObjectAnimator.ofInt(mSeekBar.getThumb(), "alpha", 0);
        animator.setDuration(200);
        animator.setInterpolator(new DecelerateInterpolator());
        animator.start();
        mSeekBar.setTag(R.id.progress_animation, animator);
    }

    @OnClick(R.id.show)
    void show() {
        ObjectAnimator animator = null;
        Object tag = mSeekBar.getTag(R.id.progress_animation);
        if (tag != null) {
            animator = (ObjectAnimator) tag;
            animator.cancel();
        }
        animator = ObjectAnimator.ofInt(mSeekBar.getThumb(), "alpha", 255);
        animator.setDuration(200);
        animator.setInterpolator(new DecelerateInterpolator());
        animator.start();
        mSeekBar.setTag(R.id.progress_animation, animator);
    }

    @OnClick(R.id.screenShot)
    void screenShot() {
        Bitmap bitmap = mVMovieVideoView.getScreenShot();
        if (bitmap != null) {
            mImageView.setImageBitmap(bitmap);
        }
    }
}
