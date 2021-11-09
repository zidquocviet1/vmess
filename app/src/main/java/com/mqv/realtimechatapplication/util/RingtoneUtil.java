package com.mqv.realtimechatapplication.util;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.MediaPlayer;

import java.io.IOException;

public final class RingtoneUtil {
    public static void open(Context context, String file) {
        // Play ringtone when receive new message
        try {
            AssetFileDescriptor afd = context.getAssets().openFd(file);

            MediaPlayer mp = new MediaPlayer();

            mp.setDataSource(afd);
            mp.prepare();
            mp.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
