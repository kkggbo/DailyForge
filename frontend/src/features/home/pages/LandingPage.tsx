import { Link } from "react-router-dom";
import { useAuth } from "../../../app/providers/AuthProvider";

const pillars = [
  {
    title: "训练计划",
    description: "用循环模板定义分化节奏，再按实际完成情况持续修正。"
  },
  {
    title: "训练打卡",
    description: "动作级记录完成、跳过与原因，为下一次计划迭代提供上下文。"
  },
  {
    title: "AI 建议",
    description: "基于体征、训练反馈与饮食目标，逐步补上个性化建议能力。"
  }
];

export function LandingPage() {
  const { isAuthenticated } = useAuth();

  return (
    <section className="space-y-8">
      <div className="grid gap-8 lg:grid-cols-[1.15fr_0.85fr] lg:items-end">
        <div className="rounded-[36px] border border-white/10 bg-white/6 p-8 shadow-[0_24px_90px_rgba(0,0,0,0.35)] backdrop-blur sm:p-10">
          <p className="text-sm uppercase tracking-[0.3em] text-amber-300">
            MVP Frontend
          </p>
          <h1 className="mt-5 max-w-4xl text-5xl font-semibold leading-[1.05] text-white sm:text-6xl">
            把混乱的训练记录，锻造成可执行、可复盘、可迭代的日常系统。
          </h1>
          <p className="mt-6 max-w-2xl text-base leading-8 text-stone-300 sm:text-lg">
            这不是最终 UI，而是一套已经能开始联调的前端底座。我们先把账号体系、基础导航、接口调用和视觉方向稳下来，后续再往 profile、计划、打卡和统计模块逐步加深。
          </p>
          <div className="mt-8 flex flex-wrap gap-3">
            <Link
              to={isAuthenticated ? "/app" : "/register"}
              className="rounded-full bg-amber-400 px-5 py-3 font-medium text-stone-950 transition hover:bg-amber-300"
            >
              {isAuthenticated ? "进入控制台" : "创建账号"}
            </Link>
            <Link
              to="/login"
              className="rounded-full border border-white/10 px-5 py-3 font-medium text-stone-100 transition hover:bg-white/8"
            >
              已有账号，直接登录
            </Link>
          </div>
        </div>

        <div className="rounded-[36px] border border-amber-300/15 bg-black/25 p-8 backdrop-blur">
          <p className="text-sm text-stone-400">当前这版已经打通</p>
          <ul className="mt-4 space-y-3 text-stone-200">
            <li>注册 / 登录 / 当前用户信息</li>
            <li>本地 token 持久化与路由守卫</li>
            <li>邀请码兑换入口</li>
            <li>Vite 代理到本地 Spring Boot `/api`</li>
          </ul>
        </div>
      </div>

      <div className="grid gap-6 md:grid-cols-3">
        {pillars.map((pillar) => (
          <article
            key={pillar.title}
            className="rounded-[28px] border border-white/10 bg-white/5 p-6 backdrop-blur"
          >
            <p className="text-sm uppercase tracking-[0.24em] text-amber-300">
              {pillar.title}
            </p>
            <p className="mt-3 text-base leading-7 text-stone-300">
              {pillar.description}
            </p>
          </article>
        ))}
      </div>
    </section>
  );
}
