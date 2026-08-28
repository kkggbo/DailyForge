package com.dailyforge.modules.stats.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class StatsFunCopyGeneratorTest {

    @Test
    void buildOverviewCopyShouldIncludeAllSegmentsAndEarthLaps() {
        String copy = StatsFunCopyGenerator.buildOverviewCopy(
                12,
                new BigDecimal("12345.5"),
                new BigDecimal("80150"));
        assertThat(copy).contains("累计训练 12 场");
        assertThat(copy).contains("总容量 12345.5kg");
        assertThat(copy).contains("总里程 80150km");
        assertThat(copy).contains("总里程相当于绕地球 2 圈。");
    }

    @Test
    void buildOverviewCopyShouldOmitZeroOrMissingSegments() {
        String copy = StatsFunCopyGenerator.buildOverviewCopy(3, new BigDecimal("500"), null);
        assertThat(copy).contains("累计训练 3 场");
        assertThat(copy).contains("总容量 500kg");
        assertThat(copy).doesNotContain("总里程");
        assertThat(copy).doesNotContain("绕地球");
    }

    @Test
    void buildOverviewCopyShouldOmitZeroVolumeAndDistance() {
        String copy = StatsFunCopyGenerator.buildOverviewCopy(3, BigDecimal.ZERO, BigDecimal.ZERO);
        assertThat(copy).contains("累计训练 3 场");
        assertThat(copy).doesNotContain("总容量");
        assertThat(copy).doesNotContain("总里程");
        assertThat(copy).doesNotContain("绕地球");
    }

    @Test
    void strengthCopyShouldUseAdultBracketBelow2000kg() {
        String copy = StatsFunCopyGenerator.buildExerciseFunCopy(
                "引体向上", true, 300, new BigDecimal("1500"), null);
        assertThat(copy).contains("引体向上 300 次");
        assertThat(copy).contains("总容量 1500kg");
        assertThat(copy).contains("相当于 21.43 个成年男子");
    }

    @Test
    void strengthCopyShouldUseCarBracketBetween2000And20000() {
        String copy = StatsFunCopyGenerator.buildExerciseFunCopy(
                "卧推", true, 150, new BigDecimal("8000"), null);
        assertThat(copy).contains("卧推 150 次");
        assertThat(copy).contains("总容量 8000kg");
        assertThat(copy).contains("相当于 5.33 辆小汽车");
    }

    @Test
    void strengthCopyShouldUseElephantBracketBetween20000And100000() {
        String copy = StatsFunCopyGenerator.buildExerciseFunCopy(
                "深蹲", true, 200, new BigDecimal("50000"), null);
        assertThat(copy).contains("深蹲 200 次");
        assertThat(copy).contains("总容量 50000kg");
        assertThat(copy).contains("相当于 10 头成年大象");
    }

    @Test
    void strengthCopyShouldUseBlueWhaleBracketAbove100000() {
        String copy = StatsFunCopyGenerator.buildExerciseFunCopy(
                "硬拉", true, 300, new BigDecimal("150000"), null);
        assertThat(copy).contains("硬拉 300 次");
        assertThat(copy).contains("总容量 150000kg");
        assertThat(copy).contains("相当于 1 头蓝鲸");
    }

    @Test
    void strengthCopyShouldBeEmptyWhenNoRepsAndNoVolume() {
        String copy = StatsFunCopyGenerator.buildExerciseFunCopy("俯卧撑", true, 0, null, null);
        assertThat(copy).isEmpty();
    }

    @Test
    void strengthCopyShouldWriteRepsOnlyWhenVolumeMissing() {
        // Body-weight exercise: reps exist but no volume -> reps only, no capacity / equivalence.
        String copy = StatsFunCopyGenerator.buildExerciseFunCopy("俯卧撑", true, 100, null, null);
        assertThat(copy).contains("俯卧撑 100 次");
        assertThat(copy).doesNotContain("总容量");
        assertThat(copy).doesNotContain("成年男子");
        assertThat(copy).doesNotContain("小汽车");
        assertThat(copy).doesNotContain("成年大象");
        assertThat(copy).doesNotContain("蓝鲸");
    }

    @Test
    void cardioCopyShouldUseTrackLapBracketBelowMarathon() {
        String copy = StatsFunCopyGenerator.buildExerciseFunCopy(
                "跑步", false, 0, null, new BigDecimal("10"));
        assertThat(copy).contains("跑步 10km");
        assertThat(copy).contains("绕标准田径场 25 圈");
    }

    @Test
    void cardioCopyShouldUseMarathonBracketBetweenMarathonAndEarth() {
        String copy = StatsFunCopyGenerator.buildExerciseFunCopy(
                "跑步", false, 0, null, new BigDecimal("100"));
        assertThat(copy).contains("跑步 100km");
        assertThat(copy).contains("相当于 2.37 趟马拉松");
    }

    @Test
    void cardioCopyShouldUseEarthBracketAboveEarthCircumference() {
        String copy = StatsFunCopyGenerator.buildExerciseFunCopy(
                "跑步", false, 0, null, new BigDecimal("40150"));
        assertThat(copy).contains("跑步 40150km");
        assertThat(copy).contains("绕地球 1 圈");
    }

    @Test
    void cardioCopyShouldBeEmptyWhenNoDistance() {
        assertThat(StatsFunCopyGenerator.buildExerciseFunCopy("跑步", false, 0, null, null))
                .isEmpty();
        assertThat(StatsFunCopyGenerator.buildExerciseFunCopy("跑步", false, 0, null, BigDecimal.ZERO))
                .isEmpty();
    }
}
