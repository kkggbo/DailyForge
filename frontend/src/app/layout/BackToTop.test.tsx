import { fireEvent, render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { BackToTop } from "./BackToTop";

describe("BackToTop", () => {
  beforeEach(() => {
    Object.defineProperty(window, "scrollY", {
      value: 0,
      writable: true,
      configurable: true
    });
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it("stays hidden until scrolled past the threshold and scrolls to top on click", async () => {
    const scrollTo = vi.spyOn(window, "scrollTo").mockImplementation(() => {});
    const user = userEvent.setup();

    render(<BackToTop />);
    expect(screen.queryByRole("button", { name: "回到顶部" })).not.toBeInTheDocument();

    Object.defineProperty(window, "scrollY", {
      value: 600,
      writable: true,
      configurable: true
    });
    fireEvent.scroll(window);

    expect(screen.getByRole("button", { name: "回到顶部" })).toBeInTheDocument();

    await user.click(screen.getByRole("button", { name: "回到顶部" }));
    expect(scrollTo).toHaveBeenCalledWith({ top: 0, behavior: "smooth" });
  });
});
