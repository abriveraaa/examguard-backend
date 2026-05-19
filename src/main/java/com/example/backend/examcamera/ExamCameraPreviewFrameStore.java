package com.example.backend.examcamera;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ExamCameraPreviewFrameStore {

    public record PreviewFrame(byte[] bytes, Instant updatedAt) {}

    private final Map<String, PreviewFrame> frameMap = new ConcurrentHashMap<>();

    public void put(String token, byte[] bytes) {
        frameMap.put(token, new PreviewFrame(bytes, Instant.now()));
    }

    public PreviewFrame get(String token) {
        return frameMap.get(token);
    }

    public void remove(String token) {
        frameMap.remove(token);
    }
}