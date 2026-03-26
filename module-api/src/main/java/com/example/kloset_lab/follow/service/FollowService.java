package com.example.kloset_lab.follow.service;

import com.example.kloset_lab.follow.dto.FollowUserItem;
import com.example.kloset_lab.follow.entity.Follow;
import com.example.kloset_lab.follow.repository.FollowRepository;
import com.example.kloset_lab.global.exception.CustomException;
import com.example.kloset_lab.global.exception.ErrorCode;
import com.example.kloset_lab.global.response.PageInfo;
import com.example.kloset_lab.global.response.PagedResponse;
import com.example.kloset_lab.media.service.MediaService;
import com.example.kloset_lab.user.dto.UserProfileDto;
import com.example.kloset_lab.user.entity.User;
import com.example.kloset_lab.user.entity.UserProfile;
import com.example.kloset_lab.user.repository.UserProfileRepository;
import com.example.kloset_lab.user.repository.UserRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FollowService {

    private final FollowRepository followRepository;
    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final MediaService mediaService;

    /**
     * 팔로우
     */
    @Transactional
    public void follow(Long currentUserId, Long targetUserId) {
        if (currentUserId.equals(targetUserId)) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }

        User currentUser = findUserOrThrow(currentUserId, ErrorCode.USER_NOT_FOUND);
        User targetUser = findUserOrThrow(targetUserId, ErrorCode.TARGET_USER_NOT_FOUND);

        if (followRepository.existsByFollowerIdAndFolloweeId(currentUserId, targetUserId)) {
            throw new CustomException(ErrorCode.ALREADY_FOLLOWING);
        }

        Follow follow =
                Follow.builder().follower(currentUser).following(targetUser).build();
        followRepository.save(follow);

        UserProfile currentProfile = findProfileOrThrow(currentUserId);
        UserProfile targetProfile = findProfileOrThrow(targetUserId);

        currentProfile.incrementFollowingCount();
        targetProfile.incrementFollowerCount();
    }

    /**
     * 언팔로우
     */
    @Transactional
    public void unfollow(Long currentUserId, Long targetUserId) {
        User currentUser = findUserOrThrow(currentUserId, ErrorCode.USER_NOT_FOUND);
        User targetUser = findUserOrThrow(targetUserId, ErrorCode.TARGET_USER_NOT_FOUND);

        Follow follow = followRepository
                .findByFollowerAndFollowee(currentUser, targetUser)
                .orElseThrow(() -> new CustomException(ErrorCode.FOLLOW_NOT_FOUND));

        followRepository.delete(follow);

        UserProfile currentProfile = findProfileOrThrow(currentUserId);
        UserProfile targetProfile = findProfileOrThrow(targetUserId);

        currentProfile.decrementFollowingCount();
        targetProfile.decrementFollowerCount();
    }

    /**
     * 팔로잉 목록 조회 (내가 팔로우하는 사람들)
     */
    public PagedResponse<FollowUserItem> getFollowings(
            Long targetUserId, Long currentUserId, Long after, int limit, String sort) {
        User targetUser = findUserOrThrow(targetUserId, ErrorCode.TARGET_USER_NOT_FOUND);
        // User currentUser = findUserOrThrow(currentUserId, ErrorCode.USER_NOT_FOUND);

        boolean isDesc = "timeDesc".equals(sort);

        List<Follow> follows = isDesc
                ? followRepository.findFollowingsByUserDesc(targetUser, after)
                : followRepository.findFollowingsByUserAsc(targetUser, after);

        return buildListResponse(follows, limit, currentUserId, true);
    }

    /**
     * 팔로워 목록 조회 (나를 팔로우하는 사람들)
     */
    public PagedResponse<FollowUserItem> getFollowers(
            Long targetUserId, Long currentUserId, Long after, int limit, String sort) {
        User targetUser = findUserOrThrow(targetUserId, ErrorCode.TARGET_USER_NOT_FOUND);
        // User currentUser = findUserOrThrow(currentUserId, ErrorCode.USER_NOT_FOUND);

        boolean isDesc = "timeDesc".equals(sort);

        List<Follow> follows = isDesc
                ? followRepository.findFollowersByUserDesc(targetUser, after)
                : followRepository.findFollowersByUserAsc(targetUser, after);

        return buildListResponse(follows, limit, currentUserId, false);
    }

    private PagedResponse<FollowUserItem> buildListResponse(
            List<Follow> follows, int limit, Long currentUserId, boolean isFollowingList) {
        boolean hasNextPage = follows.size() > limit;
        List<Follow> pagedFollows = hasNextPage ? follows.subList(0, limit) : follows;

        List<FollowUserItem> items = pagedFollows.stream()
                .map(f -> {
                    User user = isFollowingList ? f.getFollowee() : f.getFollower();
                    return toFollowUserResponse(user.getId(), currentUserId);
                })
                .toList();

        Long nextCursor = null;
        if (hasNextPage) {
            Follow lastFollow = pagedFollows.getLast();
            nextCursor = lastFollow.getId();
        }
        PageInfo pageInfo = new PageInfo(hasNextPage, nextCursor);

        return new PagedResponse<>(items, pageInfo);
    }

    private User findUserOrThrow(Long userId, ErrorCode errorCode) {
        return userRepository.findById(userId).orElseThrow(() -> new CustomException(errorCode));
    }

    private UserProfile findProfileOrThrow(Long userId) {
        return userProfileRepository
                .findByUserId(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_PROFILE_NOT_FOUND));
    }

    private FollowUserItem toFollowUserResponse(Long userId, Long currentUserId) {
        UserProfile profile = findProfileOrThrow(userId);
        boolean isFollowing = followRepository.existsByFollowerIdAndFolloweeId(currentUserId, userId);

        return FollowUserItem.builder()
                .userProfile(new UserProfileDto(
                        userId,
                        profile.getProfileFile() != null
                                ? mediaService.getFileFullUrl(
                                        profile.getProfileFile().getId())
                                : null,
                        profile.getNickname()))
                .isFollowing(isFollowing)
                .build();
    }
}
