package com.example.backend.audit;

import com.example.backend.entity.core.UserAccess;
import com.example.backend.service.auth.AuthService;
import com.example.backend.service.core.SystemActivityLogService;
import com.example.backend.utility.SessionContext;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.annotation.Annotation;

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

        String actorId = SessionContext.getSchoolId();
        String actorRole = SessionContext.getRole();

        HttpServletRequest request = getCurrentRequest();

        String ipAddress = request == null ? null : getClientIp(request);
        String userAgent = request == null ? null : request.getHeader("User-Agent");

        ActivityTargetValues targetValues = extractTargetValues(joinPoint);

        try {

            Object result = joinPoint.proceed();

            ActivityData contextData = ActivityContext.get();

            if (targetValues.examId == null) {
                targetValues.examId = contextData.getExamId();
            }

            if (targetValues.attemptId == null) {
                targetValues.attemptId = contextData.getAttemptId();
            }

            if (targetValues.questionId == null) {
                targetValues.questionId = contextData.getQuestionId();
            }

            if (targetValues.targetUserId == null) {
                targetValues.targetUserId = contextData.getTargetUserId();
            }

            if (targetValues.metadata == null) {
                targetValues.metadata = contextData.getMetadata();
            }

            long durationMs = System.currentTimeMillis() - start;

            activityLogService.log(
                    actorId,
                    actorRole,
                    targetValues.targetUserId,
                    targetValues.targetRole,
                    trackActivity.module(),
                    trackActivity.action(),
                    "SUCCESS",
                    buildMessage(trackActivity, joinPoint, null),
                    targetValues.examId,
                    targetValues.attemptId,
                    targetValues.questionId,
                    targetValues.classOfferingId,
                    durationMs,
                    ipAddress,
                    userAgent,
                    targetValues.metadata
            );

            return result;

        } catch (Throwable ex) {

            long durationMs = System.currentTimeMillis() - start;

            activityLogService.log(
                    actorId,
                    actorRole,
                    targetValues.targetUserId,
                    targetValues.targetRole,
                    trackActivity.module(),
                    trackActivity.action(),
                    "FAILED",
                    buildMessage(trackActivity, joinPoint, ex),
                    targetValues.examId,
                    targetValues.attemptId,
                    targetValues.questionId,
                    targetValues.classOfferingId,
                    durationMs,
                    ipAddress,
                    userAgent,
                    targetValues.metadata
            );

            throw ex;

        } finally {

            ActivityContext.clear();
        }

    }

    private ActivityTargetValues extractTargetValues(ProceedingJoinPoint joinPoint) {

        ActivityTargetValues values = new ActivityTargetValues();

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();

        Annotation[][] parameterAnnotations =
                signature.getMethod().getParameterAnnotations();

        Object[] args = joinPoint.getArgs();

        for (int i = 0; i < parameterAnnotations.length; i++) {

            for (Annotation annotation : parameterAnnotations[i]) {

                if (annotation instanceof ActivityTarget target) {

                    Object value = args[i];

                    if (value == null) {
                        continue;
                    }

                    switch (target.value()) {
                        case TARGET_USER_ID ->
                                values.targetUserId = String.valueOf(value);

                        case TARGET_ROLE ->
                                values.targetRole = String.valueOf(value);

                        case EXAM_ID ->
                                values.examId = toLong(value);

                        case ATTEMPT_ID ->
                                values.attemptId = toLong(value);

                        case QUESTION_ID ->
                                values.questionId = toLong(value);

                        case CLASS_OFFERING_ID ->
                                values.classOfferingId = String.valueOf(value);

                        case METADATA ->
                                values.metadata = String.valueOf(value);
                    }
                }
            }
        }

        return values;
    }

    private Long toLong(Object value) {
        try {
            if (value instanceof Long longValue) {
                return longValue;
            }

            if (value instanceof Integer intValue) {
                return intValue.longValue();
            }

            return Long.parseLong(String.valueOf(value));

        } catch (Exception e) {
            return null;
        }
    }

    private String buildMessage(
            TrackActivity trackActivity,
            ProceedingJoinPoint joinPoint,
            Throwable ex
    ) {
        String customMessage = trackActivity.message();

        if (customMessage != null && !customMessage.isBlank()) {
            if (ex == null) {
                return customMessage;
            }

            return customMessage + " | Error: " + ex.getMessage();
        }

        if (ex == null) {
            return "Method completed: " + joinPoint.getSignature().getName();
        }

        return "Method failed: " + joinPoint.getSignature().getName()
                + " | Error: " + ex.getMessage();
    }

    private HttpServletRequest getCurrentRequest() {
        try {
            ServletRequestAttributes attributes =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

            return attributes == null ? null : attributes.getRequest();

        } catch (Exception e) {
            return null;
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");

        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }

        String realIp = request.getHeader("X-Real-IP");

        if (realIp != null && !realIp.isBlank()) {
            return realIp;
        }

        return request.getRemoteAddr();
    }

    private static class ActivityTargetValues {

        private String targetUserId;
        private String targetRole;

        private Long examId;
        private Long attemptId;
        private Long questionId;

        private String classOfferingId;
        private String metadata;
    }
}