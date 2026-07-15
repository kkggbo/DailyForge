import type { MetricKey, StructureType } from "../types/cycle-template";

export type MetricMeta = {
  key: MetricKey;
  label: string;
  unitLabel: string | null;
  step: string;
  min?: number;
  max?: number;
  allowedStructureTypes: StructureType[];
};

export const metricMetas: MetricMeta[] = [
  {
    key: "weight_kg",
    label: "重量",
    unitLabel: "kg",
    step: "0.5",
    min: 0,
    max: 9999.99,
    allowedStructureTypes: ["set_based"]
  },
  {
    key: "reps",
    label: "次数",
    unitLabel: "次",
    step: "1",
    min: 1,
    max: 1000,
    allowedStructureTypes: ["set_based"]
  },
  {
    key: "duration_seconds",
    label: "时长",
    unitLabel: "秒",
    step: "1",
    min: 1,
    max: 86400,
    allowedStructureTypes: ["set_based", "single_segment"]
  },
  {
    key: "distance_km",
    label: "距离",
    unitLabel: "km",
    step: "0.1",
    min: 0,
    max: 9999.99,
    allowedStructureTypes: ["single_segment"]
  },
  {
    key: "speed_kmh",
    label: "速度",
    unitLabel: "km/h",
    step: "0.1",
    min: 0,
    max: 100,
    allowedStructureTypes: ["single_segment"]
  },
  {
    key: "pace_seconds_per_km",
    label: "配速",
    unitLabel: "秒/公里",
    step: "1",
    min: 1,
    max: 36000,
    allowedStructureTypes: ["single_segment"]
  },
  {
    key: "incline_percent",
    label: "坡度",
    unitLabel: "%",
    step: "0.5",
    min: 0,
    max: 100,
    allowedStructureTypes: ["single_segment"]
  },
  {
    key: "rest_seconds",
    label: "休息",
    unitLabel: "秒",
    step: "1",
    min: 0,
    max: 86400,
    allowedStructureTypes: ["set_based"]
  },
  {
    key: "rpe",
    label: "RPE",
    unitLabel: null,
    step: "0.5",
    min: 0,
    max: 10,
    allowedStructureTypes: ["set_based"]
  },
  {
    key: "intensity_level",
    label: "强度等级",
    unitLabel: null,
    step: "1",
    min: 1,
    max: 10,
    allowedStructureTypes: ["single_segment"]
  }
];

const metricMetaMap = new Map(metricMetas.map((meta) => [meta.key, meta]));

export function getMetricMeta(metricKey: MetricKey | "") {
  if (!metricKey) {
    return null;
  }

  return metricMetaMap.get(metricKey) ?? null;
}

export function getMetricOptionsByStructureType(structureType: StructureType | null) {
  if (!structureType) {
    return [];
  }

  return metricMetas.filter((meta) =>
    meta.allowedStructureTypes.includes(structureType)
  );
}
