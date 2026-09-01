import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter } from "react-router-dom";
import { ForgotPasswordPage } from "./ForgotPasswordPage";

const { navigateMock, sendForgotPasswordCodeMock, resetPasswordMock } = vi.hoisted(() => ({
  navigateMock: vi.fn(),
  sendForgotPasswordCodeMock: vi.fn(),
  resetPasswordMock: vi.fn()
}));

vi.mock("react-router-dom", async () => {
  const actual = await vi.importActual<typeof import("react-router-dom")>(
    "react-router-dom"
  );
  return {
    ...actual,
    useNavigate: () => navigateMock
  };
});

vi.mock("../api/auth", () => ({
  sendForgotPasswordCode: sendForgotPasswordCodeMock,
  resetPassword: resetPasswordMock
}));

describe("ForgotPasswordPage", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("sends a code on step 1 and advances to step 2", async () => {
    const user = userEvent.setup();
    sendForgotPasswordCodeMock.mockResolvedValue(undefined);

    render(
      <MemoryRouter>
        <ForgotPasswordPage />
      </MemoryRouter>
    );

    await user.type(screen.getByLabelText("注册邮箱"), "user@example.com");
    await user.click(screen.getByRole("button", { name: "发送验证码" }));

    await waitFor(() => {
      expect(sendForgotPasswordCodeMock).toHaveBeenCalledWith({
        email: "user@example.com"
      });
    });
    expect(screen.getByLabelText("验证码")).toBeInTheDocument();
  });
});
