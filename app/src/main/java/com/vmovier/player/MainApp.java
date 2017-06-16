package com.vmovier.player;

import android.app.Application;

import com.vmovier.lib.Player;

public class MainApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        Player.init(this);
   }
}
