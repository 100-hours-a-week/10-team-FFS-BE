package com.example.kloset_lab.media.controller;

import com.example.kloset_lab.global.response.ApiResponse;
import com.example.kloset_lab.global.response.ApiResponses;
import com.example.kloset_lab.global.response.Message;
import com.example.kloset_lab.media.dto.FileUploadInternalRequest;
import com.example.kloset_lab.media.dto.FileUploadRequest;
import com.example.kloset_lab.media.dto.FileUploadResponse;
import com.example.kloset_lab.media.service.MediaService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class MediaController {
    private final MediaService mediaService;

    @PostMapping("/v1/presigned-url")
    public ResponseEntity<ApiResponse<List<FileUploadResponse>>> generatePresignedUrl(
            @AuthenticationPrincipal Long userId, @RequestBody FileUploadRequest fileUploadRequest) {

        List<FileUploadResponse> fileUploadResponseList =
                mediaService.requestFileUpload(userId, fileUploadRequest.purpose(), fileUploadRequest.files());

        return ApiResponses.created(Message.PRESIGNED_URL_GENERATED, fileUploadResponseList);
    }

    @PostMapping("/internal/presigned-url")
    public ResponseEntity<ApiResponse<List<FileUploadResponse>>> generatePresignedUrlInternal(
            @RequestBody FileUploadInternalRequest fileUploadRequest) {

        List<FileUploadResponse> fileUploadResponseList = mediaService.requestFileUpload(
                fileUploadRequest.userId(), fileUploadRequest.purpose(), fileUploadRequest.files());

        return ApiResponses.created(Message.PRESIGNED_URL_GENERATED, fileUploadResponseList);
    }
}
