package com.vmovier.player.recycler;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import com.magicbox.vmovierplayer.R;
import com.vmovier.lib.utils.PlayerLog;

import butterknife.BindView;
import butterknife.ButterKnife;

public class VMovieVideoViewRecyclerActivity extends AppCompatActivity {
    private static final String TAG = "VMovieVideoViewRecycler";

    @BindView(R.id.recyclerView)
    RecyclerView mRecyclerView;

    VMovieVideoViewAdapter mAdapter;
    LinearLayoutManager mLinearLayoutManager;
    VMovieVideoViewAdapter.RecyclerViewHolder mLastPlayHolder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recycler);
        ButterKnife.bind(this);

        mAdapter = new VMovieVideoViewAdapter();
        mAdapter.setData(RecyclerVideoSource.createSource());
        mLinearLayoutManager = new LinearLayoutManager(this.getApplicationContext());
        mRecyclerView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL_LIST));
        mRecyclerView.setLayoutManager(mLinearLayoutManager);
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.addOnScrollListener(mScrollListener);
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
            mLastPlayHolder.vMovieVideoView.pause();
            mLastPlayHolder = null;
        }
        final int startChildPosition = mLinearLayoutManager.findFirstVisibleItemPosition();
        PlayerLog.d(TAG, "Play startChildPosition : " + startChildPosition);
        final int lastChildPosition = startChildPosition + mLinearLayoutManager.getChildCount();
        PlayerLog.d(TAG, "Play lastChildPosition : " + lastChildPosition);

        for (int i = startChildPosition ; i < lastChildPosition; i ++) {
            VMovieVideoViewAdapter.RecyclerViewHolder holder =  (VMovieVideoViewAdapter.RecyclerViewHolder) mRecyclerView.findViewHolderForAdapterPosition(i);
            if (holder == null) {
                PlayerLog.e(TAG, "error");
                continue;
            }
            PlayerLog.d(TAG, "holder is " + holder.toString());
            holder.vMovieVideoView.stopPlayback();
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
        if (mLastPlayHolder != null) {
            mLastPlayHolder.vMovieVideoView.pause();
        }
        mLastPlayHolder =  (VMovieVideoViewAdapter.RecyclerViewHolder) mRecyclerView.findViewHolderForAdapterPosition(position);
        mLastPlayHolder.vMovieVideoView.play();
    }
}
