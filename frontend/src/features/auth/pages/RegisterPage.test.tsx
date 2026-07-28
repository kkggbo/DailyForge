import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter } from "react-router-dom";
import { RegisterPage } from "./RegisterPage";

const navigateMock = vi.fn();
const loginMock = vi.fn();
const registerMock = vi.fn();

vi.mock("react-router-dom", async () => {
  const actual = await vi.importActual<typeof import("react-router-dom")>("react-router-dom");
  return {
    ...actual,
    useNavigate: () => navigateMock
  };
});

vi.mock("../../../app/providers/AuthProvider", () => ({
  useAuth: () => ({
    login: loginMock,
    register: registerMock
  })
}));

describe("RegisterPage", () => {
  it("blocks submission when password confirmation does not match", async () => {
    const user = userEvent.setup();
    const { container } = render(
      <MemoryRouter>
        <RegisterPage />
      </MemoryRouter>
    );

    const inputs = container.querySelectorAll("input");
    await user.type(inputs[0] as HTMLInputElement, "user@example.com");
    await user.type(inputs[1] as HTMLInputElement, "dailyforge");
    await user.type(inputs[2] as HTMLInputElement, "Password1!");
    await user.type(inputs[3] as HTMLInputElement, "Password2!");
    await user.click(screen.getByRole("button"));

    expect(screen.getByText(/两次输入的密码不一致/)).toBeInTheDocument();
    expect(registerMock).not.toHaveBeenCalled();
    expect(loginMock).not.toHaveBeenCalled();
    expect(navigateMock).not.toHaveBeenCalled();
  });
});
