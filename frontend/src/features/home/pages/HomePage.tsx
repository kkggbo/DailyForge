import { Link } from "react-router-dom";
import { useAuth } from "../../../app/providers/AuthProvider";

const gettingStartedSteps = [
  {
    title: "完善个人资料",
    description: "补充身体指标与训练目标，让计划更贴合你。",
    to: "/profile"
  },
  {
    title: "创建并启用训练模板",
    description: "用循环模板定义你的训练分化节奏。",
    to: "/cycle-templates"
  },
  {
    title: "开始训练打卡",
    description: "按当天计划记录完成、跳过与原因。",
    to: "/workout"
  },
  {
    title: "用 AI 教练",
    description: "生成模板草稿，或分析已完成周期。",
    to: "/ai-coach"
  }
];

const moduleCards = [
  {
    title: "训练模板",
    description: "草稿、正式模板与启用切换。",
    to: "/cycle-templates"
  },
  {
    title: "训练工作台",
    description: "Day 导航、打卡与历史记录。",
    to: "/workout"
  },
  {
    title: "AI 教练",
    description: "AI 生成模板与周期总结。",
    to: "/ai-coach"
  },
  {
    title: "个人资料",
    description: "基础档案与身体指标。",
    to: "/profile"
  }
];

export function HomePage() {
  const { currentUser } = useAuth();

  return (
    <section className="space-y-8">
      <div className="rounded-[36px] border border-white/10 bg-white/6 p-8 backdrop-blur">
        <p className="text-sm uppercase tracking-[0.28em] text-amber-300">控制台</p>
        <h1 className="mt-4 text-4xl font-semibold leading-tight text-white sm:text-5xl">
          你好，{currentUser?.userName ?? "训练者"}。
        </h1>
        <p className="mt-4 max-w-2xl leading-8 text-stone-300">
          开始今天的训练，或先完善你的训练计划。下面是从入门到进阶的完整路径。
        </p>
      </div>

      <div>
        <p className="text-sm uppercase tracking-[0.24em] text-amber-300">快速入门</p>
        <div className="mt-4 grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
          {gettingStartedSteps.map((step, index) => (
            <Link
              key={step.title}
              to={step.to}
              className="rounded-[28px] border border-white/10 bg-white/5 p-5 transition hover:bg-white/8"
            >
              <p className="text-xs font-semibold uppercase tracking-[0.2em] text-amber-300">
                第 {index + 1} 步
              </p>
              <p className="mt-2 text-base font-medium text-white">{step.title}</p>
              <p className="mt-1 text-sm leading-6 text-stone-300">{step.description}</p>
            </Link>
          ))}
        </div>
      </div>

      <div>
        <p className="text-sm uppercase tracking-[0.24em] text-amber-300">功能入口</p>
        <div className="mt-4 grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
          {moduleCards.map((card) => (
            <Link
              key={card.title}
              to={card.to}
              className="rounded-[28px] border border-white/10 bg-white/5 p-5 transition hover:bg-white/8"
            >
              <p className="text-base font-medium text-white">{card.title}</p>
              <p className="mt-1 text-sm leading-6 text-stone-300">{card.description}</p>
            </Link>
          ))}
        </div>
      </div>

      <div className="rounded-[32px] border border-amber-300/20 bg-stone-950/70 p-6">
        <p className="text-sm uppercase tracking-[0.24em] text-amber-300">解锁 AI 权限</p>
        <p className="mt-4 leading-7 text-stone-300">
          AI 教练功能需要邀请码解锁。已有邀请码的话，兑换后即可使用 AI 生成模板与周期总结。
        </p>
        <Link
          to="/invite-code"
          className="mt-6 inline-flex rounded-full bg-amber-400 px-5 py-3 font-medium text-stone-950 transition hover:bg-amber-300"
        >
          去兑换邀请码
        </Link>
      </div>
    </section>
  );
}
