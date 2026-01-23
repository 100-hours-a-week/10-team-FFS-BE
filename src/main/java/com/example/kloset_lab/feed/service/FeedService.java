package com.example.kloset_lab.feed.service;

import com.example.kloset_lab.clothes.entity.Clothes;
import com.example.kloset_lab.clothes.repository.ClothesRepository;
import com.example.kloset_lab.feed.dto.ClothesDto;
import com.example.kloset_lab.feed.dto.FeedCreateRequest;
import com.example.kloset_lab.feed.dto.FeedDetailResponse;
import com.example.kloset_lab.feed.entity.Feed;
import com.example.kloset_lab.feed.entity.FeedClothesMapping;
import com.example.kloset_lab.feed.entity.FeedImage;
import com.example.kloset_lab.feed.repository.FeedClothesMappingRepository;
import com.example.kloset_lab.feed.repository.FeedImageRepository;
import com.example.kloset_lab.feed.repository.FeedRepository;
import com.example.kloset_lab.global.exception.CustomException;
import com.example.kloset_lab.global.exception.ErrorCode;
import com.example.kloset_lab.media.entity.MediaFile;
import com.example.kloset_lab.media.entity.Purpose;
import com.example.kloset_lab.media.repository.MediaFileRepository;
import com.example.kloset_lab.media.service.MediaService;
import com.example.kloset_lab.user.dto.UserProfileDto;
import com.example.kloset_lab.user.entity.User;
import com.example.kloset_lab.user.entity.UserProfile;
import com.example.kloset_lab.user.repository.UserProfileRepository;
import com.example.kloset_lab.user.repository.UserRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FeedService {

    private final FeedRepository feedRepository;
    private final FeedImageRepository feedImageRepository;
    private final FeedClothesMappingRepository feedClothesMappingRepository;
    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final MediaFileRepository mediaFileRepository;
    private final ClothesRepository clothesRepository;
    private final MediaService mediaService;

    /**
     * 피드 생성
     *
     * @param userId  현재 사용자 ID
     * @param request 피드 생성 요청
     * @return 생성된 피드 상세 정보
     */
    @Transactional
    public FeedDetailResponse createFeed(Long userId, FeedCreateRequest request) {
        User user = userRepository.findById(userId).orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        mediaService.confirmFileUpload(userId, Purpose.FEED, request.fileIds());

        List<MediaFile> mediaFiles = mediaFileRepository.findAllById(request.fileIds());

        List<Clothes> clothesList = validateAndGetClothes(userId, request.clothesIds());

        Feed feed = feedRepository.save(
                Feed.builder().user(user).content(request.content()).build());

        saveFeedImages(feed, mediaFiles, request.fileIds());

        if (!clothesList.isEmpty()) {
            saveFeedClothesMappings(feed, clothesList);
        }

        return buildFeedDetailResponse(feed, userId);
    }

    /**
     * 옷 매핑 정보 검증 및 조회
     */
    private List<Clothes> validateAndGetClothes(Long userId, List<Long> clothesIds) {
        if (clothesIds == null || clothesIds.isEmpty()) {
            return List.of();
        }

        List<Clothes> clothesList = clothesRepository.findAllById(clothesIds);

        if (clothesList.size() != clothesIds.size()) {
            throw new CustomException(ErrorCode.CLOTHES_NOT_FOUND);
        }

        for (Clothes clothes : clothesList) {
            if (!clothes.getUser().getId().equals(userId)) {
                throw new CustomException(ErrorCode.CLOTHES_ACCESS_DENIED);
            }
        }

        return clothesList;
    }

    /**
     * 피드 이미지 저장
     */
    private void saveFeedImages(Feed feed, List<MediaFile> mediaFiles, List<Long> fileIds) {
        List<FeedImage> feedImages = new ArrayList<>();

        for (int i = 0; i < fileIds.size(); i++) {
            Long fileId = fileIds.get(i);
            MediaFile mediaFile = mediaFiles.stream()
                    .filter(mf -> mf.getId().equals(fileId))
                    .findFirst()
                    .orElseThrow(() -> new CustomException(ErrorCode.FILE_NOT_FOUND));

            feedImages.add(FeedImage.builder()
                    .feed(feed)
                    .file(mediaFile)
                    .displayOrder(i)
                    .isPrimary(i == 0)
                    .build());
        }

        feedImageRepository.saveAll(feedImages);
    }

    /**
     * 옷 매핑 저장
     */
    private void saveFeedClothesMappings(Feed feed, List<Clothes> clothesList) {

        List<FeedClothesMapping> mappings = clothesList.stream()
                .map(clothes ->
                        FeedClothesMapping.builder().feed(feed).clothes(clothes).build())
                .toList();

        feedClothesMappingRepository.saveAll(mappings);
    }

    /**
     * 피드 상세 응답 생성
     */
    private FeedDetailResponse buildFeedDetailResponse(Feed feed, Long currentUserId) {
        List<FeedImage> feedImages = feedImageRepository.findByFeedIdOrderByDisplayOrderAsc(feed.getId());
        List<Long> fileIds = feedImages.stream().map(fi -> fi.getFile().getId()).toList();
        List<String> imageUrls = mediaService.getFileFullUrl(fileIds);

        List<FeedClothesMapping> mappings = feedClothesMappingRepository.findByFeedId(feed.getId());
        List<ClothesDto> clothesDtoList = mappings.stream()
                .map(mapping -> {
                    Clothes clothes = mapping.getClothes();
                    String clothesImageUrl = mediaService
                            .getFileFullUrl(List.of(clothes.getFile().getId()))
                            .getFirst();
                    return new ClothesDto(
                            clothes.getId(), clothesImageUrl, clothes.getClothesName(), clothes.getPrice());
                })
                .toList();

        UserProfile userProfile = userProfileRepository
                .findByUserId(feed.getUser().getId())
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        String profileImageUrl = Optional.ofNullable(userProfile.getProfileFile())
                .map(pf -> mediaService.getFileFullUrl(List.of(pf.getId())).getFirst())
                .orElse(null);

        UserProfileDto userProfileDto =
                new UserProfileDto(feed.getUser().getId(), profileImageUrl, userProfile.getNickname());

        boolean isOwner = feed.getUser().getId().equals(currentUserId);

        return FeedDetailResponse.builder()
                .feedId(feed.getId())
                .imageUrls(imageUrls)
                .likeCount(feed.getLikeCount())
                .commentCount(feed.getCommentCount())
                .postedTime(feed.getCreatedAt())
                .clothes(clothesDtoList.isEmpty() ? null : clothesDtoList)
                .content(feed.getContent())
                .userProfile(userProfileDto)
                .isFollowing(false) // 팔로우 기능 미구현
                .isLiked(false)
                .isOwner(isOwner)
                .build();
    }
}
