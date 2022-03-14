package com.mqv.vmess.activity.br;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.FileObserver;

import com.mqv.vmess.activity.service.FileObserverService;
import com.mqv.vmess.util.Logging;

public class FileObserverBroadcastReceiver extends BroadcastReceiver {
    private onImagesChangeListener callback;
    private static final int DEFAULT_UNKNOWN_EVENT = -1;
    public static final String ACTION_FILE_OBSERVER = "com.mqv.tac.action.FILE_CHANGE";

    public FileObserverBroadcastReceiver(){}

    public FileObserverBroadcastReceiver(onImagesChangeListener callback){
        this.callback = callback;
    }

    public interface onImagesChangeListener{
        void onImageCreated(String path);

        void onImageDeleted(String path);

        void onImageMoved();
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        var eventExtra = intent.getIntExtra(FileObserverService.EXTRA_EVENT, DEFAULT_UNKNOWN_EVENT);
        var filePath = intent.getStringExtra(FileObserverService.EXTRA_REAL_PATH);

        switch (eventExtra){
            case FileObserver.OPEN:
                break;
            case FileObserver.CREATE:
                if (callback != null)
                    callback.onImageCreated(filePath);
                break;
            case FileObserver.DELETE:
            case FileObserver.DELETE_SELF:
                if (callback != null)
                    callback.onImageDeleted(filePath);
                break;
            case FileObserver.MOVE_SELF:
            case FileObserver.MOVED_FROM:
            case FileObserver.MOVED_TO:
                Logging.show("FileObserverService move file " + filePath);
                if (callback != null)
                    callback.onImageMoved();
                break;
        }
    }
}
