package com.example.backend.dto.exam.response;

import java.io.Serializable;

public class ImageUploadResponse implements Serializable {

    private boolean success;
    private String message;
    private String imageUrl;

    public ImageUploadResponse() {}

    public ImageUploadResponse(boolean success, String message, String imageUrl) {
        this.success = success;
        this.message = message;
        this.imageUrl = imageUrl;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public String getImageUrl() {
        return imageUrl;
    }
}