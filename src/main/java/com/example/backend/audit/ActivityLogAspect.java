package com.example.backend.audit;

import com.example.backend.service.core.SystemActivityLogService;
import com.example.backend.utility.SessionContext;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.stereotype.Component;

@Aspect
@Component
@RequiredArgsConstructor
public class ActivityLogAspect {

    private final SystemActivityLogService activityLogService;

    @Around("@annotation(trackActivity)")
    public Object logActivity(
            ProceedingJoinPoint joinPoint,
            TrackActivity trackActivity
    ) throws Throwable {

        long start = System.currentTimeMillis();

        String actorId = SessionContext.getUserId();
        String actorRole = SessionContext.getRole();

        try {
            Object result = joinPoint.proceed();

            long durationMs = System.currentTimeMillis() - start;

            activityLogService.log(
                    actorId,
                    actorRole,
                    trackActivity.module(),
                    trackActivity.action(),
                    "SUCCESS",
                    "Method completed: " + joinPoint.getSignature().getName(),
                    null,
                    null,
                    null,
                    durationMs
            );

            return result;

        } catch (Throwable ex) {
            long durationMs = System.currentTimeMillis() - start;

            activityLogService.log(
                    actorId,
                    actorRole,
                    trackActivity.module(),
                    trackActivity.action(),
                    "FAILED",
                    "Method failed: " + joinPoint.getSignature().getName() +
                            " - " + ex.getMessage(),
                    null,
                    null,
                    null,
                    durationMs
            );

            throw ex;
        }
    }
}
