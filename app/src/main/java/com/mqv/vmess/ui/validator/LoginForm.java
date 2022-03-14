package com.mqv.vmess.ui.validator;

public class LoginForm {
    private final String username;
    private final String password;

    public LoginForm(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }
}
