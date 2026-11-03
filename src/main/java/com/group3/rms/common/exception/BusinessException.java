package com.group3.rms.common.exception;

import org.springframework.validation.BindingResult;

public class BusinessException extends RuntimeException {

    private final String field;

    public BusinessException(String message) {
        this(null, message);
    }

    public BusinessException(String field, String message) {
        super(message);
        this.field = field;
    }

    public String getField() {
        return field;
    }

    public void applyTo(BindingResult result) {
        if (field != null) {
            result.rejectValue(field, "business", getMessage());
        } else {
            result.reject("business", getMessage());
        }
    }
}
