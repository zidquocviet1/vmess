package com.mqv.realtimechatapplication.data.model;

import java.util.HashMap;
import java.util.Map;

public enum SignInProvider {
    EMAIL,
    FACEBOOK,
    GITHUB,
    GOOGLE,
    PHONE,
    PLAYS_GAME,
    TWITTER;

    private static final Map<String, SignInProvider> sMapSignInProvider = new HashMap<>();
    private String username;

    static {
        sMapSignInProvider.put("password", EMAIL);
        sMapSignInProvider.put("facebook.com", FACEBOOK);
        sMapSignInProvider.put("github.com", GITHUB);
        sMapSignInProvider.put("google.com", GOOGLE);
        sMapSignInProvider.put("phone", PHONE);
        sMapSignInProvider.put("playgames.google.com", PLAYS_GAME);
        sMapSignInProvider.put("twitter.com", TWITTER);
    }

    public static SignInProvider getSignInProvider(String providerId){
        return sMapSignInProvider.get(providerId);
    }

    public void setUsername(String username){
        this.username = username;
    }

    public String getUsername(){
        return this.username;
    }
}
