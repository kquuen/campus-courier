package com.campus.courier.security;

import com.campus.courier.config.UserContext;
import com.campus.courier.dto.Result;
import com.campus.courier.entity.UserRole;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

@Aspect
@Component
public class RequireRoleAspect {

    @Around("execution(* com.campus.courier.controller..*(..))")
    public Object enforceRole(ProceedingJoinPoint pjp) throws Throwable {
        RequireRole ann = resolve(pjp);
        if (ann == null || ann.value().length == 0) {
            return pjp.proceed();
        }
        UserRole current = UserContext.getRole();
        if (current == null) {
            return Result.unauthorized();
        }
        for (UserRole allowed : ann.value()) {
            if (current == allowed) {
                return pjp.proceed();
            }
        }
        return Result.forbidden();
    }

    private static RequireRole resolve(ProceedingJoinPoint pjp) {
        MethodSignature sig = (MethodSignature) pjp.getSignature();
        Method method = sig.getMethod();
        RequireRole onMethod = AnnotatedElementUtils.findMergedAnnotation(method, RequireRole.class);
        if (onMethod != null) {
            return onMethod;
        }
        return AnnotatedElementUtils.findMergedAnnotation(pjp.getTarget().getClass(), RequireRole.class);
    }
}
