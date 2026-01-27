package com.example.kloset_lab.user.service;

import com.example.kloset_lab.global.response.Message;
import com.example.kloset_lab.user.dto.BirthDateValidationResult;
import com.example.kloset_lab.user.repository.UserProfileRepository;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 회원 프로필 유효성 검사 서비스
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserProfileValidationService {

    private static final int MIN_BIRTH_YEAR = 1900;

    private final UserProfileRepository userProfileRepository;

    /**
     * 닉네임 중복 여부 확인
     *
     * @param nickname 검사할 닉네임
     * @return 사용 가능 여부 (true: 사용 가능, false: 중복)
     */
    public boolean isNicknameAvailable(String nickname) {
        return !userProfileRepository.existsByNickname(nickname);
    }

    /**
     * 생년월일 유효성 검사
     *
     * @param birthDateString 검사할 생년월일 문자열 (yyyy-MM-dd)
     * @return 유효 여부와 메시지를 담은 결과 객체
     */
    public BirthDateValidationResult validateBirthDate(String birthDateString) {
        LocalDate birthDate;
        try {
            birthDate = LocalDate.parse(birthDateString);
        } catch (DateTimeParseException e) {
            return new BirthDateValidationResult(false, Message.BIRTH_DATE_INVALID_FORMAT);
        }

        LocalDate today = LocalDate.now();

        if (birthDate.isAfter(today)) {
            return new BirthDateValidationResult(false, Message.BIRTH_DATE_FUTURE);
        }

        if (birthDate.getYear() < MIN_BIRTH_YEAR) {
            return new BirthDateValidationResult(false, Message.BIRTH_DATE_TOO_OLD);
        }

        return new BirthDateValidationResult(true, Message.BIRTH_DATE_VALID);
    }
}
