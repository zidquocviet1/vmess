package com.mqv.realtimechatapplication.util;

import org.apache.commons.codec.binary.Base64;

import java.nio.charset.StandardCharsets;

public final class Const {
    private static final int PORT = 443; // 443 for Https and 8080 or 80 for Http
    public static final String BASE_IP = "192.168.100.15";
    public static final String BASE_URL = "https://" + BASE_IP + ":" + PORT + "/api/v1/";
    public static final Long NETWORK_TIME_OUT = 10000L;
    public static final String CONTENT_TYPE = "application/json";
    public static final String PHONE_REGEX_PATTERN = "(84|0[3|5|7|8|9])+([0-9]{8})\\b";
    public static final String PASSWORD_REGEX_PATTERN = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=])(?=\\S+$).{8,}$";
    public static final String AUTHORIZATION = "Authorization";
    public static final String AUTHORIZER = "Authorizer";
    public static final String MULTIPART_TYPE = "multipart/form-data";
    public static final String PREFIX_TOKEN = "Bearer ";
    public static final String IMAGE_FILE_NAME_PATTERN = "yyyyMMdd_HHmmss"; // example: 09/02/2021 13:01:30 -> 20210902_130130

    // DATABASE
    public static final String DATABASE_NAME = "tac_database";
    // TABLE USER
    public static final String TABLE_USER = "user";
    public static final String KEY_USER_NAME = "username";
    public static final String KEY_PASSWORD = "password";
    public static final String KEY_DISPLAY_NAME = "display_name";
    public static final String KEY_AVATAR = "avatar";

    // OTP Activity
    public static final String EXTRA_VERIFICATION_ID = "verification_id";
    public static final String EXTRA_RESEND_TOKEN = "resend_token";
    public static final String EXTRA_RESEND_PHONE_NUMBER = "resend_phone_number";
    public static final Long PHONE_AUTH_TIME_OUT = 60L;

    // Main Activity
    public static final String EXTRA_USER_INFO = "user_info";


    private static final String DUMMIES_IMAGES_DOMAIN = BASE_URL + "user/photo/";
    public static final String[] DUMMIES_IMAGES_URL = new String[]{
            DUMMIES_IMAGES_DOMAIN + encode("beyonce-super-bowl-messika-vignette.jpg"),
            DUMMIES_IMAGES_DOMAIN + encode("Cristiano_Ronaldo_2018.jpg"),
            DUMMIES_IMAGES_DOMAIN + encode("cuc-tinh-y-2.jpg"),
            DUMMIES_IMAGES_DOMAIN + encode("david-beckham-footballer-smile-face-wallpaper-preview.jpg"),
            DUMMIES_IMAGES_DOMAIN + encode("dich-le-nhiet-ba-7.jpg"),
            DUMMIES_IMAGES_DOMAIN + encode("dien-vien-trung-quoc-cuc-tinh-y.jpg"),
            DUMMIES_IMAGES_DOMAIN + encode("GettyImages_1152687796.jpg"),
            DUMMIES_IMAGES_DOMAIN + encode("hieuminh250-1345801305.jpg"),
            DUMMIES_IMAGES_DOMAIN + encode("thuonghai10-1345801306.jpg"),
            DUMMIES_IMAGES_DOMAIN + encode("toni-kroos-internat-volkswagen-183634082.jpg"),
            DUMMIES_IMAGES_DOMAIN + encode("zvY94dFUiqKwu1PMVsw5_thatnordicguyredo.jpg"),

    };

    private static String encode(String s){
        var byteArray = Base64.encodeBase64(("dummies/" + s).getBytes(StandardCharsets.UTF_8));
        return new String(byteArray);
    }
}
