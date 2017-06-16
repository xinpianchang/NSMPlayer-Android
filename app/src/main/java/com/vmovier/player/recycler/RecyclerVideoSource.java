package com.vmovier.player.recycler;

import android.net.Uri;

import com.vmovier.lib.player.VideoViewDataSource;

import java.util.ArrayList;

/**
 * Created by bb on 2017/1/6.
 */

public class RecyclerVideoSource {
    public String posterView;
    public VideoViewDataSource videoViewDataSource;


    public static ArrayList<RecyclerVideoSource> createSource() {
        ArrayList<RecyclerVideoSource> a = new ArrayList<>();
        RecyclerVideoSource r1 = new RecyclerVideoSource();
        r1.videoViewDataSource = new VideoViewDataSource(Uri.parse("http://vjs.zencdn.net/v/oceans.mp4"));
        a.add(r1);

        RecyclerVideoSource r2 = new RecyclerVideoSource();
        r2.videoViewDataSource = new VideoViewDataSource(Uri.parse("http://vjs.zencdn.net/v/oceans.mp4"));
        a.add(r2);

        RecyclerVideoSource r3 = new RecyclerVideoSource();
        r3.videoViewDataSource = new VideoViewDataSource(Uri.parse("http://vjs.zencdn.net/v/oceans.mp4"));
        a.add(r3);

        RecyclerVideoSource r4 = new RecyclerVideoSource();
        r4.videoViewDataSource = new VideoViewDataSource(Uri.parse("http://vjs.zencdn.net/v/oceans.mp4"));
        a.add(r4);

        RecyclerVideoSource r5 = new RecyclerVideoSource();
        r5.videoViewDataSource = new VideoViewDataSource(Uri.parse("http://vjs.zencdn.net/v/oceans.mp4"));
        a.add(r5);

        RecyclerVideoSource r6 = new RecyclerVideoSource();
        r6.videoViewDataSource = new VideoViewDataSource(Uri.parse("http://vjs.zencdn.net/v/oceans.mp4"));
        a.add(r6);

        RecyclerVideoSource r7 = new RecyclerVideoSource();
        r7.videoViewDataSource = new VideoViewDataSource(Uri.parse("http://vjs.zencdn.net/v/oceans.mp4"));
        a.add(r7);


        return a;
    }
}
