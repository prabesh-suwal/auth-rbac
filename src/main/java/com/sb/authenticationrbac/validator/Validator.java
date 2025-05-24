package com.sb.authenticationrbac.validator;

import com.sb.authenticationrbac.exception.ValidationException;

public interface Validator<T> {
    void validate(T request) throws ValidationException;
} 