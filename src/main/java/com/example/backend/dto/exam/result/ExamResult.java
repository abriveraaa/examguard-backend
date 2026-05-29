package com.example.backend.dto.exam.result;

import java.io.Serializable;

public class ExamResult implements Serializable {

    private boolean success;
    private String message;
    private Long examId;
    private int questionCount;

    public ExamResult(boolean success, String message, Long examId, int questionCount) {
        this.success = success;
        this.message = message;
        this.examId = examId;
        this.questionCount = questionCount;
    }

    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public Long getExamId() { return examId; }
    public int getQuestionCount() { return questionCount; }
}