import { useState, type FormEvent } from "react";
import { Link, useNavigate } from "react-router-dom";
import { useAuth } from "../../../app/providers/AuthProvider";

export function RegisterPage() {
  const navigate = useNavigate();
  const { register } = useAuth();
  const [form, setForm] = useState({
    email: "",
    userName: "",
    password: "",
    confirmPassword: "",
    inviteCode: ""
  });
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  function updateField<K extends keyof typeof form>(key: K, value: (typeof form)[K]) {
    setForm((previous) => ({
      ...previous,
      [key]: value
    }));
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();

    if (form.password !== form.confirmPassword) {
      setErrorMessage("两次输入的密码不一致");
      return;
    }

    setErrorMessage(null);
    setIsSubmitting(true);

    try {
      await register({
        email: form.email,
        userName: form.userName,
        password: form.password,
        confirmPassword: form.confirmPassword,
        inviteCode: form.inviteCode || undefined
      });
      navigate("/login");
    } catch (error) {
      setErrorMessage(error instanceof Error ? error.message : "注册失败，请稍后重试");
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <section className="grid gap-8 lg:grid-cols-[0.95fr_1.05fr]">
      <div className="rounded-[32px] border border-white/10 bg-white/5 p-8 backdrop-blur">
        <p className="text-sm uppercase tracking-[0.28em] text-amber-300">
          Create Account
        </p>
        <h1 className="mt-4 text-4xl font-semibold leading-tight text-white sm:text-5xl">
          先把账号链路打通，后面所有功能才有稳定落点。
        </h1>
        <div className="mt-6 space-y-4 text-stone-300">
          <p>
            这版注册表单已经对齐后端当前字段：`email`、`password`、`confirmPassword`、`userName`、`inviteCode`。
          </p>
          <p>
            如果邀请码先不填，也可以先注册普通账号；后续登录后再去邀请码页面兑换 AI 权限。
          </p>
        </div>
      </div>

      <div className="rounded-[32px] border border-white/10 bg-stone-950/70 p-8 shadow-[0_24px_80px_rgba(0,0,0,0.45)]">
        <form className="grid gap-5 sm:grid-cols-2" onSubmit={handleSubmit}>
          <div className="sm:col-span-2">
            <label className="mb-2 block text-sm text-stone-300" htmlFor="email">
              邮箱
            </label>
            <input
              id="email"
              type="email"
              required
              value={form.email}
              onChange={(event) => updateField("email", event.target.value)}
              className="w-full rounded-2xl border border-white/10 bg-white/5 px-4 py-3 text-white outline-none transition focus:border-amber-300"
            />
          </div>

          <div className="sm:col-span-2">
            <label className="mb-2 block text-sm text-stone-300" htmlFor="userName">
              用户名
            </label>
            <input
              id="userName"
              type="text"
              required
              value={form.userName}
              onChange={(event) => updateField("userName", event.target.value)}
              className="w-full rounded-2xl border border-white/10 bg-white/5 px-4 py-3 text-white outline-none transition focus:border-amber-300"
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
              value={form.password}
              onChange={(event) => updateField("password", event.target.value)}
              className="w-full rounded-2xl border border-white/10 bg-white/5 px-4 py-3 text-white outline-none transition focus:border-amber-300"
            />
          </div>

          <div>
            <label
              className="mb-2 block text-sm text-stone-300"
              htmlFor="confirmPassword"
            >
              确认密码
            </label>
            <input
              id="confirmPassword"
              type="password"
              required
              value={form.confirmPassword}
              onChange={(event) => updateField("confirmPassword", event.target.value)}
              className="w-full rounded-2xl border border-white/10 bg-white/5 px-4 py-3 text-white outline-none transition focus:border-amber-300"
            />
          </div>

          <div className="sm:col-span-2">
            <label className="mb-2 block text-sm text-stone-300" htmlFor="inviteCode">
              邀请码
            </label>
            <input
              id="inviteCode"
              type="text"
              value={form.inviteCode}
              onChange={(event) => updateField("inviteCode", event.target.value)}
              placeholder="没有可以先留空"
              className="w-full rounded-2xl border border-white/10 bg-white/5 px-4 py-3 text-white outline-none transition placeholder:text-stone-500 focus:border-amber-300"
            />
          </div>

          {errorMessage ? (
            <div className="sm:col-span-2 rounded-2xl border border-rose-400/20 bg-rose-400/10 px-4 py-3 text-sm text-rose-200">
              {errorMessage}
            </div>
          ) : null}

          <button
            type="submit"
            disabled={isSubmitting}
            className="sm:col-span-2 rounded-2xl bg-amber-400 px-5 py-3 font-medium text-stone-950 transition hover:bg-amber-300 disabled:cursor-not-allowed disabled:opacity-60"
          >
            {isSubmitting ? "注册中..." : "创建账号"}
          </button>

          <p className="sm:col-span-2 text-sm text-stone-400">
            已经有账号？{" "}
            <Link to="/login" className="text-amber-300 transition hover:text-amber-200">
              直接登录
            </Link>
          </p>
        </form>
      </div>
    </section>
  );
}
