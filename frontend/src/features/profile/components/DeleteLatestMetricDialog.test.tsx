import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { DeleteLatestMetricDialog } from "./DeleteLatestMetricDialog";

describe("DeleteLatestMetricDialog", () => {
  it("renders only when open and triggers confirm/cancel actions", async () => {
    const user = userEvent.setup();
    const onClose = vi.fn();
    const onConfirm = vi.fn();
    const { rerender } = render(
      <DeleteLatestMetricDialog
        open={false}
        isSubmitting={false}
        onClose={onClose}
        onConfirm={onConfirm}
      />
    );

    expect(screen.queryByText("Delete Latest Record")).not.toBeInTheDocument();

    rerender(
      <DeleteLatestMetricDialog
        open
        isSubmitting={false}
        onClose={onClose}
        onConfirm={onConfirm}
      />
    );

    expect(screen.getByText("Delete Latest Record")).toBeInTheDocument();

    const buttons = screen.getAllByRole("button");
    await user.click(buttons[0]!);
    await user.click(buttons[1]!);

    expect(onClose).toHaveBeenCalledTimes(1);
    expect(onConfirm).toHaveBeenCalledTimes(1);
  });
});
