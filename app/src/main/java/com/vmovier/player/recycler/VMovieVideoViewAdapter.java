package com.vmovier.player.recycler;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;

import com.magicbox.vmovierplayer.R;
import com.vmovier.lib.view.VMovieVideoView;
import com.vmovier.player.BaseUtil;

import java.util.ArrayList;

/**
 * Created by bb on 2017/1/6.
 */

public class VMovieVideoViewAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private ArrayList<RecyclerVideoSource> list;

    public void setData(ArrayList<RecyclerVideoSource> l) {
        this.list = l;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = View.inflate(parent.getContext(), R.layout.item_recyclervideo, null);
        view.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, BaseUtil.dip2px(parent.getContext(), 202.5f)));
        view.setTag(getItemCount());
        return new RecyclerViewHolder(view);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if (RecyclerViewHolder.class.isInstance(holder)) {
            RecyclerViewHolder recyclerViewHolder = (RecyclerViewHolder) holder;
            RecyclerVideoSource s = list.get(position);
            recyclerViewHolder.vMovieVideoView.setMediaDataSource(s.videoViewDataSource);
        }
    }

    @Override
    public int getItemCount() {
        return list == null ? 0 : list.size();
    }

    public class RecyclerViewHolder extends RecyclerView.ViewHolder {
        public VMovieVideoView vMovieVideoView;
        public RecyclerViewHolder(View itemView) {
            super(itemView);
            vMovieVideoView = (VMovieVideoView) itemView.findViewById(R.id.VMovieVideoView);
        }
    }

    @Override
    public void onViewRecycled(RecyclerView.ViewHolder holder) {
        super.onViewRecycled(holder);
    }
}

