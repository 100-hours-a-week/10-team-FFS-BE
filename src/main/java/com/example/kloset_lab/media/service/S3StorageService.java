package com.example.kloset_lab.media.service;

import com.example.kloset_lab.global.exception.CustomException;
import com.example.kloset_lab.global.exception.ErrorCode;
import com.example.kloset_lab.media.dto.PresignedUrlInfo;
import java.time.Duration;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;

@Service
@RequiredArgsConstructor
public class S3StorageService {

    private static final long MAX_IMAGE_SIZE_BYTES = 10L * 1024 * 1024;

    @Value("${aws.s3.bucket}")
    private String bucketName;

    @Value("${cloud.aws.region.static}")
    private String region;

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    public PresignedUrlInfo generatePresignedUrl(String fileName, String fileType) {

        String objectKey = generateObjectKey(fileName);

        PutObjectRequest objectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(objectKey)
                .contentType(fileType)
                .build();

        PresignedPutObjectRequest presignedRequest = s3Presigner.presignPutObject(
                r -> r.signatureDuration(Duration.ofMinutes(10)).putObjectRequest(objectRequest));

        return PresignedUrlInfo.builder()
                .presignedUrl(presignedRequest.url().toString())
                .objectKey(objectKey)
                .build();
    }

    public void validateUpload(String objectKey, String expectedFileType) {
        try {
            HeadObjectRequest headObjectRequest = HeadObjectRequest.builder()
                    .bucket(bucketName)
                    .key(objectKey)
                    .build();

            HeadObjectResponse response = s3Client.headObject(headObjectRequest);

            String actualFileType = response.contentType();
            if (!expectedFileType.equals(actualFileType)) {
                throw new CustomException(ErrorCode.UPLOADED_FILE_MISMATCH);
            }
            long actualFileSize = response.contentLength();
            if (actualFileSize != MAX_IMAGE_SIZE_BYTES) {
                throw new CustomException(ErrorCode.FILE_TOO_LARGE);
            }
        } catch (S3Exception e) {
            if (e.statusCode() == 404) {
                throw new CustomException(ErrorCode.FILE_NOT_FOUND);
            }
            throw new CustomException(ErrorCode.IMAGE_PROCESSING_ERROR);
        }
    }

    public String getFullImageUrl(String objectKey) {
        return "https://" + bucketName + ".s3." + region + ".amazonaws.com/" + objectKey;
    }

    private String generateObjectKey(String fileName) {
        String uuid = UUID.randomUUID().toString();
        String extension = extractExtension(fileName);
        return uuid + "." + extension;
    }

    private String extractExtension(String fileName) {
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex > 0 && lastDotIndex < fileName.length() - 1) {
            return fileName.substring(lastDotIndex + 1);
        }
        return "";
    }
}
