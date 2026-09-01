import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter } from "react-router-dom";
import { AccountPage } from "./AccountPage";

const updateUserNameMock = vi.fn();
const changePasswordMock = vi.fn();

vi.mock("../../../app/providers/AuthProvider", () => ({
  useAuth: () => ({
    accessToken: "account-token",
    currentUser: {
      userId: 1,
      email: "user@example.com",
      userName: "旧名字",
      platformRole: "user",
      accountTier: "basic",
      accountTierExpiresAt: null,
      status: "active"
    },
    updateUserName: updateUserNameMock
  })
}));

vi.mock("../api/auth", () => ({
  changePassword: changePasswordMock
}));

describe("AccountPage", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("updates the username on submit", async () => {
    const user = userEvent.setup();
    updateUserNameMock.mockResolvedValue(undefined);

    render(
      <MemoryRouter>
        <AccountPage />
      </MemoryRouter>
    );

    const nameInput = screen.getByLabelText("新用户名");
    await user.clear(nameInput);
    await user.type(nameInput, "新名字");
    await user.click(screen.getByRole("button", { name: "保存用户名" }));

    await waitFor(() => {
      expect(updateUserNameMock).toHaveBeenCalledWith("新名字");
    });
    expect(screen.getByText("用户名已更新。")).toBeInTheDocument();
  });
});
