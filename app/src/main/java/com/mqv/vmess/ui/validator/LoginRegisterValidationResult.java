package com.mqv.vmess.ui.validator;

import com.mqv.vmess.R;

public enum LoginRegisterValidationResult {
    SUCCESS(-1),
    EMAIL_ERROR(R.string.invalid_username),
    DISPLAY_NAME_ERROR(R.string.invalid_display_name),
    PASSWORD_ERROR(R.string.invalid_password),
    RE_PASSWORD_ERROR(R.string.invalid_re_password);

    private final Integer message;

    LoginRegisterValidationResult(Integer message) {
        this.message = message;
    }

    public Integer getMessage() {
        return message;
    }
}
