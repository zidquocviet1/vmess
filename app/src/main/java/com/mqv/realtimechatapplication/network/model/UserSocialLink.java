package com.mqv.realtimechatapplication.network.model;

import com.google.gson.annotations.SerializedName;

public class UserSocialLink {
    private Long id;
    private SocialType type;
    @SerializedName("account_name")
    private String accountName;

    public UserSocialLink(Long id, SocialType type, String accountName) {
        this.id = id;
        this.type = type;
        this.accountName = accountName;
    }

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
}
