import {
  getMetricMeta,
  getMetricOptionsByStructureType
} from "./cycle-template-metric-config";

describe("cycle-template-metric-config", () => {
  it("still resolves RPE metadata for backward-compatible display and validation", () => {
    const meta = getMetricMeta("rpe");

    expect(meta).not.toBeNull();
    expect(meta?.label).toBe("RPE");
  });

  it("omits hidden metrics (RPE) from selectable options", () => {
    const options = getMetricOptionsByStructureType("set_based");

    expect(options.some((option) => option.key === "rpe")).toBe(false);
    expect(options.some((option) => option.key === "reps")).toBe(true);
  });
});
