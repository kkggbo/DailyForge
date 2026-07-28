import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter } from "react-router-dom";
import { LoginPage } from "./LoginPage";

const navigateMock = vi.fn();
const loginMock = vi.fn();

vi.mock("react-router-dom", async () => {
  const actual = await vi.importActual<typeof import("react-router-dom")>("react-router-dom");
  return {
    ...actual,
    useNavigate: () => navigateMock,
    useLocation: () => ({ state: null })
  };
});

vi.mock("../../../app/providers/AuthProvider", () => ({
  useAuth: () => ({
    login: loginMock
  })
}));

describe("LoginPage", () => {
  it("submits login and surfaces server errors", async () => {
    const user = userEvent.setup();
    loginMock.mockRejectedValueOnce(new Error("Invalid credentials"));

    const { container } = render(
      <MemoryRouter>
        <LoginPage />
      </MemoryRouter>
    );

    await user.type(screen.getByPlaceholderText("user@example.com"), "user@example.com");
    const passwordInput = container.querySelector('input[type="password"]');
    expect(passwordInput).not.toBeNull();
    await user.type(passwordInput as HTMLInputElement, "wrong-password");
    await user.click(screen.getByRole("button"));

    await waitFor(() => {
      expect(loginMock).toHaveBeenCalledWith({
        email: "user@example.com",
        password: "wrong-password"
      });
    });

    expect(screen.getByText("Invalid credentials")).toBeInTheDocument();
    expect(navigateMock).not.toHaveBeenCalled();
  });
});
