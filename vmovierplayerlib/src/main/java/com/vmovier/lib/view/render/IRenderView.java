package com.vmovier.lib.view.render;

import android.graphics.SurfaceTexture;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.View;

import com.vmovier.lib.player.IPlayer;


/**
 * 渲染view的 行为接口.
 */
@SuppressWarnings("unused")
public interface IRenderView {
    /** 可能会剪裁,保持原视频的大小，显示在中心,当原视频的大小超过view的大小超过部分裁剪处理 */
    int SCALE_FIT_PARENT = 0; // without clip
    /** 可能会剪裁,等比例放大视频，直到填满View为止,超过View的部分作裁剪处理 */
    int SCALE_FILL_PARENT = 1; // may clip
    /** 将视频的内容完整居中显示，如果视频大于view,则按比例缩视频直到完全显示在view中 */
    int SCALE_WRAP_CONTENT = 2;
    /** 不剪裁,非等比例拉伸画面填满整个View */
    int SCALE_MATCH_PARENT = 3;
    /** 不剪裁,非等比例拉伸画面到16:9,并完全显示在View中 */
    int SCALE_16_9_FIT_PARENT = 4;
    /** 不剪裁,非等比例拉伸画面到4:3,并完全显示在View中 */
    int SCALE_4_3_FIT_PARENT = 5;

    View getView();

    boolean shouldWaitForResize();

    void setVideoSize(int videoWidth, int videoHeight);

    void setVideoSampleAspectRatio(int videoSarNum, int videoSarDen);

    void setVideoRotation(int degree);

    void setScaleType(int scaleType);

    void addRenderCallback(@NonNull IRenderCallback callback);

    void removeRenderCallback(@NonNull IRenderCallback callback);

    interface ISurfaceHolder {
        void bindToMediaPlayer(@NonNull IPlayer mp);

        @NonNull
        IRenderView getRenderView();

        @Nullable
        SurfaceHolder getSurfaceHolder();

        @Nullable
        Surface openSurface();

        @Nullable
        SurfaceTexture getSurfaceTexture();
    }

    interface IRenderCallback {
        /**
         * @param holder
         * @param width  could be 0
         * @param height could be 0
         */
        void onSurfaceCreated(@NonNull ISurfaceHolder holder, int width, int height);

        /**
         * @param holder
         * @param format could be 0
         * @param width
         * @param height
         */
        void onSurfaceChanged(@NonNull ISurfaceHolder holder, int format, int width, int height);

        void onSurfaceDestroyed(@NonNull ISurfaceHolder holder);
    }
}
