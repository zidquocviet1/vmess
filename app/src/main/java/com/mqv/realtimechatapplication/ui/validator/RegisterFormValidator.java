package com.mqv.realtimechatapplication.ui.validator;

import static com.mqv.realtimechatapplication.ui.validator.LoginRegisterValidationResult.*;

import android.text.TextUtils;
import android.util.Patterns;

import com.mqv.realtimechatapplication.util.Const;

import java.util.function.Function;
import java.util.regex.Pattern;

// Combinator Pattern
public interface RegisterFormValidator extends Function<RegisterForm, LoginRegisterValidationResult> {
    static RegisterFormValidator isDisplayNameValid() {
        return form -> TextUtils.isEmpty(form.getDisplayName()) ? DISPLAY_NAME_ERROR : SUCCESS;
    }

    static RegisterFormValidator isEmailValid() {
        return form -> regexEmail(form.getEmail()) ? SUCCESS : EMAIL_ERROR;
    }

    static RegisterFormValidator isPasswordValid() {
        return form -> regexPassword(form.getPassword()) ? SUCCESS : PASSWORD_ERROR;
    }

    static RegisterFormValidator isRePasswordValid() {
        return form -> form.getRePassword().equals(form.getPassword()) ? SUCCESS : RE_PASSWORD_ERROR;
    }

    default RegisterFormValidator and(RegisterFormValidator other) {
        return form -> {
            var result = this.apply(form);

            return result == SUCCESS ? other.apply(form) : result;
        };
    }

    static boolean regexEmail(String email) {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    static boolean regexPassword(String password) {
        return Pattern.compile(Const.PASSWORD_REGEX_PATTERN).matcher(password).matches();
    }
}
