package com.example.kloset_lab.clothes.service;

import com.example.kloset_lab.clothes.entity.Clothes;
import com.example.kloset_lab.clothes.repository.ClothesRepository;
import com.example.kloset_lab.global.exception.CustomException;
import com.example.kloset_lab.global.exception.ErrorCode;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClothesValidationService {

    private final ClothesRepository clothesRepository;

    /**
     * 옷 검증 및 조회 (존재 + 소유권)
     *
     * @param userId 사용자 ID
     * @param clothesIds 옷 ID 목록
     * @return 검증된 옷 목록
     */
    public List<Clothes> getVerifiedClothes(Long userId, List<Long> clothesIds) {
        return Optional.ofNullable(clothesIds)
                .filter(ids -> !ids.isEmpty())
                .map(ids -> {
                    List<Clothes> clothesList = findAllByIdsOrThrow(ids);
                    validateOwnership(userId, clothesList);
                    return clothesList;
                })
                .orElse(List.of());
    }

    /**
     * 옷 조회 및 존재 검증
     *
     * @param clothesIds 옷 ID 목록
     * @return 조회된 옷 목록
     */
    private List<Clothes> findAllByIdsOrThrow(List<Long> clothesIds) {
        List<Clothes> clothesList = clothesRepository.findAllById(clothesIds);

        if (clothesList.size() != clothesIds.size()) {
            throw new CustomException(ErrorCode.CLOTHES_NOT_FOUND);
        }

        return clothesList;
    }

    /**
     * 옷 소유권 검증
     *
     * @param userId 사용자 ID
     * @param clothesList 검증할 옷 목록
     */
    private void validateOwnership(Long userId, List<Clothes> clothesList) {
        clothesList.stream()
                .filter(clothes -> !clothes.getUser().getId().equals(userId))
                .findFirst()
                .ifPresent(clothes -> {
                    throw new CustomException(ErrorCode.CLOTHES_ACCESS_DENIED);
                });
    }
}
