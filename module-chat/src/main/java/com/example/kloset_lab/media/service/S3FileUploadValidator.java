package com.example.kloset_lab.media.service;

import com.example.kloset_lab.global.exception.CustomException;
import com.example.kloset_lab.global.exception.ErrorCode;
import com.example.kloset_lab.media.entity.FileType;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;

/** 채팅 이미지 S3 업로드 확인용 FileUploadValidator 구현체. */
@Service
@RequiredArgsConstructor
public class S3FileUploadValidator implements FileUploadValidator {

    private static final long MAX_IMAGE_SIZE_BYTES = 10L * 1024 * 1024;

    @Value("${cloud.aws.s3.bucket}")
    private String bucketName;

    private final S3Client s3Client;

    @Override
    public void validateUpload(String objectKey, FileType expectedFileType) {
        try {
            HeadObjectRequest headObjectRequest = HeadObjectRequest.builder()
                    .bucket(bucketName)
                    .key(objectKey)
                    .build();

            HeadObjectResponse response = s3Client.headObject(headObjectRequest);

            String actualFileType = response.contentType();
            if (!FileType.PNG.getMimeType().equals(actualFileType)
                    && !FileType.JPEG.getMimeType().equals(actualFileType)) {
                throw new CustomException(ErrorCode.UPLOADED_FILE_MISMATCH);
            }
            long actualFileSize = response.contentLength();
            if (actualFileSize > MAX_IMAGE_SIZE_BYTES) {
                throw new CustomException(ErrorCode.FILE_SIZE_EXCEEDS_10MB);
            }
        } catch (S3Exception e) {
            if (e.statusCode() == 404) {
                throw new CustomException(ErrorCode.FILE_NOT_FOUND);
            }
            throw new CustomException(ErrorCode.IMAGE_PROCESSING_ERROR);
        }
    }
}
