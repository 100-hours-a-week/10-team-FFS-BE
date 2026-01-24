package com.example.kloset_lab.feed.service;

import com.example.kloset_lab.clothes.entity.Clothes;
import com.example.kloset_lab.clothes.service.ClothesValidationService;
import com.example.kloset_lab.feed.dto.ClothesDto;
import com.example.kloset_lab.feed.dto.FeedCreateRequest;
import com.example.kloset_lab.feed.dto.FeedDetailResponse;
import com.example.kloset_lab.feed.dto.FeedListItem;
import com.example.kloset_lab.feed.dto.FeedUpdateRequest;
import com.example.kloset_lab.feed.dto.LikeResponse;
import com.example.kloset_lab.feed.entity.Feed;
import com.example.kloset_lab.feed.entity.FeedClothesMapping;
import com.example.kloset_lab.feed.entity.FeedImage;
import com.example.kloset_lab.feed.entity.FeedLike;
import com.example.kloset_lab.feed.repository.FeedClothesMappingRepository;
import com.example.kloset_lab.feed.repository.FeedImageRepository;
import com.example.kloset_lab.feed.repository.FeedLikeRepository;
import com.example.kloset_lab.feed.repository.FeedRepository;
import com.example.kloset_lab.global.exception.CustomException;
import com.example.kloset_lab.global.exception.ErrorCode;
import com.example.kloset_lab.global.response.PageInfo;
import com.example.kloset_lab.global.response.PagedResponse;
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
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FeedService {

    private final FeedRepository feedRepository;
    private final FeedImageRepository feedImageRepository;
    private final FeedClothesMappingRepository feedClothesMappingRepository;
    private final FeedLikeRepository feedLikeRepository;
    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final MediaFileRepository mediaFileRepository;
    private final ClothesValidationService clothesValidationService;
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

        List<Clothes> clothesList = clothesValidationService.getVerifiedClothes(userId, request.clothesIds());

        Feed feed = feedRepository.save(
                Feed.builder().user(user).content(request.content()).build());

        saveFeedImages(feed, mediaFiles, request.fileIds());

        if (!clothesList.isEmpty()) {
            saveFeedClothesMappings(feed, clothesList);
        }

        return buildFeedDetailResponse(feed, userId);
    }

    /**
     * 피드 상세 조회
     *
     * @param userId 현재 사용자 ID
     * @param feedId 조회할 피드 ID
     * @return 피드 상세 정보
     */
    public FeedDetailResponse getFeed(Long userId, Long feedId) {
        Feed feed = feedRepository.findById(feedId).orElseThrow(() -> new CustomException(ErrorCode.FEED_NOT_FOUND));

        return buildFeedDetailResponse(feed, userId);
    }

    /**
     * 피드 수정
     *
     * @param userId  현재 사용자 ID
     * @param feedId  수정할 피드 ID
     * @param request 피드 수정 요청
     * @return 수정된 피드 상세 정보
     */
    @Transactional
    public FeedDetailResponse updateFeed(Long userId, Long feedId, FeedUpdateRequest request) {
        Feed feed = feedRepository.findById(feedId).orElseThrow(() -> new CustomException(ErrorCode.FEED_NOT_FOUND));

        if (!feed.getUser().getId().equals(userId)) {
            throw new CustomException(ErrorCode.FEED_EDIT_DENIED);
        }

        Optional.ofNullable(request.clothesIds()).ifPresent(clothesIds -> {
            feedClothesMappingRepository.deleteByFeedId(feedId);

            if (!clothesIds.isEmpty()) {
                List<Clothes> clothesList = clothesValidationService.getVerifiedClothes(userId, clothesIds);
                saveFeedClothesMappings(feed, clothesList);
            }
        });

        Optional.ofNullable(request.content()).ifPresent(feed::updateContent);

        return buildFeedDetailResponse(feed, userId);
    }

    /**
     * 피드 삭제
     *
     * @param userId 현재 사용자 ID
     * @param feedId 삭제할 피드 ID
     */
    @Transactional
    public void deleteFeed(Long userId, Long feedId) {
        Feed feed = feedRepository.findById(feedId).orElseThrow(() -> new CustomException(ErrorCode.FEED_NOT_FOUND));

        if (!feed.getUser().getId().equals(userId)) {
            throw new CustomException(ErrorCode.FEED_DELETE_DENIED);
        }

        feed.softDelete();
    }

    /**
     * 피드 좋아요
     *
     * @param userId 현재 사용자 ID
     * @param feedId 좋아요할 피드 ID
     * @return 좋아요 응답 (좋아요 개수, 좋아요 여부)
     */
    @Transactional
    public LikeResponse likeFeed(Long userId, Long feedId) {
        Feed feed = feedRepository.findById(feedId).orElseThrow(() -> new CustomException(ErrorCode.FEED_NOT_FOUND));
        User user = userRepository.findById(userId).orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        boolean alreadyLiked =
                feedLikeRepository.findByFeedIdAndUserId(feedId, userId).isPresent();

        if (!alreadyLiked) {
            feedLikeRepository.save(FeedLike.builder().feed(feed).user(user).build());
            feed.incrementLikeCount();
        }

        return LikeResponse.builder()
                .likeCount(feed.getLikeCount())
                .isLiked(true)
                .build();
    }

    /**
     * 피드 좋아요 취소
     *
     * @param userId 현재 사용자 ID
     * @param feedId 좋아요 취소할 피드 ID
     * @return 좋아요 응답 (좋아요 개수, 좋아요 여부)
     */
    @Transactional
    public LikeResponse unlikeFeed(Long userId, Long feedId) {
        Feed feed = feedRepository.findById(feedId).orElseThrow(() -> new CustomException(ErrorCode.FEED_NOT_FOUND));

        Optional<FeedLike> existingLike = feedLikeRepository.findByFeedIdAndUserId(feedId, userId);

        if (existingLike.isPresent()) {
            feedLikeRepository.delete(existingLike.get());
            feed.decrementLikeCount();
        }

        return LikeResponse.builder()
                .likeCount(feed.getLikeCount())
                .isLiked(false)
                .build();
    }

    /**
     * 피드 홈 목록 조회
     *
     * @param userId 현재 사용자 ID
     * @param after  커서 (이전 페이지 마지막 피드 ID)
     * @param limit  조회 개수
     * @return 피드 목록 및 페이지 정보
     */
    public PagedResponse<FeedListItem> getFeeds(Long userId, Long after, int limit) {
        Slice<Feed> feedSlice = feedRepository.findByCursor(after, PageRequest.of(0, limit));
        List<Feed> feeds = feedSlice.getContent();

        List<Long> feedIds = feeds.stream().map(Feed::getId).toList();

        Map<Long, FeedImage> primaryImageMap = feedImageRepository.findByFeedIdInAndPrimaryTrue(feedIds).stream()
                .collect(Collectors.toMap(fi -> fi.getFeed().getId(), Function.identity()));

        List<Long> userIds =
                feeds.stream().map(f -> f.getUser().getId()).distinct().toList();
        Map<Long, UserProfile> userProfileMap = userProfileRepository.findByUserIdIn(userIds).stream()
                .collect(Collectors.toMap(up -> up.getUser().getId(), Function.identity()));

        Set<Long> likedFeedIds = feedLikeRepository.findByFeedIdInAndUserId(feedIds, userId).stream()
                .map(fl -> fl.getFeed().getId())
                .collect(Collectors.toSet());

        List<FeedListItem> items = feeds.stream()
                .map(feed -> buildFeedListItem(feed, primaryImageMap, userProfileMap, likedFeedIds))
                .toList();

        Long nextCursor = feedSlice.hasNext() ? feeds.getLast().getId() : null;
        PageInfo pageInfo = new PageInfo(feedSlice.hasNext(), nextCursor);

        return new PagedResponse<>(items, pageInfo);
    }

    /**
     * 피드 목록 아이템 생성
     */
    private FeedListItem buildFeedListItem(
            Feed feed,
            Map<Long, FeedImage> primaryImageMap,
            Map<Long, UserProfile> userProfileMap,
            Set<Long> likedFeedIds) {

        String primaryImageUrl = Optional.ofNullable(primaryImageMap.get(feed.getId()))
                .map(fi -> mediaService
                        .getFileFullUrl(List.of(fi.getFile().getId()))
                        .getFirst())
                .orElseThrow(() -> new CustomException(ErrorCode.FILE_NOT_FOUND));

        UserProfile userProfile = Optional.ofNullable(
                        userProfileMap.get(feed.getUser().getId()))
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        String profileImageUrl = Optional.ofNullable(userProfile.getProfileFile())
                .map(pf -> mediaService.getFileFullUrl(List.of(pf.getId())).getFirst())
                .orElse(null);

        UserProfileDto userProfileDto =
                new UserProfileDto(feed.getUser().getId(), profileImageUrl, userProfile.getNickname());

        return FeedListItem.builder()
                .feedId(feed.getId())
                .primaryImageUrl(primaryImageUrl)
                .likeCount(feed.getLikeCount())
                .commentCount(feed.getCommentCount())
                .userProfile(userProfileDto)
                .isLiked(likedFeedIds.contains(feed.getId()))
                .build();
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
        boolean isLiked = feedLikeRepository
                .findByFeedIdAndUserId(feed.getId(), currentUserId)
                .isPresent();

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
                .isLiked(isLiked)
                .isOwner(isOwner)
                .build();
    }
}
