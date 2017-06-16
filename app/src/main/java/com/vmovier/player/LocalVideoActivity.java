package com.vmovier.player;

import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.magicbox.vmovierplayer.R;
import com.vmovier.lib.player.VideoViewDataSource;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;

public class LocalVideoActivity extends AppCompatActivity {
    private ArrayList<String> data = new ArrayList<>();

    @BindView(R.id.videoRecyclerView)
    RecyclerView mVideoRecyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_local_video);
        ButterKnife.bind(this);

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getApplicationContext());
        mVideoRecyclerView.setLayoutManager(linearLayoutManager);
        VideoUrlAdapter mAdapter = new VideoUrlAdapter();
        mAdapter.setData(data);
        mAdapter.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(int position) {
                Intent intent = new Intent(LocalVideoActivity.this, VMovieVideoViewActivity.class);
                Bundle bundle = new Bundle();
                VideoViewDataSource dataSource  = new VideoViewDataSource(Uri.parse(data.get(position)));
                bundle.putParcelable(VMovieVideoViewActivity.VIDEO_SOURCE, dataSource);
                intent.putExtras(bundle);
                startActivity(intent);
            }
        });
        mVideoRecyclerView.setAdapter(mAdapter);

        ContentResolver contentResolver = getContentResolver();
        Uri uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
        Cursor cursor = contentResolver.query(uri, null, null, null, null);
        if (cursor == null) {
            Toast.makeText(this, "查询失败", Toast.LENGTH_SHORT).show();
        } else {
            int totalCount = cursor.getCount();
            cursor.moveToFirst();
            for (int i = 0 ; i < totalCount; i++) {
                int index = cursor.getColumnIndexOrThrow(MediaStore.Audio.Playlists.DATA);
                String url = cursor.getString(index);
                data.add(url);
                cursor.moveToNext();
            }
            mAdapter.notifyDataSetChanged();
            cursor.close();
        }
    }

    private class VideoUrlAdapter extends RecyclerView.Adapter<VideoUrlAdapter.VideoViewHolder> {
        private ArrayList<String> urlList;
        private OnItemClickListener listener;

        private VideoUrlAdapter() {
        }

        public void setData(ArrayList<String> urlList) {
            this.urlList = urlList;
        }

        @Override
        public VideoUrlAdapter.VideoViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = View.inflate(parent.getContext(), R.layout.item_videourl, null);
            ViewGroup.LayoutParams layoutParams = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, BaseUtil.dip2px(parent.getContext(), 60));
            v.setLayoutParams(layoutParams);
            return new VideoViewHolder(v);
        }

        @Override
        public void onBindViewHolder(VideoUrlAdapter.VideoViewHolder holder, int position) {
            holder.textView.setText(urlList.get(position));
        }

        @Override
        public int getItemCount() {
            return urlList.size();
        }

        class VideoViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
            TextView textView;
            VideoViewHolder(View itemView) {
                super(itemView);
                textView = (TextView) itemView;
                itemView.setOnClickListener(this);
            }

            @Override
            public void onClick(View v) {
                if(listener != null){
                    int position = getAdapterPosition();
                    if (position < data.size() && position >= 0) {
                        listener.onItemClick(position);
                    }
                }
            }
        }

        void setOnItemClickListener(OnItemClickListener onItemClickListener) {
            this.listener = onItemClickListener;
        }
    }

    interface OnItemClickListener {
        void onItemClick(int position);
    }
}
