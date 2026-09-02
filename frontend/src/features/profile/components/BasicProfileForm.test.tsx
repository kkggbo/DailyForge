import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { BasicProfileForm } from "./BasicProfileForm";

describe("BasicProfileForm", () => {
  it("submits the filled basic profile payload", async () => {
    const user = userEvent.setup();
    const onSubmit = vi.fn().mockResolvedValue(undefined);
    const { container } = render(
      <BasicProfileForm
        submitLabel="Save Basic Profile"
        submitSuccessMessage="Saved"
        isSubmitting={false}
        onSubmit={onSubmit}
      />
    );

    const selects = container.querySelectorAll("select");
    const dateInput = container.querySelector('input[type="date"]');
    const heightInput = container.querySelector('input[type="number"]');
    const notesTextarea = container.querySelector("textarea");

    expect(selects).toHaveLength(4);
    expect(dateInput).not.toBeNull();
    expect(heightInput).not.toBeNull();
    expect(notesTextarea).not.toBeNull();

    await user.selectOptions(selects[0] as HTMLSelectElement, "male");
    fireEvent.change(dateInput as HTMLInputElement, {
      target: { value: "1998-08-01" }
    });
    await user.type(heightInput as HTMLInputElement, "178");
    await user.selectOptions(selects[1] as HTMLSelectElement, "muscle_gain");
    await user.selectOptions(selects[2] as HTMLSelectElement, "experienced");
    await user.selectOptions(selects[3] as HTMLSelectElement, "moderate");
    await user.type(notesTextarea as HTMLTextAreaElement, "Left shoulder discomfort");
    await user.click(screen.getByRole("button", { name: "Save Basic Profile" }));

    await waitFor(() => {
      expect(onSubmit).toHaveBeenCalledWith({
        gender: "male",
        birthDate: "1998-08-01",
        heightCm: 178,
        goalType: "muscle_gain",
        trainingLevel: "experienced",
        injuryNotes: "Left shoulder discomfort",
        activityLevel: "moderate"
      });
    });

    expect(screen.getByText("Saved")).toBeInTheDocument();
  });
});
