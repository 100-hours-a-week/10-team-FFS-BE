package com.example.kloset_lab.user.controller;

import static com.example.kloset_lab.global.constants.PaginationDefaults.CLOTHES_LIST;
import static com.example.kloset_lab.global.constants.PaginationDefaults.FEED_LIST;

import com.example.kloset_lab.clothes.dto.ClothesListItem;
import com.example.kloset_lab.clothes.entity.Category;
import com.example.kloset_lab.clothes.service.ClothesService;
import com.example.kloset_lab.feed.dto.FeedListItem;
import com.example.kloset_lab.feed.service.FeedService;
import com.example.kloset_lab.global.response.ApiResponse;
import com.example.kloset_lab.global.response.ApiResponses;
import com.example.kloset_lab.global.response.Message;
import com.example.kloset_lab.global.response.PagedResponse;
import com.example.kloset_lab.user.dto.*;
import com.example.kloset_lab.user.service.UserLifecycleService;
import com.example.kloset_lab.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final UserLifecycleService userLifecycleService;
    private final FeedService feedService;
    private final ClothesService clothesService;

    /**
     * 회원가입 후 추가 정보 저장 API
     * POST /api/v1/users
     *
     * @param userId  인증된 회원 ID (임시 토큰에서 추출)
     * @param request 회원가입 요청 정보
     * @return 201 Created
     */
    @PostMapping ("/v1/users")
    public ResponseEntity<ApiResponse<Void>> registerUserProfile(
            @AuthenticationPrincipal Long userId, @RequestBody @Valid UserRegisterRequest request) {
        userService.registerUserProfile(userId, request);
        return ApiResponses.created(Message.USER_CREATED);
    }

    /**
     * 회원 탈퇴 API
     * DELETE /api/v1/users
     *
     * @param userId 인증된 회원 ID
     * @return 200 OK
     */
    @DeleteMapping("/v1/users")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@AuthenticationPrincipal Long userId) {
        userLifecycleService.deleteUser(userId);
        return ApiResponses.ok(Message.USER_DELETED);
    }

    /**
     * 닉네임 유효성 검사 API
     * GET /api/v1/users/validation/nickname?nickname={nickname}
     *
     * @param request 닉네임 검사 요청
     * @return 사용 가능 여부
     */
    @GetMapping("/v1/users/validation/nickname")
    public ResponseEntity<ApiResponse<NicknameValidationResponse>> validateNickname(
            @Valid NicknameValidationRequest request) {
        NicknameValidationResult result = userService.validateNicknameWithMessage(request.nickname());
        NicknameValidationResponse response = new NicknameValidationResponse(result.isAvailable());

        return ApiResponses.ok(result.message(), response);
    }

    /**
     * 생년월일 유효성 검사 API
     * GET /api/v1/users/validation/birth-date?birthDate={birthDate}
     *
     * @param request 생년월일 검사 요청 (yyyy-MM-dd 형식)
     * @return 유효 여부
     */
    @GetMapping("/v1/users/validation/birth-date")
    public ResponseEntity<ApiResponse<BirthDateValidationResponse>> validateBirthDate(
            @Valid BirthDateValidationRequest request) {
        BirthDateValidationResult result = userService.validateBirthDate(request.birthDate());
        BirthDateValidationResponse response = new BirthDateValidationResponse(result.isValid());

        return ApiResponses.ok(result.message(), response);
    }

    /**
     * 내 옷장 옷 개수 조회 API
     * GET /api/v1/users/me/clothes/count
     *
     * @param userId 현재 로그인한 사용자 ID
     * @return 옷 개수
     */
    @GetMapping("/v1/users/me/clothes/count")
    public ResponseEntity<ApiResponse<ClothesCountResponse>> getMyClothesCount(@AuthenticationPrincipal Long userId) {
        long count = clothesService.getClothesCount(userId);
        ClothesCountResponse response = new ClothesCountResponse(count);
        return ApiResponses.ok(Message.CLOTHES_COUNT_RETRIEVED, response);
    }

    /**
     * 특정 유저 프로필 조회 API
     * GET /api/v1/users/{userId}
     *
     * @param userId        조회할 유저 ID
     * @param currentUserId 현재 로그인한 유저 ID
     * @return 유저 프로필 정보
     */
    @GetMapping("/v1/users/{userId}")
    public ResponseEntity<ApiResponse<UserProfileInfoResponse>> getUserProfile(
            @PathVariable Long userId, @AuthenticationPrincipal Long currentUserId) {
        UserProfileInfoResponse response = userService.getUserProfile(userId, currentUserId);
        return ApiResponses.ok(Message.PROFILE_RETRIEVED, response);
    }

    /**
     * 특정 유저의 피드 목록 조회 API
     *
     * @param currentUserId 현재 로그인한 사용자 ID
     * @param userId        조회 대상 사용자 ID
     * @param after         커서 (이전 페이지 마지막 피드 ID)
     * @param limit         조회 개수
     * @return 피드 목록 및 페이지 정보
     */
    @GetMapping("/v1/users/{userId}/feeds")
    public ResponseEntity<ApiResponse<PagedResponse<FeedListItem>>> getUserFeeds(
            @AuthenticationPrincipal Long currentUserId,
            @PathVariable Long userId,
            @RequestParam(required = false) Long after,
            @RequestParam(defaultValue = FEED_LIST) int limit) {

        PagedResponse<FeedListItem> response = feedService.getFeedsByUserId(currentUserId, userId, after, limit);
        return ApiResponses.ok(Message.USER_FEEDS_RETRIEVED, response);
    }

    /**
     * 특정 유저의 옷장 조회 API
     *
     * @param userId 조회 대상 사용자 ID
     * @param after 커서 (이전 페이지 마지막 옷 ID)
     * @param limit 조회 개수
     * @param category 카테고리 필터 (옵션)
     * @return 옷 목록 및 페이지 정보
     */
    @GetMapping("/v1/users/{userId}/clothes")
    public ResponseEntity<ApiResponse<PagedResponse<ClothesListItem>>> getUserClothes(
            @PathVariable Long userId,
            @RequestParam(required = false) Long after,
            @RequestParam(defaultValue = CLOTHES_LIST) int limit,
            @RequestParam(required = false) Category category) {

        PagedResponse<ClothesListItem> response = clothesService.getClothes(userId, category, after, limit);
        return ApiResponses.ok(Message.CLOTHES_LIST_RETRIEVED, response);
    }
}
