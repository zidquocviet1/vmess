package com.mqv.realtimechatapplication.network.model;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;
import com.mqv.realtimechatapplication.network.model.type.SocialType;

public class UserSocialLink implements Parcelable {
    private Long id;
    private SocialType type;
    @SerializedName("account_name")
    private String accountName;

    public UserSocialLink(Long id, SocialType type, String accountName) {
        this.id = id;
        this.type = type;
        this.accountName = accountName;
    }

    protected UserSocialLink(Parcel in) {
        if (in.readByte() == 0) {
            id = null;
        } else {
            id = in.readLong();
        }
        accountName = in.readString();
    }

    public static final Creator<UserSocialLink> CREATOR = new Creator<UserSocialLink>() {
        @Override
        public UserSocialLink createFromParcel(Parcel in) {
            return new UserSocialLink(in);
        }

        @Override
        public UserSocialLink[] newArray(int size) {
            return new UserSocialLink[size];
        }
    };

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public SocialType getType() {
        return type;
    }

    public void setType(SocialType type) {
        this.type = type;
    }

    public String getAccountName() {
        return accountName;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (obj == null || obj.getClass() != UserSocialLink.class)
            return false;

        var obj2 = (UserSocialLink) obj;
        return obj2.getId().equals(this.getId()) &&
                obj2.getType().getKey() == this.getType().getKey() &&
                obj2.getAccountName().equals(this.getAccountName());
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
        dest.writeString(accountName);
    }
}
