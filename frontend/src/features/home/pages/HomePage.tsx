import { Link } from "react-router-dom";
import { useAuth } from "../../../app/providers/AuthProvider";

const nextModules = [
  "个人资料与身体指标 profile",
  "循环模板 cycle template",
  "训练日 session 打卡",
  "历史统计与趋势图"
];

export function HomePage() {
  const { currentUser } = useAuth();

  return (
    <section className="space-y-8">
      <div className="grid gap-8 xl:grid-cols-[1.05fr_0.95fr]">
        <div className="rounded-[36px] border border-white/10 bg-white/6 p-8 backdrop-blur">
          <p className="text-sm uppercase tracking-[0.28em] text-amber-300">
            Dashboard
          </p>
          <h1 className="mt-4 text-4xl font-semibold leading-tight text-white sm:text-5xl">
            {currentUser?.userName}，基础联调已经就位。
          </h1>
          <p className="mt-4 max-w-2xl leading-8 text-stone-300">
            你现在可以把这个页面作为前端开发起点：鉴权状态、用户摘要、受保护路由、退出登录和邀请码升级都已经具备。接下来优先从个人资料模块进入，把 AI 所需的最小用户画像先补齐。
          </p>
          <div className="mt-6 flex flex-wrap gap-3">
            <Link
              to="/profile"
              className="rounded-full bg-amber-400 px-5 py-3 font-medium text-stone-950 transition hover:bg-amber-300"
            >
              进入个人资料
            </Link>
            <Link
              to="/profile/ai-completion?scene=ai-plan&redirect=/app"
              className="rounded-full border border-white/10 px-5 py-3 font-medium text-stone-100 transition hover:bg-white/8"
            >
              查看 AI 补录页
            </Link>
          </div>
        </div>

        <div className="rounded-[36px] border border-amber-300/15 bg-black/25 p-8 backdrop-blur">
          <p className="text-sm text-stone-400">账户摘要</p>
          <div className="mt-5 grid gap-4 sm:grid-cols-2">
            <SummaryCard label="用户 ID" value={String(currentUser?.userId ?? "-")} />
            <SummaryCard label="邮箱" value={currentUser?.email ?? "-"} />
            <SummaryCard label="平台角色" value={currentUser?.platformRole ?? "-"} />
            <SummaryCard label="账户层级" value={currentUser?.accountTier ?? "-"} />
            <SummaryCard label="状态" value={currentUser?.status ?? "-"} />
          </div>
        </div>
      </div>

      <div className="grid gap-6 lg:grid-cols-[0.9fr_1.1fr]">
        <div className="rounded-[32px] border border-white/10 bg-white/5 p-6">
          <p className="text-sm uppercase tracking-[0.24em] text-amber-300">
            下一步建议
          </p>
          <ul className="mt-4 space-y-3 text-stone-300">
            {nextModules.map((item) => (
              <li key={item}>{item}</li>
            ))}
          </ul>
        </div>

        <div className="rounded-[32px] border border-white/10 bg-stone-950/70 p-6">
          <p className="text-sm uppercase tracking-[0.24em] text-amber-300">
            权限升级
          </p>
          <p className="mt-4 leading-7 text-stone-300">
            如果你当前账户还是普通层级，可以先去邀请码页面兑换 AI 访问资格，验证后端的账户层级变更链路。
          </p>
          <Link
            to="/invite-code"
            className="mt-6 inline-flex rounded-full bg-amber-400 px-5 py-3 font-medium text-stone-950 transition hover:bg-amber-300"
          >
            去兑换邀请码
          </Link>
        </div>
      </div>
    </section>
  );
}

type SummaryCardProps = {
  label: string;
  value: string;
};

function SummaryCard({ label, value }: SummaryCardProps) {
  return (
    <article className="rounded-3xl border border-white/10 bg-white/5 p-4">
      <p className="text-xs uppercase tracking-[0.22em] text-stone-500">{label}</p>
      <p className="mt-2 break-all text-base font-medium text-white">{value}</p>
    </article>
  );
}
