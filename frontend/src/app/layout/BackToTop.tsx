import { useEffect, useState } from "react";

const SCROLL_THRESHOLD = 400;

export function BackToTop() {
  const [visible, setVisible] = useState(false);

  useEffect(() => {
    function handleScroll() {
      setVisible(window.scrollY > SCROLL_THRESHOLD);
    }

    handleScroll();
    window.addEventListener("scroll", handleScroll);
    return () => window.removeEventListener("scroll", handleScroll);
  }, []);

  if (!visible) {
    return null;
  }

  return (
    <button
      type="button"
      aria-label="回到顶部"
      onClick={() => window.scrollTo({ top: 0, behavior: "smooth" })}
      className="fixed bottom-6 z-40 flex h-12 w-12 items-center justify-center rounded-full bg-amber-400 text-xl font-semibold text-stone-950 shadow-2xl shadow-black/40 transition hover:bg-amber-300"
      style={{ right: "max(1rem, calc((100vw - 80rem) / 2 + 1rem))" }}
    >
      <span aria-hidden="true">↑</span>
    </button>
  );
}
