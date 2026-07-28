import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { BodyMetricForm } from "./BodyMetricForm";

describe("BodyMetricForm", () => {
  it("requires at least one metric and submits the latest body metric payload", async () => {
    const user = userEvent.setup();
    const onSubmit = vi.fn().mockResolvedValue(undefined);
    const { container } = render(
      <BodyMetricForm
        submitLabel="Save Metric"
        submitSuccessMessage="Metric Saved"
        isSubmitting={false}
        onSubmit={onSubmit}
      />
    );

    await user.click(screen.getByRole("button", { name: "Save Metric" }));
    expect(screen.getByText(/至少填写一个身体指标/)).toBeInTheDocument();

    expect(container.querySelector("textarea")).not.toBeInTheDocument();

    const numberInputs = container.querySelectorAll('input[type="number"]');
    await user.type(numberInputs[0] as HTMLInputElement, "80.5");

    expect(container.querySelector("textarea")).toBeInTheDocument();

    await user.click(screen.getByRole("button", { name: "Save Metric" }));

    await waitFor(() => {
      expect(onSubmit).toHaveBeenCalledWith(
        expect.objectContaining({
          weightKg: 80.5,
          note: null,
          recordDate: expect.stringMatching(/^\d{4}-\d{2}-\d{2}$/)
        })
      );
    });

    expect(screen.getByText("Metric Saved")).toBeInTheDocument();
  });
});
