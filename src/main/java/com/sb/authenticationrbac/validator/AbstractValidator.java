package com.sb.authenticationrbac.validator;

import com.sb.authenticationrbac.exception.ValidationException;

public abstract class AbstractValidator<T> implements Validator<T> {
    
    @Override
    public final void validate(T request) throws ValidationException {
        // Template method pattern
        preValidate(request);
        doValidate(request);
        postValidate(request);
    }

    // Hook methods that can be overridden by concrete validators
    protected void preValidate(T request) throws ValidationException {
        // Default empty implementation
    }

    protected abstract void doValidate(T request) throws ValidationException;

    protected void postValidate(T request) throws ValidationException {
        // Default empty implementation
    }
} 