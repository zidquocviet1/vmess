package com.mqv.vmess.activity.service;

import static android.os.Environment.DIRECTORY_DCIM;
import static android.os.Environment.DIRECTORY_DOWNLOADS;
import static android.os.Environment.DIRECTORY_PICTURES;
import static android.os.Environment.DIRECTORY_SCREENSHOTS;
import static android.os.Environment.getExternalStorageDirectory;

import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.FileObserver;
import android.os.IBinder;

import androidx.annotation.Nullable;

import com.mqv.vmess.activity.br.FileObserverBroadcastReceiver;
import com.mqv.vmess.util.Logging;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FileObserverService extends Service {
    public static final String EXTRA_EVENT = "event";
    public static final String EXTRA_REAL_PATH = "real_path";
    private Intent mFileObserverIntent;
    private static final String basePath = getExternalStorageDirectory().getPath();
    private List<File> subDirectories;
    private static final List<String> subDirectoryName = new ArrayList<>();
    static {
        subDirectoryName.add(DIRECTORY_DCIM);
        subDirectoryName.add(DIRECTORY_PICTURES);
        subDirectoryName.add(DIRECTORY_DOWNLOADS);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            subDirectoryName.add(DIRECTORY_SCREENSHOTS);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Logging.show("File Observer Service is created");
        mFileObserverIntent = new Intent(FileObserverBroadcastReceiver.ACTION_FILE_OBSERVER);

        // Get all the subdirectories in the base directory to observe
        subDirectories = getAllSubDirectories();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Logging.show("FileObserverService start");

        FileObserver mFileObserver = new FileObserver(subDirectories) {
            @Override
            public void onEvent(int event, @Nullable String path) {
                if (event == FileObserver.DELETE || event == FileObserver.DELETE_SELF){
                    if (path != null && isImage(path))
                        sendCustomBroadcast(event, basePath + "/" + path);
                }else {
                    var file = new File(basePath + "/" + path);

                    if (isValidImage.accept(file))
                        sendCustomBroadcast(event, file.getPath());
                }
            }
        };
        mFileObserver.startWatching();
        return START_REDELIVER_INTENT;
    }

    private List<File> getAllSubDirectories() {
        Set<File> directories = new HashSet<>();
        try {
            for (var sub : subDirectoryName) {
                Files.walkFileTree(Paths.get(basePath, sub), new SimpleFileVisitor<>() {
                    @Override
                    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                        if (attrs.isDirectory()) {
                            directories.add(dir.toFile());
                        }
                        return FileVisitResult.CONTINUE;
                    }
                });
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new ArrayList<>(directories);
    }

    private void sendCustomBroadcast(int event, String realPath) {
        mFileObserverIntent.putExtra(EXTRA_EVENT, event);
        mFileObserverIntent.putExtra(EXTRA_REAL_PATH, realPath);
        sendBroadcast(mFileObserverIntent);
    }

    private final FileFilter isValidImage = file -> {
        final String name = file.getName();
        String ext = null;
        int i = name.lastIndexOf('.');

        if (i > 0 && i < name.length() - 1) {
            ext = name.substring(i + 1).toLowerCase();
        }

        if (ext == null) return false;
        else return ext.equals("jpg") || ext.equals("jpeg") || ext.equals("png") || ext.equals("gif");
    };

    private boolean isImage(String name){
        String ext = null;
        int i = name.lastIndexOf('.');

        if (i > 0 && i < name.length() - 1) {
            ext = name.substring(i + 1).toLowerCase();
        }

        if (ext == null) return false;
        else return ext.equals("jpg") || ext.equals("jpeg") || ext.equals("png") || ext.equals("gif");
    }
}
