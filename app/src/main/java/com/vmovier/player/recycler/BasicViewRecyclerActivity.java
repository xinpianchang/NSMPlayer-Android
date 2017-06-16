package com.vmovier.player.recycler;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import com.magicbox.vmovierplayer.R;
import com.vmovier.lib.player.IPlayer;
import com.vmovier.lib.player.IPlayerFactory;
import com.vmovier.lib.utils.PlayerLog;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;

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

}
