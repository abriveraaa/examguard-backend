package com.example.backend.dto.exam.result;

public class AssignExamResult {

    private boolean success;
    private String message;
    private Long examId;
    private int assignedCount;

    public AssignExamResult(boolean success, String message, Long examId, int assignedCount) {
        this.success = success;
        this.message = message;
        this.examId = examId;
        this.assignedCount = assignedCount;
    }

    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public Long getExamId() { return examId; }
    public int getAssignedCount() { return assignedCount; }
}