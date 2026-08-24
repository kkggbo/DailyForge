import { render, screen } from "@testing-library/react";
import { MetricEditor } from "./MetricEditor";
import type { EditorMetricForm } from "../types/cycle-template";

const rpeMetric: EditorMetricForm = {
  localId: "metric-1",
  sortOrder: 1,
  metricKey: "rpe",
  metricValueNumberText: "8"
};

describe("MetricEditor", () => {
  it("renders a disabled legacy option so a hidden metric like RPE stays visible", () => {
    render(
      <MetricEditor
        metric={rpeMetric}
        structureType="set_based"
        locked={false}
        onChange={vi.fn()}
        onRemove={vi.fn()}
        onMove={vi.fn()}
      />
    );

    const legacyOption = screen.getByRole("option", { name: "RPE（已停用）" });
    expect(legacyOption).toBeDisabled();
    expect(screen.getByRole("combobox")).toHaveValue("rpe");
  });
});
