package com.dailyforge.modules.profile.domain.service;

import com.dailyforge.modules.profile.infrastructure.persistence.entity.UserCurrentBodyMetricsEntity;
import com.dailyforge.modules.profile.infrastructure.persistence.entity.UserProfileEntity;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ProfileCompletionDomainService {

    /**
     * Calculate profile completion and AI readiness flags.
     */
    public CompletionSummary summarize(UserProfileEntity profile, UserCurrentBodyMetricsEntity snapshot) {
        List<String> missingBasicProfileFields = new ArrayList<>();
        if (profile.getGender() == null) {
            missingBasicProfileFields.add("gender");
        }
        if (profile.getBirthDate() == null) {
            missingBasicProfileFields.add("birthDate");
        }
        if (profile.getHeightCm() == null) {
            missingBasicProfileFields.add("heightCm");
        }
        if (profile.getGoalType() == null) {
            missingBasicProfileFields.add("goalType");
        }

        BigDecimal currentWeightKg = snapshot == null ? null : snapshot.getCurrentWeightKg();
        boolean hasWeightRecord = currentWeightKg != null;
        boolean basicProfileReady = missingBasicProfileFields.isEmpty();

        List<String> aiMissingFields = new ArrayList<>(missingBasicProfileFields);
        if (!hasWeightRecord) {
            aiMissingFields.add("weightRecord");
        }
        boolean aiReady = aiMissingFields.isEmpty();

        return new CompletionSummary(
                basicProfileReady,
                hasWeightRecord,
                currentWeightKg,
                List.copyOf(missingBasicProfileFields),
                aiReady,
                List.copyOf(aiMissingFields),
                aiReady,
                List.copyOf(aiMissingFields),
                aiReady,
                List.copyOf(aiMissingFields));
    }

    public record CompletionSummary(
            boolean basicProfileReady,
            boolean hasWeightRecord,
            BigDecimal currentWeightKg,
            List<String> missingBasicProfileFields,
            boolean aiPlanReady,
            List<String> aiPlanMissingFields,
            boolean aiNutritionReady,
            List<String> aiNutritionMissingFields,
            boolean aiSummaryReady,
            List<String> aiSummaryMissingFields) {
    }
}
