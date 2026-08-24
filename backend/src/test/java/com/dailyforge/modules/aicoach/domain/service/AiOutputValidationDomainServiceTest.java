package com.dailyforge.modules.aicoach.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.dailyforge.common.BusinessException;
import com.dailyforge.common.ErrorCode;
import com.dailyforge.modules.aicoach.interfaces.dto.TemplateGenerationRequest;
import com.dailyforge.modules.exercise.application.model.SystemExerciseLookupResult;
import com.dailyforge.modules.exercise.application.service.SystemExerciseLookupService;
import com.dailyforge.modules.plan.domain.service.CycleTemplatePolicyService;
import com.dailyforge.modules.plan.domain.service.ExerciseStructurePolicyService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AiOutputValidationDomainServiceTest {

    private static final long SINGLE_SEGMENT_EXERCISE_ID = 1001L;

    @Mock
    private SystemExerciseLookupService systemExerciseLookupService;
    @Mock
    private CycleTemplatePolicyService cycleTemplatePolicyService;
    @Mock
    private ExerciseStructurePolicyService exerciseStructurePolicyService;

    private AiOutputValidationDomainService service;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        service = new AiOutputValidationDomainService(
                objectMapper,
                systemExerciseLookupService,
                cycleTemplatePolicyService,
                exerciseStructurePolicyService);
        when(systemExerciseLookupService.loadActiveSystemExercisesByIds(any()))
                .thenReturn(Map.of(SINGLE_SEGMENT_EXERCISE_ID, new SystemExerciseLookupResult(
                        SINGLE_SEGMENT_EXERCISE_ID,
                        null,
                        "椭圆机",
                        "cardio",
                        "push",
                        "minutes",
                        "single_segment",
                        1)));
    }

    @Test
    void validateTemplateGenerationShouldRejectRpeMetricOnSingleSegmentExercise() {
        // Given
        TemplateGenerationRequest request =
                new TemplateGenerationRequest("req-1", "gym", "muscle_gain", 4, true, null);

        // When / Then
        assertThatThrownBy(() -> service.validateTemplateGeneration(singleSegmentJson("rpe"), request))
                .isInstanceOf(BusinessException.class)
                .satisfies(error -> {
                    BusinessException businessException = (BusinessException) error;
                    assertThat(businessException.getErrorCode()).isEqualTo(ErrorCode.AI_OUTPUT_INVALID);
                    assertThat(businessException.getMessage()).contains("rpe");
                    assertThat(businessException.getMessage()).contains("single_segment");
                });
    }

    @Test
    void validateTemplateGenerationShouldAcceptDurationSecondsMetricOnSingleSegmentExercise() {
        // Given
        TemplateGenerationRequest request =
                new TemplateGenerationRequest("req-1", "gym", "muscle_gain", 4, true, null);

        // When / Then
        assertThatCode(() -> service.validateTemplateGeneration(singleSegmentJson("duration_seconds"), request))
                .doesNotThrowAnyException();
    }

    private String singleSegmentJson(String metricKey) {
        return """
                {
                  "templateName": "测试模板",
                  "cycleLength": 4,
                  "days": [
                    {
                      "dayIndex": 1,
                      "dayName": "Day 1",
                      "exercises": [
                        {
                          "sortOrder": 1,
                          "exerciseId": 1001,
                          "structureType": "single_segment",
                          "note": null,
                          "items": [
                            {
                              "itemIndex": 1,
                              "itemType": "segment",
                              "itemName": "段 1",
                              "note": null,
                              "metrics": [
                                {
                                  "sortOrder": 1,
                                  "metricKey": "%s",
                                  "metricValueNumber": 30
                                }
                              ]
                            }
                          ]
                        }
                      ]
                    }
                  ],
                  "generationRationale": {
                    "overallDesignSummary": "整体设计",
                    "dayRationales": [],
                    "keyExerciseRationales": [],
                    "intensityRationale": {
                      "basisType": "starting_recommendation",
                      "summary": "起始建议"
                    },
                    "warnings": []
                  }
                }
                """.formatted(metricKey);
    }
}
