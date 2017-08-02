package com.vmovier.lib.view;

/**
 * Player State's Listener
 */
public interface IVideoStateListener {
    /**
     * state值
     * @param oldState 老状态
     * @param newState 新状态
     */
    void onStateChanged(int oldState, int newState);

    /**
     * 当音量发生改变以后回调
     *
     * @param startVolume 发生改变之前的音量
     * @param finalVolume 改变之后的音量
     */
    void onVolumeChanged(int startVolume, int finalVolume);
}
