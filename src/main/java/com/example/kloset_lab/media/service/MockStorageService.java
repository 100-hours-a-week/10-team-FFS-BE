package com.example.kloset_lab.media.service;

import com.example.kloset_lab.media.dto.PresignedUrlInfo;
import com.example.kloset_lab.media.entity.FileType;

public class MockStorageService implements StorageService {
    @Override
    public PresignedUrlInfo generatePresignedUrl(String fileName, FileType fileType) {
        return PresignedUrlInfo.builder()
                .presignedUrl("sample")
                .objectKey("sample-object-key")
                .build();
    }

    @Override
    public void validateUpload(String objectKey, FileType expectedFileType) {}

    @Override
    public String getFullImageUrl(String objectKey) {
        return "https://www.emoneynews.co.kr/news/photo/202505/119827_47885_3941.jpg";
    }
}
