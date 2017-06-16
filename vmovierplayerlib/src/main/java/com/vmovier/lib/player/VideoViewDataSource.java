package com.vmovier.lib.player;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.HashMap;

public class VideoViewDataSource implements Parcelable {
    public final Uri uri;
    public final HashMap<String, String> headers;

    public VideoViewDataSource(Uri uri) {
        this(uri, null);
    }

    public VideoViewDataSource(Uri uri, HashMap<String, String> headers) {
        this.uri = uri;
        this.headers = headers;
    }

    @Override
    public String toString() {
        return "Uri is " + (uri == null ? "Empty" : uri.toString());
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(this.uri, flags);
        dest.writeSerializable(this.headers);
    }

    protected VideoViewDataSource(Parcel in) {
        this.uri = in.readParcelable(Uri.class.getClassLoader());
        this.headers = (HashMap<String, String>) in.readSerializable();
    }

    public static final Parcelable.Creator<VideoViewDataSource> CREATOR = new Parcelable.Creator<VideoViewDataSource>() {
        @Override
        public VideoViewDataSource createFromParcel(Parcel source) {
            return new VideoViewDataSource(source);
        }

        @Override
        public VideoViewDataSource[] newArray(int size) {
            return new VideoViewDataSource[size];
        }
    };
}
