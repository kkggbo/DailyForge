import { useMemo, useState, type FormEvent } from "react";
import { Link, useNavigate } from "react-router-dom";
import { useAuth } from "../../../app/providers/AuthProvider";

type PasswordStrengthLevel = "invalid" | "weak" | "medium" | "strong" | "very-strong";

type PasswordStrengthMeta = {
  level: PasswordStrengthLevel;
  label: string;
  description: string;
  toneClassName: string;
  progressClassName: string;
  progressWidthClassName: string;
};

const passwordStrengthMetaMap: Record<PasswordStrengthLevel, PasswordStrengthMeta> = {
  invalid: {
    level: "invalid",
    label: "长度不合规",
    description: "密码长度必须在 6 到 18 位之间。",
    toneClassName: "text-rose-200",
    progressClassName: "bg-rose-400/15",
    progressWidthClassName: "w-1/4"
  },
  weak: {
    level: "weak",
    label: "弱",
    description: "建议不要只用纯数字或单一字符类型。",
    toneClassName: "text-rose-200",
    progressClassName: "bg-rose-400",
    progressWidthClassName: "w-1/4"
  },
  medium: {
    level: "medium",
    label: "中",
    description: "已具备基础安全性，建议继续加入更多字符类型。",
    toneClassName: "text-amber-200",
    progressClassName: "bg-amber-400",
    progressWidthClassName: "w-2/4"
  },
  strong: {
    level: "strong",
    label: "强",
    description: "密码复杂度较好，适合作为日常使用密码。",
    toneClassName: "text-lime-200",
    progressClassName: "bg-lime-400",
    progressWidthClassName: "w-3/4"
  },
  "very-strong": {
    level: "very-strong",
    label: "很强",
    description: "已包含多种字符类型，安全性较高。",
    toneClassName: "text-emerald-200",
    progressClassName: "bg-emerald-400",
    progressWidthClassName: "w-full"
  }
};

export function RegisterPage() {
  const navigate = useNavigate();
  const { login, register } = useAuth();
  const [form, setForm] = useState({
    email: "",
    userName: "",
    password: "",
    confirmPassword: "",
    inviteCode: ""
  });
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const passwordStrength = useMemo(
    () => evaluatePasswordStrength(form.password),
    [form.password]
  );

  function updateField<K extends keyof typeof form>(key: K, value: (typeof form)[K]) {
    setForm((previous) => ({
      ...previous,
      [key]: value
    }));
    setErrorMessage(null);
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();

    if (!isPasswordLengthValid(form.password)) {
      setErrorMessage("密码长度必须在 6 到 18 位之间");
      return;
    }

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

      try {
        await login({
          email: form.email,
          password: form.password
        });
        navigate("/app", { replace: true });
      } catch {
        navigate("/login", {
          replace: true,
          state: {
            email: form.email,
            message: "注册成功，但自动登录失败，请手动登录。"
          }
        });
      }
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
          先把账户建好，我们就能直接进入 DailyForge 的训练记录和资料引导。
        </h1>
        <div className="mt-6 space-y-4 text-stone-300">
          <p>
            当前注册表单已经对齐后端接口字段，包括 `email`、`password`、
            `confirmPassword`、`userName` 和 `inviteCode`。
          </p>
          <p>
            如果暂时没有邀请码，也可以先注册普通账号，后续登录后再到邀请码页面兑换 AI 权限。
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

          <div className="sm:col-span-2">
            <label className="mb-2 block text-sm text-stone-300" htmlFor="password">
              密码
            </label>
            <input
              id="password"
              type="password"
              required
              minLength={6}
              maxLength={18}
              value={form.password}
              onChange={(event) => updateField("password", event.target.value)}
              className="w-full rounded-2xl border border-white/10 bg-white/5 px-4 py-3 text-white outline-none transition focus:border-amber-300"
            />

            <div className="mt-3 rounded-2xl border border-white/10 bg-white/5 px-4 py-3">
              <div className="flex items-center justify-between gap-3">
                <p className="text-sm text-stone-300">密码安全等级</p>
                <span className={`text-sm font-medium ${passwordStrength.toneClassName}`}>
                  {form.password ? passwordStrength.label : "待输入"}
                </span>
              </div>

              <div className="mt-3 h-2 rounded-full bg-white/8">
                <div
                  className={[
                    "h-2 rounded-full transition-all",
                    form.password
                      ? passwordStrength.progressClassName
                      : "bg-white/10",
                    form.password ? passwordStrength.progressWidthClassName : "w-0"
                  ].join(" ")}
                />
              </div>

              <p className="mt-3 text-xs leading-6 text-stone-400">
                规则：密码长度必须为 6 到 18 位。安全等级会根据是否为数字英文混合、是否包含大写字母、是否包含特殊符号进行提示，不额外限制你的实际密码内容。
              </p>

              {form.password ? (
                <p className={`mt-2 text-xs leading-6 ${passwordStrength.toneClassName}`}>
                  {passwordStrength.description}
                </p>
              ) : null}
            </div>
          </div>

          <div className="sm:col-span-2">
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
              minLength={6}
              maxLength={18}
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

function isPasswordLengthValid(password: string) {
  return password.length >= 6 && password.length <= 18;
}

function evaluatePasswordStrength(password: string): PasswordStrengthMeta {
  if (!password) {
    return passwordStrengthMetaMap.invalid;
  }

  if (!isPasswordLengthValid(password)) {
    return passwordStrengthMetaMap.invalid;
  }

  const hasDigit = /\d/.test(password);
  const hasLowercase = /[a-z]/.test(password);
  const hasUppercase = /[A-Z]/.test(password);
  const hasSpecial = /[^A-Za-z0-9]/.test(password);

  const categoryCount = [hasDigit, hasLowercase, hasUppercase, hasSpecial].filter(Boolean)
    .length;

  if (categoryCount <= 1) {
    return passwordStrengthMetaMap.weak;
  }

  if (categoryCount === 2) {
    if (hasDigit && hasLowercase && password.length >= 10) {
      return passwordStrengthMetaMap.strong;
    }

    return passwordStrengthMetaMap.medium;
  }

  if (categoryCount === 3) {
    return password.length >= 10
      ? passwordStrengthMetaMap["very-strong"]
      : passwordStrengthMetaMap.strong;
  }

  return passwordStrengthMetaMap["very-strong"];
}
