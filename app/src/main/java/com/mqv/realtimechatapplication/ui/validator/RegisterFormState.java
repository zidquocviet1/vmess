package com.mqv.realtimechatapplication.ui.validator;

public class RegisterFormState {
    private Integer usernameError;
    private Integer displayNameError;
    private Integer passwordError;
    private Integer rePasswordError;
    private boolean isDataValid;

    public RegisterFormState(){}

    public RegisterFormState(Integer usernameError, Integer displayNameError, Integer passwordError, Integer rePasswordError, boolean isDataValid) {
        this.usernameError = usernameError;
        this.displayNameError = displayNameError;
        this.passwordError = passwordError;
        this.rePasswordError = rePasswordError;
        this.isDataValid = isDataValid;
    }

    public RegisterFormState(boolean isDataValid){
        this.isDataValid = isDataValid;
    }

    public Integer getUsernameError() {
        return usernameError;
    }

    public void setUsernameError(Integer usernameError) {
        this.usernameError = usernameError;
    }

    public Integer getDisplayNameError() {
        return displayNameError;
    }

    public void setDisplayNameError(Integer displayNameError) {
        this.displayNameError = displayNameError;
    }

    public Integer getPasswordError() {
        return passwordError;
    }

    public void setPasswordError(Integer passwordError) {
        this.passwordError = passwordError;
    }

    public Integer getRePasswordError() {
        return rePasswordError;
    }

    public void setRePasswordError(Integer rePasswordError) {
        this.rePasswordError = rePasswordError;
    }

    public boolean isDataValid() {
        return isDataValid;
    }

    public void setDataValid(boolean dataValid) {
        isDataValid = dataValid;
    }
}
