package com.example.kloset_lab.media.service;

import com.example.kloset_lab.media.entity.FileType;

/** S3 업로드 완료 여부 검증 포트 인터페이스 */
public interface FileUploadValidator {

    /**
     * objectKey에 해당하는 파일이 S3에 실제로 업로드됐는지 검증한다.
     *
     * @param objectKey S3 오브젝트 키
     * @param expectedFileType 기대하는 파일 타입
     */
    void validateUpload(String objectKey, FileType expectedFileType);
}
