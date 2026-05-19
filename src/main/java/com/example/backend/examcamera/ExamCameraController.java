package com.example.backend.examcamera;

import com.example.backend.examcamera.dto.*;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Controller
@RequestMapping
public class ExamCameraController {

    private final ExamCameraService cameraService;
    private final ExamCameraPreviewFrameStore previewFrameStore;

    public ExamCameraController(ExamCameraService cameraService, ExamCameraPreviewFrameStore previewFrameStore) {
        this.cameraService = cameraService;
        this.previewFrameStore = previewFrameStore;
    }

    @ResponseBody
    @PostMapping("/exam-camera/sessions")
    public ResponseEntity<CreateCameraSessionResponse> createSession(
            @RequestBody CreateCameraSessionRequest request
    ) {
        return ResponseEntity.ok(cameraService.createSession(request));
    }

    @ResponseBody
    @GetMapping("/exam-camera/sessions/{token}")
    public ResponseEntity<CameraSessionStatusResponse> getStatus(
            @PathVariable String token
    ) {
        return ResponseEntity.ok(cameraService.getStatus(token));
    }

    @ResponseBody
    @PostMapping("/exam-camera/sessions/{token}/pair")
    public ResponseEntity<CameraSessionStatusResponse> pairPhone(
            @PathVariable String token,
            @RequestBody(required = false) PairCameraRequest request
    ) {
        return ResponseEntity.ok(cameraService.pairPhone(token, request));
    }

    @ResponseBody
    @PostMapping("/exam-camera/sessions/{token}/heartbeat")
    public ResponseEntity<CameraSessionStatusResponse> heartbeat(
            @PathVariable String token
    ) {
        return ResponseEntity.ok(cameraService.heartbeat(token));
    }

    @ResponseBody
    @PostMapping("/exam-camera/sessions/{token}/end")
    public ResponseEntity<CameraSessionStatusResponse> endSession(
            @PathVariable String token
    ) {
        return ResponseEntity.ok(cameraService.endSession(token));
    }

    @ResponseBody
    @PostMapping(
            value = "/exam-camera/sessions/{token}/preview-frame",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<Void> uploadPreviewFrame(
            @PathVariable String token,
            @RequestParam("frame") MultipartFile frame
    ) throws Exception {
        previewFrameStore.put(token, frame.getBytes());
        return ResponseEntity.ok().build();
    }

    @ResponseBody
    @GetMapping(
            value = "/exam-camera/sessions/{token}/preview-frame",
            produces = MediaType.IMAGE_JPEG_VALUE
    )
    public ResponseEntity<byte[]> getPreviewFrame(
            @PathVariable String token
    ) {
        ExamCameraPreviewFrameStore.PreviewFrame frame =
                previewFrameStore.get(token);

        if (frame == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .body(frame.bytes());
    }

    @GetMapping("/camera-pair/{token}")
    public String cameraPairPage(
            @PathVariable String token,
            Model model
    ) {
        return "forward:/camera-pair.html";
    }
}