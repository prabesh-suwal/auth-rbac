package com.sb.authenticationrbac.aop;

import com.sb.authenticationrbac.dtos.PermissionResult;
import com.sb.authenticationrbac.exceptions.PermissionDeniedException;
import com.sb.authenticationrbac.repositories.UserRepository;
import com.sb.authenticationrbac.security.SecurityContextUtils;
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

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Aspect
@Component
@Order(1)
public class DynamicPermissionCheckAspect {

    private final UserRepository userRepository;
    private final DynamicPermissionEvaluationService permissionService;
    private final ResourceLoaderService resourceLoaderService;
    private final HttpServletRequest request;

    public DynamicPermissionCheckAspect(UserRepository userRepository,
                                        DynamicPermissionEvaluationService permissionService,
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
        
        // Get method parameters and resource for detailed exception
        Map<String, Object> methodParams = getMethodParameters(joinPoint);
        Object resource = loadResource(checkPermission, methodParams);
        Map<String, Object> context = createEvaluationContext(joinPoint, checkPermission, methodParams);
        
        String message = result.getReason().isEmpty() ? checkPermission.message() : result.getReason();
        String resourceString = resource != null ? resource.toString() : null;
        
        throw new PermissionDeniedException(
            message,
            checkPermission.value(),
            resourceString,
            checkPermission.operation(),
            context
        );
    }

    @Around("@annotation(checkPermissions)")
    public Object checkMultiplePermissions(ProceedingJoinPoint joinPoint, CheckPermissions checkPermissions) throws Throwable {
        String logic = checkPermissions.logic();
        boolean result;
        
        // Get method parameters and context for detailed exception (shared for all permissions)
        Map<String, Object> methodParams = getMethodParameters(joinPoint);
        Map<String, Object> context = createEvaluationContext(joinPoint, checkPermissions.value()[0], methodParams);
        
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
                    Object resource = loadResource(permission, methodParams);
                    String message = checkPermissions.message().isEmpty() ? permResult.getReason() : checkPermissions.message();
                    String resourceString = resource != null ? resource.toString() : null;
                    
                    throw new PermissionDeniedException(
                        message,
                        permission.value(),
                        resourceString,
                        permission.operation(),
                        context
                    );
                }
            }
        }
        
        if (result) {
            return joinPoint.proceed();
        }
        
        // If we reach here, it means OR logic failed for all permissions
        String firstPermissionName = checkPermissions.value().length > 0 ? checkPermissions.value()[0].value() : "UNKNOWN";
        Object firstResource = checkPermissions.value().length > 0 ? loadResource(checkPermissions.value()[0], methodParams) : null;
        String firstOperation = checkPermissions.value().length > 0 ? checkPermissions.value()[0].operation() : "";
        String resourceString = firstResource != null ? firstResource.toString() : null;
        
        throw new PermissionDeniedException(
            checkPermissions.message(),
            firstPermissionName,
            resourceString,
            firstOperation,
            context
        );
    }

    private PermissionResult evaluatePermission(ProceedingJoinPoint joinPoint, CheckPermission checkPermission) {
        try {
            Optional<String> currentUserId = SecurityContextUtils.getCurrentUserId();
            if (currentUserId.isEmpty()) {
                return PermissionResult.denied("No authenticated user found");
            }
            String userId = currentUserId.get();
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
        if (request != null) {
            context.put("requestMethod", request.getMethod());
            context.put("requestPath", request.getRequestURI());
            context.put("userAgent", request.getHeader("User-Agent"));
            context.put("remoteAddr", request.getRemoteAddr());
            
            // Add request parameters
            Map<String, String[]> requestParams = request.getParameterMap();
            if (!requestParams.isEmpty()) {
                context.put("requestParams", requestParams);
            }
        }
        
        // Add method information
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        context.put("methodName", signature.getMethod().getName());
        context.put("className", signature.getDeclaringTypeName());
        
        // Add authentication information
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null) {
            context.put("authenticationName", authentication.getName());
            context.put("authorities", authentication.getAuthorities());
        }
        
        return context;
    }
}