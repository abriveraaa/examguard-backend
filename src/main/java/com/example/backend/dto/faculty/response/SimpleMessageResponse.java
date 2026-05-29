package com.example.backend.dto.faculty.response;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
public class SimpleMessageResponse implements Serializable {

    private boolean success;
    private String message;

    public SimpleMessageResponse() {
    }

    public SimpleMessageResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
    }
}
