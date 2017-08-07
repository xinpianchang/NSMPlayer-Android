package com.vmovier.player;

import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.FrameLayout;

import com.magicbox.vmovierplayer.R;
import com.vmovier.lib.player.IPlayer;
import com.vmovier.lib.player.IPlayerFactory;
import com.vmovier.lib.player.VideoViewDataSource;
import com.vmovier.lib.view.BasicVideoView;
import com.vmovier.lib.view.IVideoStateListener;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;

public class EssayDetailActivity extends AppCompatActivity implements EssayAdapter.OnVideoDetailClickListener, View.OnClickListener {
    private static final String TAG = EssayDetailActivity.class.getSimpleName();
    @BindView(R.id.recyclerView)
    RecyclerView mRecyclerView;
    private ArrayList<EssayBean> data;
    private EssayAdapter mEssayAdapter;
    private LinearLayoutManager mLinearLayoutManager;
    private IPlayer mPlayer;
    private EssayAdapter.VideoViewHolder mLastPlayViewHolder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_essay_detail);
        ButterKnife.bind(this);

        mPlayer = IPlayerFactory.newInstance(this);
        mPlayer.setPlayerType(IPlayer.PLAYERTYPE_EXO);
        mPlayer.setAutoPlay(true);
        mPlayer.addVideoStateListener(new IVideoStateListener() {
            @Override
            public void onStateChanged(int oldState, int newState) {
                Log.d(TAG, "newState :" + newState);
            }

            @Override
            public void onVolumeChanged(int startVolume, int finalVolume) {

            }
        });

        data = makeDatas();
        mEssayAdapter = new EssayAdapter(data);
        mEssayAdapter.setClickListener(this);
        mLinearLayoutManager = new LinearLayoutManager(this.getApplicationContext());
        mRecyclerView.setLayoutManager(mLinearLayoutManager);
        mRecyclerView.setAdapter(mEssayAdapter);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mPlayer.stopPlayback();
        mPlayer = null;
    }

    private static ArrayList<EssayBean> makeDatas() {
        ArrayList<EssayBean> data = new ArrayList<>();
        EssayBean title = new EssayBean();
        title.viewType = EssayAdapter.TITLE;
        title.content = "小情书：路过青春遇到你";
        data.add(title);

        EssayBean content = new EssayBean();
        content.viewType = EssayAdapter.TEXT;
        content.content = "《小情书》是由新片场“超链接”出品的爱情短片系列，以自然流露的方式，清新唯美的色调，讲述关于你、我、他的爱情故事。\n" +
                "\n" +
                "青春有多好，大概只有路过的人才知道。暖暖的阳光从窗户洒进教室，黑板上老师写字粉笔的刷刷声，组成这世界上最和谐的乐音。你熬夜整理课堂笔记给Ta，你上课时候一半的余光给Ta，连Ta不小心撞到你的手，你都觉得这是自己一天心跳的最快的时刻。";
        data.add(content);

        EssayBean image = new EssayBean();
        image.viewType = EssayAdapter.IMAGE;
        data.add(image);

        EssayBean video1 = new EssayBean();
        video1.viewType = EssayAdapter.VIDEO;
        video1.videoUrl = "http://vjs.zencdn.net/v/oceans.mp4";
        data.add(video1);


        EssayBean content2 = new EssayBean();
        content2.viewType = EssayAdapter.TEXT;
        content2.content =  "在这懵懂的年纪里，最重要的好像不是在一起，而是遇见你。遇见你那颗一尘不染的真心，才是我生命中最美好的幸运。过了多年后想起来，曾经喜欢过这样一个人，实在算得上青春里的美事一桩了。";
        data.add(content2);

        EssayBean title2 = new EssayBean();
        title2.viewType = EssayAdapter.TITLE;
        title2.content = "by 范范蒙太奇\n" +
                          "2016.12.25";
        data.add(title2);

        EssayBean video2 = new EssayBean();
        video2.viewType = EssayAdapter.VIDEO;
        video2.videoUrl = "http://vjs.zencdn.net/v/oceans.mp4";
        data.add(video2);
        return data;
    }

    @Override
    public void onVideoItemPlayClick(EssayAdapter.VideoViewHolder videoViewHolder, EssayBean essayBean) {
        mPlayer.pause();
        if (mLastPlayViewHolder != null) {
            mLastPlayViewHolder.basicVideoView.setPlayer(null);
            mLastPlayViewHolder.mCoverLayout.setVisibility(View.VISIBLE);
        }

        mLastPlayViewHolder = videoViewHolder;
        videoViewHolder.mCoverLayout.setVisibility(View.INVISIBLE);
        videoViewHolder.basicVideoView.setPlayer(mPlayer);
        mPlayer.setMediaDataSource(new VideoViewDataSource(Uri.parse(essayBean.videoUrl)));
        mPlayer.play();
    }


    @Override
    public void onVideoItemChangeToFullScreenClick(EssayAdapter.VideoViewHolder videoViewHolder) {
        // 这里这个ViewGroup 是Window的
        final ViewGroup vp = (ViewGroup)(findViewById(Window.ID_ANDROID_CONTENT));

        videoViewHolder.basicVideoView.setPlayer(null);

        final BasicVideoView newBasicVideoView = (BasicVideoView) View.inflate(this, R.layout.basic_videoview, null);
        newBasicVideoView.setPlayer(mPlayer);
        View changeToInsetScreen = newBasicVideoView.findViewById(R.id.changeToInsetScreen);
        changeToInsetScreen.setOnClickListener(this);

        final FrameLayout.LayoutParams lpParent = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        final FrameLayout frameLayout = new FrameLayout(this);
        frameLayout.setId(R.id.full_screen_id);

        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        lp.gravity = Gravity.CENTER;
        frameLayout.addView(newBasicVideoView, lp);
        vp.addView(frameLayout, lpParent);
        mPlayer.seekTo(mPlayer.getCurrentPosition() - 20);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.changeToInsetScreen:
                // 这里这个ViewGroup 是Window的
                final ViewGroup vp = (ViewGroup)(findViewById(Window.ID_ANDROID_CONTENT));
                View view = vp.findViewById(R.id.full_screen_id);
                BasicVideoView b = (BasicVideoView) view.findViewById(R.id.BasicVideoView);
                b.setPlayer(null);

                vp.removeView(view);

                if (mLastPlayViewHolder != null) {
                    mLastPlayViewHolder.basicVideoView.setPlayer(mPlayer);
                }
                mPlayer.seekTo(mPlayer.getCurrentPosition() - 20);
                break;
        }
    }
}
