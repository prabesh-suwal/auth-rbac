package com.sb.authenticationrbac.validator;

import jakarta.annotation.PostConstruct;
import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;
import java.util.stream.Collectors;
//
//@Component
//public class ValidatorDispatcher {
//
//    private final Map<Class<?>, Validator<?>> validatorMap = new HashMap<>();
//
//    @Autowired
//    public ValidatorDispatcher(List<Validator<?>> validators) {
//        Map<Class<?>, List<Validator<?>>> grouped = new HashMap<>();
//
//        for (Validator<?> validator : validators) {
//            Class<?> requestType = resolveGenericType(validator);
//
//            grouped.computeIfAbsent(requestType, k -> new ArrayList<>())
//                    .add(validator);
//        }
//
//        for (Map.Entry<Class<?>, List<Validator<?>>> entry : grouped.entrySet()) {
//            Class<?> type = entry.getKey();
//            List<Validator<?>> sameTypeValidators = entry.getValue();
//
//            if (sameTypeValidators.size() > 1) {
//                throw new IllegalStateException(
//                        "Multiple validators found for " + type.getSimpleName() +
//                                ": " + sameTypeValidators.stream()
//                                .map(Object::getClass)
//                                .map(Class::getName)
//                                .collect(Collectors.joining(", "))
//                );
//            }
//
//            validatorMap.put(type, sameTypeValidators.get(0));
//        }
//    }
//
//    @SuppressWarnings("unchecked")
//    public <T> void validate(T request) {
//        Validator<T> validator = (Validator<T>) validatorMap.get(request.getClass());
//        if (validator == null) {
//            throw new IllegalStateException("No validator found for type: " + request.getClass().getSimpleName());
//        }
//        validator.validate(request);
//    }
//
//    private Class<?> resolveGenericType(Validator<?> validator) {
//        // This resolves Validator<T>'s T type
//        return Arrays.stream(validator.getClass().getGenericInterfaces())
//                .filter(type -> type instanceof ParameterizedType)
//                .map(type -> (ParameterizedType) type)
//                .filter(pt -> pt.getRawType() == Validator.class)
//                .findFirst()
//                .map(pt -> (Class<?>) pt.getActualTypeArguments()[0])
//                .orElseThrow(() -> new IllegalStateException("Cannot resolve generic type for " + validator.getClass()));
//    }
//}
@Component
public class ValidatorDispatcher {

    private final Map<Class<?>, Validator<?>> validatorMap = new HashMap<>();

    public ValidatorDispatcher(List<Validator<?>> validators) {
        for (Validator<?> validator : validators) {
            Class<?> type = resolveGenericType(validator);
            if (type != null) {
                validatorMap.put(type, validator);
            }
        }
    }

    @SuppressWarnings("unchecked")
    public <T> void validate(T request) {
        Validator<T> validator = (Validator<T>) validatorMap.get(request.getClass());
        if (validator == null) {
            throw new IllegalArgumentException("No validator found for: " + request.getClass());
        }
        validator.validate(request);
    }
    private Class<?> resolveGenericType(Validator<?> validator) {
        Class<?> actualClass = AopProxyUtils.ultimateTargetClass(validator);

        while (actualClass != null && actualClass != Object.class) {
            // Check interfaces
            for (Type iface : actualClass.getGenericInterfaces()) {
                if (iface instanceof ParameterizedType paramType &&
                        ((Class<?>) paramType.getRawType()).equals(Validator.class)) {
                    Type actualType = paramType.getActualTypeArguments()[0];
                    if (actualType instanceof Class<?> clazz) {
                        return clazz;
                    }
                }
            }

            // Check superclass
            Type superClass = actualClass.getGenericSuperclass();
            if (superClass instanceof ParameterizedType paramSuperType) {
                Class<?> rawSuper = (Class<?>) paramSuperType.getRawType();
                if (Validator.class.isAssignableFrom(rawSuper)) {
                    Type actualType = paramSuperType.getActualTypeArguments()[0];
                    if (actualType instanceof Class<?> clazz) {
                        return clazz;
                    }
                }
                actualClass = rawSuper;
            } else if (superClass instanceof Class<?> clazz) {
                actualClass = clazz;
            } else {
                break;
            }
        }

        return null;
    }
}
