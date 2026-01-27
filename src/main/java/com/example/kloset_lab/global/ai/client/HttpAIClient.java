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
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Profile("prod")
@RequiredArgsConstructor
@Component
public class HttpAIClient implements AIClient {
    private final RestClient restClient;
    private final MediaService mediaService;

    @Override
    public ValidateResponse validateImages(Long userId, List<String> imageUrlList) {
        ValidateRequest validateRequest =
                ValidateRequest.builder().userId(userId).images(imageUrlList).build();
        return restClient
                .post()
                .uri("/v1/closet/validate")
                .body(validateRequest)
                .retrieve()
                .body(ValidateResponse.class);
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
                    .fileInfo(fileUploadResponses.get(i))
                    .build();
            imageInfos.add(imageInfo);
        }
        AnalyzeRequest analyzeRequest = AnalyzeRequest.builder()
                .userId(userId)
                .batchId(UlidCreator.getUlid().toString())
                .images(imageInfos)
                .build();
        return restClient
                .post()
                .uri("/v1/closet/analyze")
                .body(analyzeRequest)
                .retrieve()
                .body(BatchResponse.class);
    }

    @Override
    public BatchResponse getBatchStatus(String batchId) {
        return restClient.get().uri("/v1/closet/analyze/" + batchId).retrieve().body(BatchResponse.class);
    }

    @Override
    public EmbeddingResponse saveEmbedding(EmbeddingRequest request) {
        return restClient
                .post()
                .uri("/v1/closet/embedding")
                .body(request)
                .retrieve()
                .body(EmbeddingResponse.class);
    }

    @Override
    public void deleteClothes(Long clothesId) {
        restClient.delete().uri("/v1/closet/" + clothesId).retrieve();
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
        return restClient
                .post()
                .uri("/v1/closet/outfit")
                .body(outfitRequest)
                .retrieve()
                .body(OutfitResponse.class);
    }

    @Override
    public ShopResponse searchShop(Long userId, String query) {
        // TODO : V2에서 구현
        return null;
    }

    private List<FileUploadInfo> createFileUploadInfos(int count) {
        return Collections.nCopies(count, new FileUploadInfo("ai_result.png", "image/png"));
    }
}
