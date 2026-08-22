import { useState, type FormEvent } from "react";
import { useAuth } from "../../../app/providers/AuthProvider";
import { getAccountTierLabel, getAccountTierMeta } from "../lib/account-tier";

export function RedeemInviteCodePage() {
  const { currentUser, redeemInviteCode } = useAuth();
  const [code, setCode] = useState("");
  const [message, setMessage] = useState<string | null>(null);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const tierMeta = getAccountTierMeta(currentUser?.accountTier);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setMessage(null);
    setErrorMessage(null);
    setIsSubmitting(true);

    try {
      const nextTier = await redeemInviteCode({ code });
      setMessage(
        `兑换成功，已升级到「${getAccountTierLabel(nextTier)}」，可以前往 AI 教练体验了。`
      );
      setCode("");
    } catch (error) {
      setErrorMessage(error instanceof Error ? error.message : "兑换失败，请稍后重试");
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <section className="mx-auto grid max-w-5xl gap-8 lg:grid-cols-[0.9fr_1.1fr]">
      <div className="rounded-[32px] border border-white/10 bg-white/5 p-8 backdrop-blur">
        <p className="text-sm uppercase tracking-[0.28em] text-amber-300">
          Invite Code
        </p>
        <h1 className="mt-4 text-4xl font-semibold text-white">兑换邀请码</h1>
        <p className="mt-4 leading-7 text-stone-300">
          输入邀请码，解锁 AI 训练模板生成与周期总结等高级能力。
        </p>
        <div className="mt-6 rounded-3xl border border-white/10 bg-black/20 p-5">
          <p className="text-sm text-stone-400">当前账户层级</p>
          <p className="mt-2 text-2xl font-semibold text-white">{tierMeta.label}</p>
          {tierMeta.description ? (
            <p className="mt-2 text-sm text-stone-400">{tierMeta.description}</p>
          ) : null}
        </div>
      </div>

      <div className="rounded-[32px] border border-white/10 bg-stone-950/70 p-8 shadow-[0_24px_80px_rgba(0,0,0,0.45)]">
        <form className="space-y-5" onSubmit={handleSubmit}>
          <div>
            <label className="mb-2 block text-sm text-stone-300" htmlFor="code">
              邀请码
            </label>
            <input
              id="code"
              type="text"
              required
              value={code}
              onChange={(event) => setCode(event.target.value)}
              placeholder="输入邀请码"
              className="w-full rounded-2xl border border-white/10 bg-white/5 px-4 py-3 text-white outline-none transition placeholder:text-stone-500 focus:border-amber-300"
            />
          </div>

          {message ? (
            <div className="rounded-2xl border border-emerald-400/20 bg-emerald-400/10 px-4 py-3 text-sm text-emerald-200">
              {message}
            </div>
          ) : null}

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
            {isSubmitting ? "兑换中..." : "兑换邀请码"}
          </button>
        </form>
      </div>
    </section>
  );
}
