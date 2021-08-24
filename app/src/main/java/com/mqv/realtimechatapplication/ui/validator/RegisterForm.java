package com.mqv.realtimechatapplication.ui.validator;

public class RegisterForm {
    private final String email;
    private final String displayName;
    private final String password;
    private final String rePassword;

    public RegisterForm(String email, String displayName, String password, String rePassword) {
        this.email = email;
        this.displayName = displayName;
        this.password = password;
        this.rePassword = rePassword;
    }

    public String getEmail() {
        return email;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getPassword() {
        return password;
    }

    public String getRePassword() {
        return rePassword;
    }
}
