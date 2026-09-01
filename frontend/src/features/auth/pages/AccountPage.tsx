import { useState } from "react";
import { Link } from "react-router-dom";
import { useAuth } from "../../../app/providers/AuthProvider";
import { changePassword } from "../api/auth";
import {
  getAuthErrorMessage,
  validateConfirmPassword,
  validateNewPassword,
  validateNewPasswordDiffersFromOld,
  validateUserName
} from "../lib/auth-validation";

const backLinkClass =
  "inline-flex rounded-full border border-white/10 bg-white/8 px-4 py-2 text-sm text-stone-100 transition hover:bg-white/12";

export function AccountPage() {
  const { currentUser, accessToken, updateUserName } = useAuth();

  // 用户名区
  const [userName, setUserName] = useState(currentUser?.userName ?? "");
  const [isUpdatingName, setIsUpdatingName] = useState(false);
  const [nameError, setNameError] = useState<string | null>(null);
  const [nameSuccess, setNameSuccess] = useState<string | null>(null);

  // 密码区
  const [oldPassword, setOldPassword] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [isChangingPassword, setIsChangingPassword] = useState(false);
  const [passwordError, setPasswordError] = useState<string | null>(null);
  const [passwordSuccess, setPasswordSuccess] = useState<string | null>(null);

  async function handleUserNameSubmit() {
    if (!accessToken) {
      return;
    }

    const trimmed = userName.trim();
    const validationError = validateUserName(trimmed);
    if (validationError) {
      setNameError(validationError);
      setNameSuccess(null);
      return;
    }

    setIsUpdatingName(true);
    setNameError(null);
    setNameSuccess(null);

    try {
      await updateUserName(trimmed);
      setUserName(trimmed);
      setNameSuccess("用户名已更新。");
    } catch (error) {
      setNameError(
        getAuthErrorMessage(error, "修改用户名失败，请稍后再试。")
      );
    } finally {
      setIsUpdatingName(false);
    }
  }

  async function handlePasswordSubmit() {
    if (!accessToken) {
      return;
    }

    const newPasswordError = validateNewPassword(newPassword);
    if (newPasswordError) {
      setPasswordError(newPasswordError);
      setPasswordSuccess(null);
      return;
    }

    const confirmError = validateConfirmPassword(newPassword, confirmPassword);
    if (confirmError) {
      setPasswordError(confirmError);
      setPasswordSuccess(null);
      return;
    }

    const diffError = validateNewPasswordDiffersFromOld(oldPassword, newPassword);
    if (diffError) {
      setPasswordError(diffError);
      setPasswordSuccess(null);
      return;
    }

    setIsChangingPassword(true);
    setPasswordError(null);
    setPasswordSuccess(null);

    try {
      await changePassword(accessToken, {
        oldPassword,
        newPassword,
        confirmPassword
      });
      setPasswordSuccess("密码已修改。");
      setOldPassword("");
      setNewPassword("");
      setConfirmPassword("");
    } catch (error) {
      setPasswordError(
        getAuthErrorMessage(error, "修改密码失败，请稍后再试。")
      );
    } finally {
      setIsChangingPassword(false);
    }
  }

  return (
    <section className="space-y-8">
      <div>
        <Link to="/profile" className={backLinkClass}>
          返回个人资料
        </Link>
      </div>

      <header className="rounded-[36px] border border-white/10 bg-white/6 p-8 backdrop-blur">
        <p className="text-sm uppercase tracking-[0.28em] text-amber-300">
          Account Settings
        </p>
        <h1 className="mt-4 text-4xl font-semibold leading-tight text-white sm:text-5xl">
          账号设置
        </h1>
        <p className="mt-4 max-w-2xl leading-8 text-stone-300">
          管理你的用户名与登录密码。
        </p>
      </header>

      <section className="rounded-[32px] border border-white/10 bg-white/6 p-6 backdrop-blur">
        <h2 className="text-2xl font-semibold text-white">修改用户名</h2>
        <p className="mt-2 text-sm text-stone-400">
          当前用户名：<span className="text-stone-200">{currentUser?.userName ?? "--"}</span>
        </p>

        <div className="mt-5 max-w-md">
          <label htmlFor="account-userName" className="mb-2 block text-sm text-stone-300">
            新用户名
          </label>
          <input
            id="account-userName"
            type="text"
            value={userName}
            onChange={(event) => setUserName(event.target.value)}
            placeholder="2~20 位中英文、数字或下划线"
            className="w-full rounded-2xl border border-white/10 bg-stone-950/70 px-4 py-3 text-sm text-white outline-none transition placeholder:text-stone-500 focus:border-amber-300/60"
          />
          {nameError ? (
            <p className="mt-3 rounded-2xl border border-rose-400/20 bg-rose-400/10 px-4 py-3 text-sm text-rose-100">
              {nameError}
            </p>
          ) : nameSuccess ? (
            <p className="mt-3 rounded-2xl border border-emerald-400/20 bg-emerald-400/10 px-4 py-3 text-sm text-emerald-200">
              {nameSuccess}
            </p>
          ) : null}
          <button
            type="button"
            disabled={isUpdatingName}
            onClick={() => void handleUserNameSubmit()}
            className="mt-4 rounded-full bg-amber-400 px-5 py-3 text-sm font-semibold text-stone-950 transition hover:bg-amber-300 disabled:opacity-60"
          >
            {isUpdatingName ? "保存中..." : "保存用户名"}
          </button>
        </div>
      </section>

      <section className="rounded-[32px] border border-white/10 bg-white/6 p-6 backdrop-blur">
        <h2 className="text-2xl font-semibold text-white">修改密码</h2>
        <p className="mt-2 text-sm text-stone-400">
          需要验证旧密码；新密码长度需在 6~18 位之间。
        </p>

        <div className="mt-5 max-w-md space-y-4">
          <PasswordField
            id="account-oldPassword"
            label="旧密码"
            value={oldPassword}
            onChange={setOldPassword}
          />
          <PasswordField
            id="account-newPassword"
            label="新密码"
            value={newPassword}
            onChange={setNewPassword}
          />
          <PasswordField
            id="account-confirmPassword"
            label="确认新密码"
            value={confirmPassword}
            onChange={setConfirmPassword}
          />

          {passwordError ? (
            <p className="rounded-2xl border border-rose-400/20 bg-rose-400/10 px-4 py-3 text-sm text-rose-100">
              {passwordError}
            </p>
          ) : passwordSuccess ? (
            <p className="rounded-2xl border border-emerald-400/20 bg-emerald-400/10 px-4 py-3 text-sm text-emerald-200">
              {passwordSuccess}
            </p>
          ) : null}

          <button
            type="button"
            disabled={isChangingPassword}
            onClick={() => void handlePasswordSubmit()}
            className="rounded-full bg-amber-400 px-5 py-3 text-sm font-semibold text-stone-950 transition hover:bg-amber-300 disabled:opacity-60"
          >
            {isChangingPassword ? "提交中..." : "修改密码"}
          </button>
        </div>
      </section>
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
        className="w-full rounded-2xl border border-white/10 bg-stone-950/70 px-4 py-3 text-sm text-white outline-none transition focus:border-amber-300/60"
      />
    </div>
  );
}
