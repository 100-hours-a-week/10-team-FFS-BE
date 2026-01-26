package com.example.kloset_lab.media.service;

import com.example.kloset_lab.media.dto.PresignedUrlInfo;
import com.example.kloset_lab.media.entity.FileType;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile("prod")
public class MockStorageService implements StorageService {
    @Override
    public PresignedUrlInfo generatePresignedUrl(String fileName, FileType fileType) {
        return PresignedUrlInfo.builder()
                .presignedUrl("https://sample-presigned-url")
                .objectKey("sample-object-key")
                .build();
    }

    @Override
    public void validateUpload(String objectKey, FileType expectedFileType) {}

    @Override
    public String getFullImageUrl(String objectKey) {
        return "https://sample-full-url";
    }
}
