package com.vmovier.lib.player;


public class VideoSize {
    public int videoWidth;
    public int videoHeight;
    public int videoSarNum;
    public int videoSarDen;

    @Override
    public String toString() {
        return "VideoSize{" +
                "videoWidth=" + videoWidth +
                ", videoHeight=" + videoHeight +
                ", videoSarNum=" + videoSarNum +
                ", videoSarDen=" + videoSarDen +
                '}';
    }
}
