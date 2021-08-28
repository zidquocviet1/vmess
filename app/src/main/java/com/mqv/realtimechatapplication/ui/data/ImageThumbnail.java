package com.mqv.realtimechatapplication.ui.data;

import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

import java.time.LocalDateTime;

public class ImageThumbnail {
    private final Long id;
    private final String displayName;
    private final Long size;
    private final LocalDateTime timestamp;
    private final Uri contentUri;
    private final Bitmap thumbnail;

    public ImageThumbnail(Long id, String displayName, Long size, LocalDateTime timestamp, Uri contentUri, Bitmap thumbnail) {
        this.id = id;
        this.displayName = displayName;
        this.size = size;
        this.timestamp = timestamp;
        this.contentUri = contentUri;
        this.thumbnail = thumbnail;
    }

    public Long getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Long getSize() {
        return size;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public Uri getContentUri() {
        return contentUri;
    }

    public Bitmap getThumbnail() {
        return thumbnail;
    }
}
