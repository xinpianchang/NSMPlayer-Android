package com.vmovier.player;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.magicbox.vmovierplayer.R;
import com.vmovier.lib.view.BasicVideoView;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Created by bb on 2017/7/14.
 */

public class EssayAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    public static final int TITLE = 0;
    public static final int TEXT = 1;
    public static final int IMAGE = 2;
    public static final int VIDEO = 3;
    private final ArrayList<EssayBean> datas;


    private OnVideoDetailClickListener mClickListener;

    public void setClickListener(OnVideoDetailClickListener clickListener) {
        this.mClickListener = clickListener;
    }
    public EssayAdapter(ArrayList<EssayBean> datas) {
        this.datas = datas;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        switch (viewType) {
            case TITLE: {
                View view = View.inflate(parent.getContext(), R.layout.item_essay_title, null);
                view.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                return new TitleViewHolder(view);
            }
            case TEXT: {
                View view = View.inflate(parent.getContext(), R.layout.item_essay_text, null);
                view.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                return new TextViewHolder(view);
            }
            case IMAGE: {
                View view = View.inflate(parent.getContext(), R.layout.item_essay_image, null);
                int height = BaseUtil.getScreenWidth(parent.getContext()) / 16 * 9;
                view.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height));
                return new ImageViewHolder(view);
            }
            case VIDEO: {
                View view = View.inflate(parent.getContext(), R.layout.item_essay_video, null);
                int height = BaseUtil.getScreenWidth(parent.getContext()) / 16 * 9;
                view.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height));
                return new VideoViewHolder(view, mClickListener);
            }
        }
        return null;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        EssayBean essayBean = datas.get(position);
        if (TitleViewHolder.class.isInstance(holder)) {
            TitleViewHolder titleViewHolder = (TitleViewHolder) holder;
            titleViewHolder.title.setText(essayBean.content);
        } else if (TextViewHolder.class.isInstance(holder)) {
            TextViewHolder textViewHolder = (TextViewHolder) holder;
            textViewHolder.textView.setText(essayBean.content);
        } else if (ImageViewHolder.class.isInstance(holder)) {
            ImageViewHolder imageViewHolder = (ImageViewHolder) holder;
            Glide.with(imageViewHolder.imageView.getContext()).load(essayBean.imageUrl).into(imageViewHolder.imageView);
        } else if (VideoViewHolder.class.isInstance(holder)) {
            VideoViewHolder videoViewHolder = (VideoViewHolder) holder;
            videoViewHolder.bindVideoViewHolderBean(essayBean);
        }
    }


    @Override
    public int getItemCount() {
        return datas.size();
    }

    @Override
    public int getItemViewType(int position) {
        return datas.get(position).viewType;
    }

    public static class TitleViewHolder extends RecyclerView.ViewHolder {
        public TextView title;

        TitleViewHolder(View itemView) {
            super(itemView);
            title = (TextView) itemView.findViewById(R.id.title);
        }
    }

    public static class TextViewHolder extends RecyclerView.ViewHolder {
        public TextView textView;

        TextViewHolder(View itemView) {
            super(itemView);
            textView = (TextView) itemView.findViewById(R.id.text);
        }
    }


    public static class ImageViewHolder extends RecyclerView.ViewHolder {
        public ImageView imageView;

        ImageViewHolder(View itemView) {
            super(itemView);
            imageView = (ImageView) itemView.findViewById(R.id.image);
        }
    }


    public static class VideoViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.BasicVideoView)
        public BasicVideoView basicVideoView;
        @BindView(R.id.coverLayout)
        public View mCoverLayout;

        OnVideoDetailClickListener listener;
        EssayBean essayBean;

        VideoViewHolder(View itemView, OnVideoDetailClickListener listener) {
            super(itemView);
            ButterKnife.bind(this, itemView);
            this.listener = listener;
        }

        void bindVideoViewHolderBean(EssayBean bean) {
            if (this.essayBean == bean) return;
            essayBean = bean;
        }

        @OnClick(R.id.startPlay)
        void startPlay() {
            if (listener != null) {
                listener.onVideoItemPlayClick(this, essayBean);
            }
        }

        @OnClick(R.id.coverLayout)
        void coverLayout(){}

        @OnClick(R.id.changeToFullScreen)
        void changeToFullScreen() {
            if (listener != null) {
                listener.onVideoItemChangeToFullScreenClick(this);
            }
        }
    }

    public interface OnVideoDetailClickListener {
        void onVideoItemPlayClick(VideoViewHolder videoViewHolder, EssayBean essayBean);
        void onVideoItemChangeToFullScreenClick(VideoViewHolder videoViewHolder);
    }
}
