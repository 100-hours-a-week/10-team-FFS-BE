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

@Slf4j
@RequiredArgsConstructor
@Component
public class HttpAIClient implements AIClient {
    private final RestClient restClient;
    private final MediaService mediaService;

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

        try {
            return restClient
                    .post()
                    .uri("/ai/v2/shop/outfit")
                    .body(shopRequest)
                    .retrieve()
                    .body(ShopResponse.class);
        } catch (ResourceAccessException e) {
            log.warn("AI-BE 쇼핑 검색 타임아웃: userId={}, query={}", userId, query, e);
            throw new CustomException(ErrorCode.AI_TIMEOUT);
        } catch (RestClientException e) {
            log.warn("AI-BE 쇼핑 검색 오류: userId={}, query={}", userId, query, e);
            throw new CustomException(ErrorCode.AI_SERVER_ERROR);
        }
    }

    private List<FileUploadInfo> createFileUploadInfos(int count) {
        return Collections.nCopies(count, new FileUploadInfo("ai_result.jpeg", "image/jpeg"));
    }
}
