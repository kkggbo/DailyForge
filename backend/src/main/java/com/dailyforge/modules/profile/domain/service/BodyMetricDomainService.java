package com.dailyforge.modules.profile.domain.service;

import com.dailyforge.modules.profile.infrastructure.persistence.entity.BodyMetricLogEntity;
import com.dailyforge.modules.profile.infrastructure.persistence.entity.UserCurrentBodyMetricsEntity;
import com.dailyforge.modules.profile.interfaces.dto.CreateBodyMetricRequest;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class BodyMetricDomainService {

    private static final Logger log = LoggerFactory.getLogger(BodyMetricDomainService.class);

    /**
     * Collect effective metric field names that are present in the request.
     */
    public List<String> collectEffectiveMetricFields(CreateBodyMetricRequest request) {
        List<String> fields = new ArrayList<>();
        addIfPresent(fields, "weightKg", request.weightKg());
        addIfPresent(fields, "bodyFatPercent", request.bodyFatPercent());
        addIfPresent(fields, "bmi", request.bmi());
        addIfPresent(fields, "skeletalMusclePercent", request.skeletalMusclePercent());
        addIfPresent(fields, "bodyWaterPercent", request.bodyWaterPercent());
        addIfPresent(fields, "basalMetabolicRateKcal", request.basalMetabolicRateKcal());
        addIfPresent(fields, "waistCm", request.waistCm());
        addIfPresent(fields, "hipCm", request.hipCm());
        addIfPresent(fields, "waistHipRatio", request.waistHipRatio());
        addIfPresent(fields, "bodyAge", request.bodyAge());
        addIfPresent(fields, "bodyType", request.bodyType());
        log.debug("Collected effective body metric fields. fields={}", fields);
        return fields;
    }

    /**
     * Merge current request values into an existing snapshot.
     */
    public UserCurrentBodyMetricsEntity mergeIntoSnapshot(
            Long userId,
            UserCurrentBodyMetricsEntity existingSnapshot,
            CreateBodyMetricRequest request) {
        UserCurrentBodyMetricsEntity snapshot = existingSnapshot == null
                ? new UserCurrentBodyMetricsEntity()
                : existingSnapshot;
        snapshot.setUserId(userId);
        if (request.weightKg() != null) {
            snapshot.setCurrentWeightKg(request.weightKg());
        }
        if (request.bodyFatPercent() != null) {
            snapshot.setCurrentBodyFatPercent(request.bodyFatPercent());
        }
        if (request.bmi() != null) {
            snapshot.setCurrentBmi(request.bmi());
        }
        if (request.skeletalMusclePercent() != null) {
            snapshot.setCurrentSkeletalMusclePercent(request.skeletalMusclePercent());
        }
        if (request.bodyWaterPercent() != null) {
            snapshot.setCurrentBodyWaterPercent(request.bodyWaterPercent());
        }
        if (request.basalMetabolicRateKcal() != null) {
            snapshot.setCurrentBasalMetabolicRateKcal(request.basalMetabolicRateKcal());
        }
        if (request.waistCm() != null) {
            snapshot.setCurrentWaistCm(request.waistCm());
        }
        if (request.hipCm() != null) {
            snapshot.setCurrentHipCm(request.hipCm());
        }
        if (request.waistHipRatio() != null) {
            snapshot.setCurrentWaistHipRatio(request.waistHipRatio());
        }
        if (request.bodyAge() != null) {
            snapshot.setCurrentBodyAge(request.bodyAge());
        }
        if (request.bodyType() != null) {
            snapshot.setCurrentBodyType(request.bodyType());
        }
        return snapshot;
    }

    /**
     * Rebuild the snapshot from active history rows ordered by latest first.
     */
    public UserCurrentBodyMetricsEntity rebuildSnapshot(Long userId, List<BodyMetricLogEntity> activeRecords) {
        if (activeRecords == null || activeRecords.isEmpty()) {
            log.debug("Rebuild snapshot skipped because no active records exist. userId={}", userId);
            return null;
        }

        UserCurrentBodyMetricsEntity snapshot = new UserCurrentBodyMetricsEntity();
        snapshot.setUserId(userId);
        List<String> sources = new ArrayList<>();
        for (BodyMetricLogEntity record : activeRecords) {
            if (snapshot.getCurrentWeightKg() == null && record.getWeightKg() != null) {
                snapshot.setCurrentWeightKg(record.getWeightKg());
                sources.add(source("currentWeightKg", record));
            }
            if (snapshot.getCurrentBodyFatPercent() == null && record.getBodyFatPercent() != null) {
                snapshot.setCurrentBodyFatPercent(record.getBodyFatPercent());
                sources.add(source("currentBodyFatPercent", record));
            }
            if (snapshot.getCurrentBmi() == null && record.getBmi() != null) {
                snapshot.setCurrentBmi(record.getBmi());
                sources.add(source("currentBmi", record));
            }
            if (snapshot.getCurrentSkeletalMusclePercent() == null && record.getSkeletalMusclePercent() != null) {
                snapshot.setCurrentSkeletalMusclePercent(record.getSkeletalMusclePercent());
                sources.add(source("currentSkeletalMusclePercent", record));
            }
            if (snapshot.getCurrentBodyWaterPercent() == null && record.getBodyWaterPercent() != null) {
                snapshot.setCurrentBodyWaterPercent(record.getBodyWaterPercent());
                sources.add(source("currentBodyWaterPercent", record));
            }
            if (snapshot.getCurrentBasalMetabolicRateKcal() == null && record.getBasalMetabolicRateKcal() != null) {
                snapshot.setCurrentBasalMetabolicRateKcal(record.getBasalMetabolicRateKcal());
                sources.add(source("currentBasalMetabolicRateKcal", record));
            }
            if (snapshot.getCurrentWaistCm() == null && record.getWaistCm() != null) {
                snapshot.setCurrentWaistCm(record.getWaistCm());
                sources.add(source("currentWaistCm", record));
            }
            if (snapshot.getCurrentHipCm() == null && record.getHipCm() != null) {
                snapshot.setCurrentHipCm(record.getHipCm());
                sources.add(source("currentHipCm", record));
            }
            if (snapshot.getCurrentWaistHipRatio() == null && record.getWaistHipRatio() != null) {
                snapshot.setCurrentWaistHipRatio(record.getWaistHipRatio());
                sources.add(source("currentWaistHipRatio", record));
            }
            if (snapshot.getCurrentBodyAge() == null && record.getBodyAge() != null) {
                snapshot.setCurrentBodyAge(record.getBodyAge());
                sources.add(source("currentBodyAge", record));
            }
            if (snapshot.getCurrentBodyType() == null && record.getBodyType() != null) {
                snapshot.setCurrentBodyType(record.getBodyType());
                sources.add(source("currentBodyType", record));
            }
        }
        log.debug("Rebuilt body metric snapshot. userId={}, sources={}", userId, sources);
        return snapshot;
    }

    private void addIfPresent(List<String> fields, String fieldName, Object value) {
        if (value != null) {
            fields.add(fieldName);
        }
    }

    private String source(String fieldName, BodyMetricLogEntity record) {
        return fieldName + "@recordId=" + record.getId() + ",date=" + record.getRecordDate();
    }
}
