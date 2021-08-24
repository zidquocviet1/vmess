package com.mqv.realtimechatapplication.util;

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


    public static final String[] DUMMIES_IMAGES_URL = new String[]{
            "https://mymodernmet.com/wp/wp-content/uploads/archive/zvY94dFUiqKwu1PMVsw5_thatnordicguyredo.jpg",
            "https://messika.cdn-tech.io/media/ultranoir/messikablog/celebrity/image//b/e/beyonce-super-bowl-messika-vignette.jpg",
            "https://cdn.vox-cdn.com/thumbor/XuYSS8JbN2KoObaYHwf9AfrpysI=/1400x1400/filters:format(jpeg)/cdn.vox-cdn.com/uploads/chorus_asset/file/19229475/GettyImages_1152687796.jpg",
            "https://c4.wallpaperflare.com/wallpaper/207/263/598/david-beckham-footballer-smile-face-wallpaper-preview.jpg",
            "https://upload.wikimedia.org/wikipedia/commons/8/8c/Cristiano_Ronaldo_2018.jpg",
            "https://thumbs.dreamstime.com/z/wolfsburg-germany-march-national-team-footballer-toni-kroos-international-friendly-soccer-game-vs-serbia-volkswagen-183634082.jpg",
            "https://bloganchoi.com/wp-content/uploads/2019/04/cuc-tinh-y-2.jpg",
            "https://tenhaynhat.com/wp-content/uploads/2019/05/dich-le-nhiet-ba-1.jpg",
            "https://www.elleman.vn/wp-content/uploads/2020/04/25/177000/dien-vien-trung-quoc-cuc-tinh-y.jpg",
            "https://lh3.googleusercontent.com/proxy/gAquX0Oq3VY-LZSp8WAKAk_y5RZF90LQ72zm5kstNdhVcZrPVkYy4FEh99W035Lb6Lz6nRg8M5b1UyDYTQZKkYbuNanJ59f10t-4QuKRf-GVZ8u0c1ZN88GXvfuHorbovfrjxEAAxNrqwN9m2__vtE1rnuE",
            "https://ss-images.saostar.vn/wp700/2018/01/23/2100290/dich-le-nhiet-ba-7.jpg"
    };
}
