import { formatMetricValue } from "./cycle-template-formatters";
import type {
  CycleTemplateMetricResponse,
  MetricKey
} from "../types/cycle-template";

describe("formatMetricValue", () => {
  it("prefers the Chinese meta unit over the backend English unit", () => {
    const metric: CycleTemplateMetricResponse = {
      sortOrder: 1,
      metricKey: "duration_seconds",
      metricValueNumber: 60,
      metricUnit: "seconds"
    };

    expect(formatMetricValue(metric)).toBe("60 秒");
  });

  it("formats duration_minutes with the Chinese minutes unit", () => {
    const metric: CycleTemplateMetricResponse = {
      sortOrder: 1,
      metricKey: "duration_minutes",
      metricValueNumber: 30,
      metricUnit: "minutes"
    };

    expect(formatMetricValue(metric)).toBe("30 分钟");
  });

  it("falls back to the backend unit when the key has no meta", () => {
    const metric = {
      sortOrder: 1,
      metricKey: "future_key" as MetricKey,
      metricValueNumber: 10,
      metricUnit: "km"
    } as CycleTemplateMetricResponse;

    expect(formatMetricValue(metric)).toBe("10 km");
  });
});
