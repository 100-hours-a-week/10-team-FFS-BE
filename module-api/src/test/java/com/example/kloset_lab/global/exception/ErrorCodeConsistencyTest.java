package com.example.kloset_lab.global.exception;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * ErrorCode.Code 상수와 ErrorCode enum 간의 일관성을 검증하는 테스트
 */
class ErrorCodeConsistencyTest {

    @Test
    @DisplayName("ErrorCode.Code 상수값이 해당 ErrorCode의 message와 일치해야 함")
    void checkErrorCodeConsistency() throws IllegalAccessException {
        Field[] fields = ErrorCode.Code.class.getDeclaredFields();

        for (Field field : fields) {
            if (Modifier.isStatic(field.getModifiers()) && field.getType().equals(String.class)) {
                String constantName = field.getName();
                String constantValue = (String) field.get(null);

                // 상수명과 동일한 ErrorCode enum이 존재해야 함
                ErrorCode errorCode = assertDoesNotThrow(
                        () -> ErrorCode.valueOf(constantName),
                        "ErrorCode.Code." + constantName + "에 대응하는 ErrorCode enum이 없습니다");

                // 상수값이 해당 ErrorCode의 message와 일치해야 함
                assertEquals(
                        errorCode.getMessage(),
                        constantValue,
                        "ErrorCode.Code."
                                + constantName
                                + "의 값이 ErrorCode."
                                + constantName
                                + ".getMessage()와 일치하지 않습니다");
            }
        }
    }
}
