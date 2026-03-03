package com.example.kloset_lab.global.ai.http.client;

import com.example.kloset_lab.global.ai.http.dto.*;
import com.example.kloset_lab.global.exception.CustomException;
import com.example.kloset_lab.global.exception.ErrorCode;
import com.example.kloset_lab.media.dto.FileUploadInfo;
import com.example.kloset_lab.media.dto.FileUploadResponse;
import com.example.kloset_lab.media.entity.Purpose;
import com.example.kloset_lab.media.service.MediaService;
import com.github.f4b6a3.ulid.UlidCreator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Slf4j
@RequiredArgsConstructor
@Component
public class HttpAIClient implements AIClient {
    private final RestClient restClient;
    private final MediaService mediaService;

    @Override
    public ValidateResponse validateImages(Long userId, List<String> imageUrlList) {
        return ValidateResponse.builder()
                .success(true)
                .validationSummary(ValidateResponse.ValidationSummary.builder()
                        .total(imageUrlList.size())
                        .passed(imageUrlList.size())
                        .failed(0)
                        .build())
                .validationResults(imageUrlList.stream()
                        .map(url -> ValidateResponse.ValidationResult.builder()
                                .originUrl(url)
                                .passed(true)
                                .build())
                        .toList())
                .build();
        // TODO: V2에서 어뷰징 처리 기능 연동
        /*
        ValidateRequest validateRequest =
                ValidateRequest.builder().userId(userId).images(imageUrlList).build();
        return restClient
                .post()
                .uri("/v1/closet/validate")
                .body(validateRequest)
                .retrieve()
                .body(ValidateResponse.class);
        */
    }

    @Override
    public BatchResponse analyzeImages(Long userId, List<String> imageUrlList) {
        List<FileUploadInfo> fileUploadInfos = createFileUploadInfos(imageUrlList.size());
        List<FileUploadResponse> fileUploadResponses =
                mediaService.requestFileUpload(userId, Purpose.CLOTHES, fileUploadInfos);

        List<AnalyzeRequest.ImageInfo> imageInfos = new ArrayList<>();
        for (int i = 0; i < imageUrlList.size(); i++) {
            AnalyzeRequest.ImageInfo imageInfo = AnalyzeRequest.ImageInfo.builder()
                    .sequence(i)
                    .targetImage(imageUrlList.get(i))
                    .taskId(UlidCreator.getUlid().toString())
                    .fileUploadInfo(fileUploadResponses.get(i))
                    .build();
            imageInfos.add(imageInfo);
        }

        String batchId = UlidCreator.getUlid().toString();
        AnalyzeRequest analyzeRequest = AnalyzeRequest.builder()
                .userId(userId)
                .batchId(batchId)
                .images(imageInfos)
                .build();

        try {
            BatchResponse response = restClient
                    .post()
                    .uri("/v1/closet/analyze")
                    .body(analyzeRequest)
                    .retrieve()
                    .body(BatchResponse.class);

            return response;

        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    @Override
    public OutfitResponse recommendOutfit(Long userId, String query) {
        List<FileUploadInfo> fileUploadInfos = createFileUploadInfos(3);
        List<FileUploadResponse> fileUploadResponses =
                mediaService.requestFileUpload(userId, Purpose.OUTFIT, fileUploadInfos);
        OutfitRequest outfitRequest = OutfitRequest.builder()
                .userId(userId)
                .query(query)
                .sessionId(null)
                .weather(null)
                .urls(fileUploadResponses)
                .build();
        long startedAtNanos = System.nanoTime();

        try {
            OutfitResponse response = restClient
                    .post()
                    .uri("/v1/closet/outfit")
                    .body(outfitRequest)
                    .retrieve()
                    .body(OutfitResponse.class);
            log.info("AI-BE TPO 코디 추천 성공: userId={}, elapsedMs={}", userId, elapsedMillis(startedAtNanos));
            return response;
        } catch (ResourceAccessException e) {
            int deletedCount = cleanupPendingOutfitFiles(userId, fileUploadResponses);
            log.warn(
                    "AI-BE TPO 코디 추천 타임아웃: userId={}, elapsedMs={}, cleanedUpFileCount={}",
                    userId,
                    elapsedMillis(startedAtNanos),
                    deletedCount,
                    e);
            throw new CustomException(ErrorCode.AI_TIMEOUT);
        } catch (RestClientResponseException e) {
            int deletedCount = cleanupPendingOutfitFiles(userId, fileUploadResponses);
            int statusCode = e.getStatusCode().value();

            if (isUpstreamTimeout(statusCode)) {
                log.warn(
                        "AI-BE TPO 코디 추천 업스트림 타임아웃: userId={}, statusCode={}, elapsedMs={}, cleanedUpFileCount={}",
                        userId,
                        statusCode,
                        elapsedMillis(startedAtNanos),
                        deletedCount,
                        e);
                throw new CustomException(ErrorCode.AI_TIMEOUT);
            }

            log.warn(
                    "AI-BE TPO 코디 추천 응답 오류: userId={}, statusCode={}, elapsedMs={}, cleanedUpFileCount={}",
                    userId,
                    statusCode,
                    elapsedMillis(startedAtNanos),
                    deletedCount,
                    e);
            throw new CustomException(ErrorCode.AI_SERVER_ERROR);
        } catch (RestClientException e) {
            int deletedCount = cleanupPendingOutfitFiles(userId, fileUploadResponses);
            log.warn(
                    "AI-BE TPO 코디 추천 통신 오류: userId={}, elapsedMs={}, cleanedUpFileCount={}",
                    userId,
                    elapsedMillis(startedAtNanos),
                    deletedCount,
                    e);
            throw new CustomException(ErrorCode.AI_SERVER_ERROR);
        }
    }

    @Override
    public BatchResponse getBatchStatus(String batchId) {

        try {
            BatchResponse response = restClient
                    .get()
                    .uri("/v1/closet/batches/" + batchId)
                    .retrieve()
                    .body(BatchResponse.class);

            return response;

        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    @Override
    public EmbeddingResponse saveEmbedding(EmbeddingRequest request) {
        try {
            EmbeddingResponse response = restClient
                    .post()
                    .uri("/v1/closet/embedding")
                    .body(request)
                    .retrieve()
                    .body(EmbeddingResponse.class);
            return response;

        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    @Override
    public void deleteClothes(Long clothesId) {
        try {
            restClient.delete().uri("/v1/closet/" + clothesId).retrieve();
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    @Override
    public ShopResponse searchShop(Long userId, String query) {
        ShopRequest shopRequest = ShopRequest.builder()
                .userId(userId)
                .query(query)
                .sessionId(null)
                .build();
        long startedAtNanos = System.nanoTime();

        try {
            ShopResponse response = restClient
                    .post()
                    .uri("/v2/shop/outfit")
                    .body(shopRequest)
                    .retrieve()
                    .body(ShopResponse.class);
            log.info("AI-BE 쇼핑 검색 성공: userId={}, elapsedMs={}", userId, elapsedMillis(startedAtNanos));
            return response;
        } catch (ResourceAccessException e) {
            log.warn(
                    "AI-BE 쇼핑 검색 타임아웃: userId={}, query={}, elapsedMs={}",
                    userId,
                    query,
                    elapsedMillis(startedAtNanos),
                    e);
            throw new CustomException(ErrorCode.AI_TIMEOUT);
        } catch (RestClientResponseException e) {
            int statusCode = e.getStatusCode().value();

            if (isUpstreamTimeout(statusCode)) {
                log.warn(
                        "AI-BE 쇼핑 검색 업스트림 타임아웃: userId={}, query={}, statusCode={}, elapsedMs={}",
                        userId,
                        query,
                        statusCode,
                        elapsedMillis(startedAtNanos),
                        e);
                throw new CustomException(ErrorCode.AI_TIMEOUT);
            }

            log.warn(
                    "AI-BE 쇼핑 검색 응답 오류: userId={}, query={}, statusCode={}, elapsedMs={}",
                    userId,
                    query,
                    statusCode,
                    elapsedMillis(startedAtNanos),
                    e);
            throw new CustomException(ErrorCode.AI_SERVER_ERROR);
        } catch (RestClientException e) {
            log.warn(
                    "AI-BE 쇼핑 검색 통신 오류: userId={}, query={}, elapsedMs={}",
                    userId,
                    query,
                    elapsedMillis(startedAtNanos),
                    e);
            throw new CustomException(ErrorCode.AI_SERVER_ERROR);
        }
    }

    private List<FileUploadInfo> createFileUploadInfos(int count) {
        return Collections.nCopies(count, new FileUploadInfo("ai_result.jpeg", "image/jpeg"));
    }

    private int cleanupPendingOutfitFiles(Long userId, List<FileUploadResponse> fileUploadResponses) {
        List<Long> fileIds =
                fileUploadResponses.stream().map(FileUploadResponse::fileId).toList();

        try {
            return mediaService.deletePendingFiles(userId, Purpose.OUTFIT, fileIds);
        } catch (Exception cleanupException) {
            log.error(
                    "AI-BE TPO 코디 추천 실패 후 media_file 정리 실패: userId={}, fileIds={}", userId, fileIds, cleanupException);
            return 0;
        }
    }

    private boolean isUpstreamTimeout(int statusCode) {
        return statusCode == 408 || statusCode == 504;
    }

    private long elapsedMillis(long startedAtNanos) {
        return (System.nanoTime() - startedAtNanos) / 1_000_000;
    }
}
