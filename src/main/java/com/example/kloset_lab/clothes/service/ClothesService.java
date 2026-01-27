package com.example.kloset_lab.clothes.service;

import com.example.kloset_lab.clothes.dto.ClothesCreateRequest;
import com.example.kloset_lab.clothes.dto.ClothesDetailResponse;
import com.example.kloset_lab.clothes.dto.ClothesListItem;
import com.example.kloset_lab.clothes.dto.ClothesUpdateRequest;
import com.example.kloset_lab.clothes.entity.Category;
import com.example.kloset_lab.clothes.entity.Clothes;
import com.example.kloset_lab.clothes.repository.ClothesRepository;
import com.example.kloset_lab.global.exception.CustomException;
import com.example.kloset_lab.global.exception.ErrorCode;
import com.example.kloset_lab.global.response.PageInfo;
import com.example.kloset_lab.global.response.PagedResponse;
import com.example.kloset_lab.media.entity.MediaFile;
import com.example.kloset_lab.media.entity.Purpose;
import com.example.kloset_lab.media.repository.MediaFileRepository;
import com.example.kloset_lab.media.service.MediaService;
import com.example.kloset_lab.media.service.StorageService;
import com.example.kloset_lab.user.entity.User;
import com.example.kloset_lab.user.repository.UserRepository;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClothesService {

    private final UserRepository userRepository;
    private final ClothesRepository clothesRepository;
    private final MediaFileRepository mediaFileRepository;
    private final MediaService mediaService;
    private final StorageService storageService;

    @Transactional
    public ClothesDetailResponse createClothes(Long currentUserId, ClothesCreateRequest request) {
        User user =
                userRepository.findById(currentUserId).orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        mediaService.confirmFileUpload(currentUserId, Purpose.CLOTHES, List.of(request.fileId()));
        MediaFile mediaFile = mediaFileRepository
                .findById(request.fileId())
                .orElseThrow(() -> new CustomException(ErrorCode.FILE_NOT_FOUND));
        Clothes clothes = Clothes.builder()
                .user(user)
                .file(mediaFile)
                .name(request.name())
                .brandName(request.brand())
                .price(request.price())
                .size(request.size())
                .boughtDate(request.boughtDate())
                .category(request.category())
                .colors(request.color())
                .materials(request.material())
                .styleTags(request.styleTag())
                .build();

        clothes = clothesRepository.save(clothes);

        String imageUrl = storageService.getFullImageUrl(mediaFile.getObjectKey());
        return ClothesDetailResponse.from(clothes, imageUrl, true);
    }

    @Transactional(readOnly = true)
    public ClothesDetailResponse getClothesDetail(Long currentUserId, Long clothesId) {
        Clothes clothes = clothesRepository
                .findById(clothesId)
                .orElseThrow(() -> new CustomException(ErrorCode.CLOTHES_DETAIL_NOT_FOUND));

        String imageUrl = storageService.getFullImageUrl(clothes.getFile().getObjectKey());

        return ClothesDetailResponse.from(clothes, imageUrl, clothes.isOwner(currentUserId));
    }

    @Transactional
    public ClothesDetailResponse updateClothes(Long currentUserId, Long clothesId, ClothesUpdateRequest request) {
        Clothes clothes = clothesRepository
                .findById(clothesId)
                .orElseThrow(() -> new CustomException(ErrorCode.CLOTHES_NOT_FOUND));

        if (!clothes.isOwner(currentUserId)) {
            throw new CustomException(ErrorCode.CLOTHES_EDIT_DENIED);
        }

        clothes.update(
                request.name(),
                request.brand(),
                request.price(),
                request.size(),
                request.boughtDate(),
                request.category(),
                request.color(),
                request.material());

        String imageUrl = storageService.getFullImageUrl(clothes.getFile().getObjectKey());
        return ClothesDetailResponse.from(clothes, imageUrl, true);
    }

    @Transactional
    public void deleteClothes(Long currentUserId, Long clothesId) {
        Clothes clothes = clothesRepository
                .findById(clothesId)
                .orElseThrow(() -> new CustomException(ErrorCode.CLOTHES_NOT_FOUND));

        if (!clothes.isOwner(currentUserId)) {
            throw new CustomException(ErrorCode.CLOTHES_DELETE_DENIED);
        }

        clothes.softDelete();
    }

    /**
     * 사용자의 옷 개수 조회
     *
     * @param userId 사용자 ID
     * @return 옷 개수
     */
    public long getClothesCount(Long userId) {
        return clothesRepository.countByUserId(userId);
    }

    /**
     * 특정 유저의 옷 목록 조회 (카테고리 필터링 옵션)
     *
     * @param targetUserId 조회 대상 유저 ID
     * @param category 카테고리 (null이면 전체 조회)
     * @param after 커서 (마지막 옷 ID)
     * @param limit 조회 개수
     * @return 옷 목록
     */
    public PagedResponse<ClothesListItem> getClothes(Long targetUserId, Category category, Long after, int limit) {
        Slice<Clothes> clothesSlice;
        if (category == null) {
            clothesSlice = clothesRepository.findByCursor(targetUserId, after, PageRequest.of(0, limit));
        } else {
            clothesSlice =
                    clothesRepository.findByCursorAndCategory(targetUserId, category, after, PageRequest.of(0, limit));
        }
        List<Clothes> clothes = clothesSlice.getContent();

        List<ClothesListItem> items = new ArrayList<>();
        for (Clothes c : clothes) {
            items.add(ClothesListItem.builder()
                    .clothesId(c.getId())
                    .imageUrl(mediaService.getFileFullUrl(c.getFile().getId()))
                    .build());
        }

        PageInfo pageInfo = PageInfo.builder()
                .hasNextPage(clothesSlice.hasNext())
                .nextCursor(clothesSlice.hasNext() ? items.getLast().clothesId() : null)
                .build();

        return PagedResponse.<ClothesListItem>builder()
                .items(items)
                .pageInfo(pageInfo)
                .build();
    }
}
