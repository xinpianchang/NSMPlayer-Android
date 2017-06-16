package com.vmovier.lib.player;

import android.content.Context;
import android.support.annotation.NonNull;

public class IPlayerFactory {
    public static IPlayer newInstance(@NonNull Context context) {
        return new VMoviePlayer(context);
    }
}
