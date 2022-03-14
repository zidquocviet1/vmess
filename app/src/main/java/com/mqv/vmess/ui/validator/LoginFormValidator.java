package com.mqv.vmess.ui.validator;


import static com.mqv.vmess.ui.validator.LoginRegisterValidationResult.*;

import java.util.function.Function;

public interface LoginFormValidator extends Function<LoginForm, LoginRegisterValidationResult> {
    static LoginFormValidator isUsernameValid() {
        return form -> RegisterFormValidator.regexEmail(form.getUsername()) ? SUCCESS : EMAIL_ERROR;
    }

    static LoginFormValidator isPasswordValid() {
        return form -> RegisterFormValidator.regexPassword(form.getPassword()) ? SUCCESS : PASSWORD_ERROR;
    }

    default LoginFormValidator and(LoginFormValidator other) {
        return form -> {
            var result = this.apply(form);

            return result == SUCCESS ? other.apply(form) : result;
        };
    }
}
