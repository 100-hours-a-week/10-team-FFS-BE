package com.example.kloset_lab.global.ai.client;

import com.example.kloset_lab.global.ai.dto.*;
import com.example.kloset_lab.media.dto.FileUploadInfo;
import com.example.kloset_lab.media.dto.FileUploadResponse;
import com.example.kloset_lab.media.entity.Purpose;
import com.example.kloset_lab.media.service.MediaService;
import com.github.f4b6a3.ulid.UlidCreator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

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

        try {
            OutfitResponse response = restClient
                    .post()
                    .uri("/v1/closet/outfit")
                    .body(outfitRequest)
                    .retrieve()
                    .body(OutfitResponse.class);

            return response;

        } catch (Exception e) {
            e.printStackTrace();
            throw e;
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
        // TODO : V2에서 구현
        return null;
    }

    private List<FileUploadInfo> createFileUploadInfos(int count) {
        return Collections.nCopies(count, new FileUploadInfo("ai_result.jpeg", "image/jpeg"));
    }
}
