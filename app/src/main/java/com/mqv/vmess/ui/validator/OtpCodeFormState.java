package com.mqv.vmess.ui.validator;

public class OtpCodeFormState {
    private String code1;
    private String code2;
    private String code3;
    private String code4;
    private String code5;
    private String code6;
    private boolean isValid;

    public OtpCodeFormState() {
    }

    public OtpCodeFormState(boolean isValid) {
        this.isValid = isValid;
    }

    public boolean isValid() {
        return isValid;
    }
}
