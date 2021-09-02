package com.mqv.realtimechatapplication.ui.data;

import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

import java.time.LocalDateTime;

public class ImageThumbnail implements Parcelable{
    private Long id;
    private String displayName;
    private Long size;
    private LocalDateTime timestamp;
    private Uri contentUri;
    private Bitmap thumbnail;
    private String mimeType;
    private String relativePath;
    private String realPath;

    public ImageThumbnail(){}

    public ImageThumbnail(Long id, String displayName, Long size, LocalDateTime timestamp,
                          Uri contentUri, Bitmap thumbnail, String mimeType,
                          String relativePath, String realPath) {
        this.id = id;
        this.displayName = displayName;
        this.size = size;
        this.timestamp = timestamp;
        this.contentUri = contentUri;
        this.thumbnail = thumbnail;
        this.mimeType = mimeType;
        this.relativePath = relativePath;
        this.realPath = realPath;
    }

    protected ImageThumbnail(Parcel in) {
        if (in.readByte() == 0) {
            id = null;
        } else {
            id = in.readLong();
        }
        displayName = in.readString();
        if (in.readByte() == 0) {
            size = null;
        } else {
            size = in.readLong();
        }
        contentUri = in.readParcelable(Uri.class.getClassLoader());
//        thumbnail = in.readParcelable(Bitmap.class.getClassLoader());
        timestamp = (LocalDateTime) in.readSerializable();
        mimeType = in.readString();
        relativePath = in.readString();
        realPath = in.readString();
    }

    public static final Creator<ImageThumbnail> CREATOR = new Creator<ImageThumbnail>() {
        @Override
        public ImageThumbnail createFromParcel(Parcel in) {
            return new ImageThumbnail(in);
        }

        @Override
        public ImageThumbnail[] newArray(int size) {
            return new ImageThumbnail[size];
        }
    };

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

    public String getMimeType() {
        return mimeType;
    }

    public String getRelativePath() {
        return relativePath;
    }

    public String getRealPath() {
        return realPath;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        if (id == null) {
            dest.writeByte((byte) 0);
        } else {
            dest.writeByte((byte) 1);
            dest.writeLong(id);
        }
        dest.writeString(displayName);
        if (size == null) {
            dest.writeByte((byte) 0);
        } else {
            dest.writeByte((byte) 1);
            dest.writeLong(size);
        }
        dest.writeParcelable(contentUri, flags);
//        dest.writeParcelable(thumbnail, flags);
        dest.writeSerializable(timestamp);
        dest.writeString(mimeType);
        dest.writeString(relativePath);
        dest.writeString(realPath);
    }
}
