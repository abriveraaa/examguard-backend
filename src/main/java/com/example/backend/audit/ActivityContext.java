package com.example.backend.audit;

public class ActivityContext {

    private static final ThreadLocal<ActivityData> CONTEXT =
            new ThreadLocal<>();

    public static void clear() {
        CONTEXT.remove();
    }

    public static ActivityData get() {
        ActivityData data = CONTEXT.get();

        if (data == null) {
            data = new ActivityData();
            CONTEXT.set(data);
        }

        return data;
    }

    public static void setTargetUserId(String targetUserId) {
        get().setTargetUserId(targetUserId);
    }

    public static void setTargetRole(String targetRole) {
        get().setTargetRole(targetRole);
    }

    public static void setExamId(Long examId) {
        get().setExamId(examId);
    }

    public static void setAttemptId(Long attemptId) {
        get().setAttemptId(attemptId);
    }

    public static void setQuestionId(Long questionId) {
        get().setQuestionId(questionId);
    }

    public static void setClassOfferingId(String classOfferingId) {
        get().setClassOfferingId(classOfferingId);
    }

    public static void setMetadata(String metadata) {
        get().setMetadata(metadata);
    }
}