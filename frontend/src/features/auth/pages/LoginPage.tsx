import { useState, type FormEvent } from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import { useAuth } from "../../../app/providers/AuthProvider";

type LoginLocationState = {
  email?: string;
  message?: string;
};

export function LoginPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const { login } = useAuth();
  const locationState = (location.state as LoginLocationState | null) ?? null;
  const [email, setEmail] = useState(locationState?.email ?? "");
  const [password, setPassword] = useState("");
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [statusMessage] = useState<string | null>(locationState?.message ?? null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setErrorMessage(null);
    setIsSubmitting(true);

    try {
      await login({ email, password });
      navigate("/app");
    } catch (error) {
      setErrorMessage(error instanceof Error ? error.message : "登录失败，请稍后重试");
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <section className="grid gap-8 lg:grid-cols-[1.1fr_0.9fr]">
      <div className="rounded-[32px] border border-white/10 bg-white/5 p-8 shadow-[0_20px_80px_rgba(0,0,0,0.35)] backdrop-blur">
        <p className="text-sm uppercase tracking-[0.28em] text-amber-300">
          Welcome Back
        </p>
        <h1 className="mt-4 max-w-xl text-4xl font-semibold leading-tight text-white sm:text-5xl">
          登录 DailyForge，继续管理你的训练节奏。
        </h1>
        <p className="mt-4 max-w-2xl text-base leading-7 text-stone-300">
          当前前端已经接通真实鉴权链路。登录后你可以继续完成资料引导、邀请码兑换和后续训练相关功能。
        </p>
      </div>

      <div className="rounded-[32px] border border-amber-300/20 bg-stone-950/70 p-8 shadow-[0_24px_80px_rgba(0,0,0,0.45)]">
        <form className="space-y-5" onSubmit={handleSubmit}>
          {statusMessage ? (
            <div className="rounded-2xl border border-emerald-400/20 bg-emerald-400/10 px-4 py-3 text-sm text-emerald-200">
              {statusMessage}
            </div>
          ) : null}

          <div>
            <label className="mb-2 block text-sm text-stone-300" htmlFor="email">
              邮箱
            </label>
            <input
              id="email"
              type="email"
              required
              value={email}
              onChange={(event) => setEmail(event.target.value)}
              placeholder="user@example.com"
              className="w-full rounded-2xl border border-white/10 bg-white/5 px-4 py-3 text-white outline-none transition placeholder:text-stone-500 focus:border-amber-300"
            />
          </div>

          <div>
            <label className="mb-2 block text-sm text-stone-300" htmlFor="password">
              密码
            </label>
            <input
              id="password"
              type="password"
              required
              value={password}
              onChange={(event) => setPassword(event.target.value)}
              placeholder="请输入密码"
              className="w-full rounded-2xl border border-white/10 bg-white/5 px-4 py-3 text-white outline-none transition placeholder:text-stone-500 focus:border-amber-300"
            />
          </div>

          {errorMessage ? (
            <div className="rounded-2xl border border-rose-400/20 bg-rose-400/10 px-4 py-3 text-sm text-rose-200">
              {errorMessage}
            </div>
          ) : null}

          <button
            type="submit"
            disabled={isSubmitting}
            className="w-full rounded-2xl bg-amber-400 px-5 py-3 font-medium text-stone-950 transition hover:bg-amber-300 disabled:cursor-not-allowed disabled:opacity-60"
          >
            {isSubmitting ? "登录中..." : "登录"}
          </button>

          <p className="text-sm text-stone-400">
            还没有账号？{" "}
            <Link to="/register" className="text-amber-300 transition hover:text-amber-200">
              去注册
            </Link>
          </p>
        </form>
      </div>
    </section>
  );
}
