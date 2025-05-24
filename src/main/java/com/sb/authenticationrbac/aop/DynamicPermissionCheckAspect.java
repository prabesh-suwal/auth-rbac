package com.sb.authenticationrbac.aop;

import com.sb.authenticationrbac.dtos.PermissionResult;
import com.sb.authenticationrbac.exceptions.PermissionDeniedException;
import com.sb.authenticationrbac.repositories.UserRepository;
import com.sb.authenticationrbac.services.DynamicPermissionEvaluationService;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import com.sb.authenticationrbac.security.CustomUserDetails;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.Map;

@Aspect
@Component
@Order(1)
public class DynamicPermissionCheckAspect {

    private final UserRepository userRepository;

    private final DynamicPermissionEvaluationService permissionService;
    private final ResourceLoaderService resourceLoaderService;
    private final HttpServletRequest request;

    public DynamicPermissionCheckAspect(UserRepository userRepository, DynamicPermissionEvaluationService permissionService,
                                        ResourceLoaderService resourceLoaderService,
                                        HttpServletRequest request) {
        this.userRepository = userRepository;
        this.permissionService = permissionService;
        this.resourceLoaderService = resourceLoaderService;
        this.request = request;
    }

    @Around("@annotation(checkPermission)")
    public Object checkSinglePermission(ProceedingJoinPoint joinPoint, CheckPermission checkPermission) throws Throwable {
        PermissionResult result = evaluatePermission(joinPoint, checkPermission);
        
        if (result.isAllowed()) {
            return joinPoint.proceed();
        }
        
        throw new PermissionDeniedException(
            checkPermission.message().isEmpty() ? result.getReason() : checkPermission.message()
        );
    }

    @Around("@annotation(checkPermissions)")
    public Object checkMultiplePermissions(ProceedingJoinPoint joinPoint, CheckPermissions checkPermissions) throws Throwable {
        String logic = checkPermissions.logic();
        boolean result;
        
        if ("OR".equalsIgnoreCase(logic)) {
            result = false;
            for (CheckPermission permission : checkPermissions.value()) {
                PermissionResult permResult = evaluatePermission(joinPoint, permission);
                if (permResult.isAllowed()) {
                    result = true;
                    break;
                }
            }
        } else { // Default to AND logic
            result = true;
            for (CheckPermission permission : checkPermissions.value()) {
                PermissionResult permResult = evaluatePermission(joinPoint, permission);
                if (!permResult.isAllowed()) {
                    result = false;
                    throw new PermissionDeniedException(
                        checkPermissions.message().isEmpty() ? permResult.getReason() : checkPermissions.message()
                    );
                }
            }
        }
        
        if (result) {
            return joinPoint.proceed();
        }
        
        throw new PermissionDeniedException(checkPermissions.message());
    }

    private PermissionResult evaluatePermission(ProceedingJoinPoint joinPoint, CheckPermission checkPermission) {
        try {
            // Get current user
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated() || auth.getPrincipal() instanceof String && auth.getPrincipal().equals("anonymousUser")) {
                return PermissionResult.denied("User not authenticated or anonymous");
            }

            CustomUserDetails userDetails = (CustomUserDetails) auth.getPrincipal();
            String userId = userDetails.getUserId();
            // String userId = "682f57330b508dac57282092"; // Removed hardcoded userId

            // Get method parameters
            Map<String, Object> methodParams = getMethodParameters(joinPoint);
            
            // Load resource if specified
            Object resource = loadResource(checkPermission, methodParams);
            
            // Create context with method parameters and request info
            Map<String, Object> context = createEvaluationContext(joinPoint, checkPermission, methodParams);

            // Evaluate permission using the dynamic service
            return permissionService.hasPermission(
                userId,
                checkPermission.value(), 
                checkPermission.operation(), 
                resource, 
                context
            );

        } catch (Exception e) {
            // Log the exception for debugging
            return PermissionResult.denied("Permission evaluation failed: " + e.getMessage());
        }
    }

    private Map<String, Object> getMethodParameters(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Parameter[] parameters = method.getParameters();
        Object[] args = joinPoint.getArgs();
        
        Map<String, Object> paramMap = new HashMap<>();
        
        for (int i = 0; i < parameters.length && i < args.length; i++) {
            paramMap.put(parameters[i].getName(), args[i]);
        }
        
        return paramMap;
    }

    private Object loadResource(CheckPermission checkPermission, Map<String, Object> methodParams) {
        // First try to get resource directly from parameter
        if (!checkPermission.resourceParam().isEmpty()) {
            return methodParams.get(checkPermission.resourceParam());
        }

        // Then try to load resource using ID and type
        if (!checkPermission.resource().isEmpty() && !checkPermission.resourceIdParam().isEmpty()) {
            Object resourceId = methodParams.get(checkPermission.resourceIdParam());
            if (resourceId != null) {
                return resourceLoaderService.loadResource(checkPermission.resource(), resourceId);
            }
        }

        return null;
    }

    private Map<String, Object> createEvaluationContext(ProceedingJoinPoint joinPoint, 
                                                       CheckPermission checkPermission, 
                                                       Map<String, Object> methodParams) {
        Map<String, Object> context = new HashMap<>();
        
        // Add all method parameters
        context.putAll(methodParams);
        
        // Add specific context parameters if specified
        for (String contextParam : checkPermission.contextParams()) {
            if (methodParams.containsKey(contextParam)) {
                context.put("ctx_" + contextParam, methodParams.get(contextParam));
            }
        }
        
        // Add request information
        context.put("requestMethod", request.getMethod());
        context.put("requestPath", request.getRequestURI());
        context.put("userAgent", request.getHeader("User-Agent"));
        context.put("remoteAddr", request.getRemoteAddr());
        
        // Add method information
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        context.put("methodName", signature.getMethod().getName());
        context.put("className", signature.getDeclaringTypeName());
        
        return context;
    }
}