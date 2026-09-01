import { useEffect, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { resetPassword, sendForgotPasswordCode } from "../api/auth";
import {
  getAuthErrorMessage,
  validateConfirmPassword,
  validateNewPassword
} from "../lib/auth-validation";

const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

const backLinkClass =
  "inline-flex rounded-full border border-white/10 bg-white/8 px-4 py-2 text-sm text-stone-100 transition hover:bg-white/12";

export function ForgotPasswordPage() {
  const navigate = useNavigate();
  const [step, setStep] = useState<1 | 2>(1);
  const [email, setEmail] = useState("");
  const [code, setCode] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");

  const [isSending, setIsSending] = useState(false);
  const [sendError, setSendError] = useState<string | null>(null);
  const [countdown, setCountdown] = useState(0);

  const [isResetting, setIsResetting] = useState(false);
  const [resetError, setResetError] = useState<string | null>(null);

  useEffect(() => {
    if (countdown <= 0) {
      return;
    }

    const timerId = window.setTimeout(() => {
      setCountdown((previous) => previous - 1);
    }, 1000);

    return () => {
      window.clearTimeout(timerId);
    };
  }, [countdown]);

  async function handleSendCode() {
    const trimmedEmail = email.trim();
    if (!EMAIL_PATTERN.test(trimmedEmail)) {
      setSendError("请输入有效的邮箱地址。");
      return;
    }

    setIsSending(true);
    setSendError(null);

    try {
      await sendForgotPasswordCode({ email: trimmedEmail });
      setStep(2);
      setCountdown(60);
    } catch (error) {
      setSendError(
        getAuthErrorMessage(error, "验证码发送失败，请稍后再试。")
      );
    } finally {
      setIsSending(false);
    }
  }

  async function handleReset() {
    const newPasswordError = validateNewPassword(newPassword);
    if (newPasswordError) {
      setResetError(newPasswordError);
      return;
    }

    const confirmError = validateConfirmPassword(newPassword, confirmPassword);
    if (confirmError) {
      setResetError(confirmError);
      return;
    }

    setIsResetting(true);
    setResetError(null);

    try {
      await resetPassword({
        email: email.trim(),
        code: code.trim(),
        newPassword,
        confirmPassword
      });
      navigate("/login", {
        replace: true,
        state: { message: "密码已重置，请使用新密码登录。" }
      });
    } catch (error) {
      setResetError(
        getAuthErrorMessage(error, "重置密码失败，请稍后再试。")
      );
    } finally {
      setIsResetting(false);
    }
  }

  return (
    <section className="mx-auto max-w-xl space-y-6">
      <div>
        <Link to="/login" className={backLinkClass}>
          返回登录
        </Link>
      </div>

      <header className="rounded-[32px] border border-white/10 bg-white/6 p-8 backdrop-blur">
        <p className="text-sm uppercase tracking-[0.28em] text-amber-300">
          Reset Password
        </p>
        <h1 className="mt-4 text-3xl font-semibold leading-tight text-white sm:text-4xl">
          忘记密码
        </h1>
        <p className="mt-3 leading-7 text-stone-300">
          {step === 1
            ? "输入注册邮箱，我们会向你发送一个 6 位数字验证码（10 分钟内有效）。"
            : "输入验证码并设置新密码。"}
        </p>
      </header>

      {step === 1 ? (
        <div className="rounded-[32px] border border-white/10 bg-white/6 p-6 backdrop-blur">
          <label htmlFor="forgot-email" className="mb-2 block text-sm text-stone-300">
            注册邮箱
          </label>
          <input
            id="forgot-email"
            type="email"
            value={email}
            onChange={(event) => setEmail(event.target.value)}
            placeholder="user@example.com"
            className="w-full rounded-2xl border border-white/10 bg-stone-950/70 px-4 py-3 text-white outline-none transition placeholder:text-stone-500 focus:border-amber-300/60"
          />

          {sendError ? (
            <p className="mt-4 rounded-2xl border border-rose-400/20 bg-rose-400/10 px-4 py-3 text-sm text-rose-100">
              {sendError}
            </p>
          ) : null}

          <button
            type="button"
            disabled={isSending || countdown > 0}
            onClick={() => void handleSendCode()}
            className="mt-4 rounded-full bg-amber-400 px-5 py-3 text-sm font-semibold text-stone-950 transition hover:bg-amber-300 disabled:opacity-60"
          >
            {isSending
              ? "发送中..."
              : countdown > 0
                ? `重新发送 (${countdown}s)`
                : "发送验证码"}
          </button>
        </div>
      ) : (
        <div className="rounded-[32px] border border-white/10 bg-white/6 p-6 backdrop-blur">
          <div className="mb-4 rounded-2xl border border-white/10 bg-white/5 px-4 py-3 text-sm text-stone-300">
            验证码已发送到 <span className="text-stone-100">{email}</span>，请查收（10 分钟内有效）。
          </div>

          <div className="space-y-4">
            <div>
              <label htmlFor="forgot-code" className="mb-2 block text-sm text-stone-300">
                验证码
              </label>
              <input
                id="forgot-code"
                type="text"
                inputMode="numeric"
                value={code}
                onChange={(event) => setCode(event.target.value)}
                placeholder="6 位数字验证码"
                className="w-full rounded-2xl border border-white/10 bg-stone-950/70 px-4 py-3 text-white outline-none transition focus:border-amber-300/60"
              />
            </div>
            <PasswordField
              id="forgot-newPassword"
              label="新密码"
              value={newPassword}
              onChange={setNewPassword}
            />
            <PasswordField
              id="forgot-confirmPassword"
              label="确认新密码"
              value={confirmPassword}
              onChange={setConfirmPassword}
            />
          </div>

          {resetError ? (
            <p className="mt-4 rounded-2xl border border-rose-400/20 bg-rose-400/10 px-4 py-3 text-sm text-rose-100">
              {resetError}
            </p>
          ) : null}

          <div className="mt-4 flex flex-wrap gap-3">
            <button
              type="button"
              disabled={isResetting}
              onClick={() => void handleReset()}
              className="rounded-full bg-amber-400 px-5 py-3 text-sm font-semibold text-stone-950 transition hover:bg-amber-300 disabled:opacity-60"
            >
              {isResetting ? "提交中..." : "重置密码"}
            </button>
            {countdown > 0 ? (
              <button
                type="button"
                onClick={() => void handleSendCode()}
                className="rounded-full border border-white/10 bg-white/8 px-5 py-3 text-sm font-semibold text-stone-100 transition hover:bg-white/12"
              >
                重新发送 ({countdown}s)
              </button>
            ) : null}
          </div>
        </div>
      )}
    </section>
  );
}

function PasswordField({
  id,
  label,
  value,
  onChange
}: {
  id: string;
  label: string;
  value: string;
  onChange: (value: string) => void;
}) {
  return (
    <div>
      <label htmlFor={id} className="mb-2 block text-sm text-stone-300">
        {label}
      </label>
      <input
        id={id}
        type="password"
        value={value}
        onChange={(event) => onChange(event.target.value)}
        className="w-full rounded-2xl border border-white/10 bg-stone-950/70 px-4 py-3 text-white outline-none transition focus:border-amber-300/60"
      />
    </div>
  );
}
