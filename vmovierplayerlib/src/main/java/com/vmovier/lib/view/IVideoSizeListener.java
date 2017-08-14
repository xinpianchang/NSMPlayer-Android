package com.vmovier.lib.view;

import com.vmovier.lib.player.VideoSize;

/**
 * Player VideoSize Listener.
 */
public interface IVideoSizeListener {
    /**
     * VideoSize发生改变之后,回调该方法
     * @param videoSize 视频Size
     */
    void onVideoSizeChanged(VideoSize videoSize);
}
