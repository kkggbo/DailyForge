package com.dailyforge.modules.diet.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.dailyforge.common.BusinessException;
import com.dailyforge.common.ErrorCode;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class DietQueryServiceTest {

    @Test
    void resolveDateShouldReturnNowWhenBlank() {
        assertThatCode(() -> DietQueryService.resolveDate(null)).doesNotThrowAnyException();
        assertThatCode(() -> DietQueryService.resolveDate("  ")).doesNotThrowAnyException();
    }

    @Test
    void resolveDateShouldParseValidDate() {
        LocalDate parsed = DietQueryService.resolveDate("2026-09-03");
        assertThat(parsed).isEqualTo(LocalDate.of(2026, 9, 3));
    }

    @Test
    void resolveDateShouldRejectIllegalDate() {
        assertThatThrownBy(() -> DietQueryService.resolveDate("not-a-date"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_ARGUMENT);
        assertThatThrownBy(() -> DietQueryService.resolveDate("2026-13-40"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_ARGUMENT);
    }
}
