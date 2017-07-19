package com.vmovier.player.recycler;

import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.FrameLayout;

import com.magicbox.vmovierplayer.R;
import com.vmovier.lib.player.IPlayer;
import com.vmovier.lib.player.IPlayerFactory;
import com.vmovier.lib.utils.PlayerLog;
import com.vmovier.lib.view.BasicVideoView;
import com.vmovier.lib.view.render.TextureRenderView;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class BasicViewRecyclerActivity extends AppCompatActivity {
    private static final String TAG = BasicViewRecyclerActivity.class.getSimpleName();
    private BasicVideoViewAdapter mBasicVideoViewAdapter;
    LinearLayoutManager mLinearLayoutManager;
    @BindView(R.id.recyclerView)
    RecyclerView mRecyclerView;

    private ArrayList<RecyclerVideoSource> data = RecyclerVideoSource.createSource();
    private IPlayer mPlayer;
    private BasicVideoViewAdapter.RecyclerViewHolder mLastPlayHolder;
    int mLastPosition = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_basic_view_recycler);
        ButterKnife.bind(this);

        mBasicVideoViewAdapter = new BasicVideoViewAdapter();
        mBasicVideoViewAdapter.setData(data);
        mLinearLayoutManager = new LinearLayoutManager(this.getApplicationContext());
        mRecyclerView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL_LIST));
        mRecyclerView.setLayoutManager(mLinearLayoutManager);
        mRecyclerView.setAdapter(mBasicVideoViewAdapter);
        mRecyclerView.addOnScrollListener(mScrollListener);
        mPlayer = IPlayerFactory.newInstance(this);
        mPlayer.setPlayerType(IPlayer.PLAYERTYPE_EXO);
        mPlayer.setAutoPlay(true);

        /// 拿到最后一个显示的条目
        mRecyclerView.post(new Runnable() {
            @Override
            public void run() {
                int pos = mLinearLayoutManager.findFirstCompletelyVisibleItemPosition();
                playVideo(pos);
            }
        });

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mLastPlayHolder != null) {
            mLastPlayHolder.basicVideoView.setPlayer(null);
        }
        if (mPlayer != null) {
            mPlayer.stopPlayback();
            mPlayer = null;
        }
    }

    private RecyclerView.OnScrollListener mScrollListener = new RecyclerView.OnScrollListener() {
        @Override
        public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
            if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                /// 拿到最后一个显示的条目
                final int pos = mLinearLayoutManager.findFirstCompletelyVisibleItemPosition();
                playVideo(pos);
            }
            PlayerLog.d(TAG, "mRecyclerView first " + mLinearLayoutManager.findFirstVisibleItemPosition());
            PlayerLog.d(TAG, "item is " + mLinearLayoutManager.getChildCount());
        }

        @Override
        public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
            super.onScrolled(recyclerView, dx, dy);
        }
    };

    private void playVideo(int position) {
        if (mLastPosition == position) {
            return;
        }
        mLastPosition = position;

        if (mLastPlayHolder != null) {
            mLastPlayHolder.basicVideoView.setPlayer(null);
        }

        mPlayer.setMediaDataSource(data.get(position).videoViewDataSource);
        mLastPlayHolder =  (BasicVideoViewAdapter.RecyclerViewHolder) mRecyclerView.findViewHolderForAdapterPosition(position);
        mLastPlayHolder.basicVideoView.setPlayer(mPlayer);
    }


    @OnClick(R.id.out)
    void out() {
        if (mLastPlayHolder != null) {
            View v = mLastPlayHolder.parent.getChildAt(0);
            if (v == null || ! (v instanceof BasicVideoView)) return;
            BasicVideoView basicVideoView = (BasicVideoView) v;
            startWindowFullscreen(basicVideoView);
        }
    }

    @OnClick(R.id.in)
    void in() {
        if (mLastPlayHolder != null) {
            View v = mLastPlayHolder.parent.getChildAt(0);
            if (v == null || ! (v instanceof BasicVideoView)) return;
            BasicVideoView basicVideoView = (BasicVideoView) v;
            basicVideoView.setPlayer(mPlayer);
        }
    }

    protected int[] mListItemRect;//当前item框的屏幕位置
    protected int[] mListItemSize;//当前item的大小

    private void startWindowFullscreen(BasicVideoView basicVideoView) {
        mListItemRect = new int[2];
        basicVideoView.getLocationOnScreen(mListItemRect);
        mListItemSize = new int[2];
        mListItemSize[0] = basicVideoView.getWidth();
        mListItemSize[1] = basicVideoView.getHeight();

        // 这里这个ViewGroup 是Window的
        final ViewGroup vp = (ViewGroup)(findViewById(Window.ID_ANDROID_CONTENT));

        // FULLSCREEN_ID 是他自己设定的一个
        removeVideo(vp, R.id.full_screen_id);

        for (int i = 0; i < basicVideoView.getChildCount(); i++) {
            View view = basicVideoView.getChildAt(i);
            basicVideoView.setPlayer(null);
            if (view instanceof TextureRenderView) {
                basicVideoView.removeView(view);
            }
        }

        final BasicVideoView newBasicVideoView = (BasicVideoView) View.inflate(this, R.layout.basic_videoview, null);
        newBasicVideoView.setPlayer(mPlayer);

        final FrameLayout.LayoutParams lpParent = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        final FrameLayout frameLayout = new FrameLayout(this);
        frameLayout.setBackgroundColor(Color.GREEN);

        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(basicVideoView.getWidth(), basicVideoView.getHeight());
        lp.gravity = Gravity.CENTER;
        frameLayout.addView(newBasicVideoView, lp);
        vp.addView(frameLayout, lpParent);
    }

    /**
     * 移除没用的
     */
    private void removeVideo(ViewGroup vp, int id) {
        View old = vp.findViewById(id);
        if (old != null) {
            if (old.getParent() != null) {
                ViewGroup viewGroup = (ViewGroup) old.getParent();
                vp.removeView(viewGroup);
            }
        }
    }

}
