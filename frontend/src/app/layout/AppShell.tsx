import { Link, NavLink, Outlet } from "react-router-dom";
import { useAuth } from "../providers/AuthProvider";

const navLinkClass = ({ isActive }: { isActive: boolean }) =>
  [
    "rounded-full px-4 py-2 text-sm transition",
    isActive
      ? "bg-amber-400 text-stone-950"
      : "text-stone-200 hover:bg-white/10 hover:text-white"
  ].join(" ");

export function AppShell() {
  const { currentUser, isAuthenticated, logout } = useAuth();

  return (
    <div className="relative min-h-screen overflow-hidden bg-[radial-gradient(circle_at_top,_rgba(251,191,36,0.24),_transparent_36%),linear-gradient(160deg,_#17120f_0%,_#231814_50%,_#0f0a09_100%)] text-white">
      <div className="pointer-events-none absolute inset-0 bg-[linear-gradient(rgba(255,255,255,0.03)_1px,transparent_1px),linear-gradient(90deg,rgba(255,255,255,0.03)_1px,transparent_1px)] bg-[size:32px_32px] opacity-25" />
      <div className="relative mx-auto flex min-h-screen max-w-7xl flex-col px-6 pb-10 pt-6 sm:px-8 lg:px-10">
        <header className="flex flex-col gap-4 rounded-[28px] border border-white/10 bg-black/20 px-5 py-4 backdrop-blur md:flex-row md:items-center md:justify-between">
          <div>
            <Link to="/" className="inline-flex items-center gap-3">
              <span className="flex h-11 w-11 items-center justify-center rounded-2xl bg-amber-400 font-semibold text-stone-950 shadow-[0_0_30px_rgba(251,191,36,0.3)]">
                DF
              </span>
              <div>
                <p className="text-lg font-semibold tracking-[0.18em] text-amber-300 uppercase">
                  DailyForge
                </p>
                <p className="text-sm text-stone-400">
                  为训练节奏、饮食决策与成长记录搭一条清晰主线
                </p>
              </div>
            </Link>
          </div>

          <div className="flex flex-col gap-3 md:flex-row md:items-center">
            <nav className="flex flex-wrap items-center gap-2">
              <NavLink to="/" className={navLinkClass}>
                首页
              </NavLink>
              {isAuthenticated ? (
                <>
                  <NavLink to="/app" className={navLinkClass}>
                    控制台
                  </NavLink>
                  <NavLink to="/profile" className={navLinkClass}>
                    个人资料
                  </NavLink>
                  <NavLink to="/invite-code" className={navLinkClass}>
                    邀请码
                  </NavLink>
                </>
              ) : (
                <>
                  <NavLink to="/login" className={navLinkClass}>
                    登录
                  </NavLink>
                  <NavLink to="/register" className={navLinkClass}>
                    注册
                  </NavLink>
                </>
              )}
            </nav>

            <div className="flex items-center gap-3 rounded-full border border-white/10 bg-white/6 px-4 py-2">
              <div className="min-w-0">
                <p className="truncate text-sm font-medium text-stone-100">
                  {currentUser?.userName ?? "未登录"}
                </p>
                <p className="truncate text-xs text-stone-400">
                  {currentUser?.accountTier ?? "guest"}
                </p>
              </div>
              {isAuthenticated ? (
                <button
                  type="button"
                  onClick={() => {
                    void logout();
                  }}
                  className="rounded-full border border-white/10 px-3 py-1.5 text-xs text-stone-200 transition hover:bg-white/10"
                >
                  退出
                </button>
              ) : null}
            </div>
          </div>
        </header>

        <main className="flex-1 pt-8">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
