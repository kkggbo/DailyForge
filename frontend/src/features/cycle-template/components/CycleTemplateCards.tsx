import { Link } from "react-router-dom";
import {
  formatCycleLength,
  formatDateTime,
  formatGoalType,
  formatStatus
} from "../lib/cycle-template-formatters";
import type {
  CurrentActiveTemplateResponse,
  DraftTemplateListItem,
  FormalTemplateListItem
} from "../types/cycle-template";

type ActiveTemplatePanelProps = {
  activeTemplate: CurrentActiveTemplateResponse | null;
};

export function ActiveTemplatePanel({ activeTemplate }: ActiveTemplatePanelProps) {
  if (!activeTemplate) {
    return (
      <section className="rounded-[28px] border border-dashed border-white/15 bg-white/6 p-6">
        <p className="text-sm uppercase tracking-[0.24em] text-amber-300">
          Active Cycle
        </p>
        <h2 className="mt-3 text-2xl font-semibold text-white">当前没有启用模板</h2>
        <p className="mt-2 text-sm leading-6 text-stone-300">
          创建并启用一个训练模板后，这里会显示当前循环进度和下一次默认训练日。
        </p>
      </section>
    );
  }

  return (
    <section className="rounded-[28px] border border-amber-300/20 bg-amber-300/10 p-6 shadow-2xl shadow-amber-950/20">
      <div className="flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
        <div>
          <p className="text-sm uppercase tracking-[0.24em] text-amber-200">
            Active Cycle
          </p>
          <h2 className="mt-3 text-3xl font-semibold text-white">
            {activeTemplate.templateName}
          </h2>
          <p className="mt-2 text-sm text-stone-200">
            Day {activeTemplate.currentDayIndex} ·{" "}
            {activeTemplate.currentDayName ?? "未命名训练日"} ·{" "}
            {formatCycleLength(activeTemplate.cycleLength)}
          </p>
          <p className="mt-1 text-xs text-stone-400">
            启用时间：{formatDateTime(activeTemplate.startedAt)}
          </p>
        </div>
        <Link
          to={`/cycle-templates/${activeTemplate.templateId}`}
          className="rounded-full bg-amber-400 px-5 py-3 text-sm font-semibold text-stone-950 transition hover:bg-amber-300"
        >
          查看当前模板
        </Link>
      </div>
    </section>
  );
}

type TemplateListProps = {
  formalTemplates?: FormalTemplateListItem[];
  draftTemplates?: DraftTemplateListItem[];
  type: "formal" | "drafts";
  onCopy: (templateId: number, templateName: string) => void;
  onDelete: (templateId: number, templateName: string) => void;
  onActivate: (templateId: number, templateName: string) => void;
};

export function TemplateList({
  formalTemplates,
  draftTemplates,
  type,
  onCopy,
  onDelete,
  onActivate
}: TemplateListProps) {
  const isEmpty =
    type === "formal"
      ? (formalTemplates?.length ?? 0) === 0
      : (draftTemplates?.length ?? 0) === 0;

  if (isEmpty) {
    return (
      <div className="rounded-[28px] border border-dashed border-white/12 bg-white/5 p-8 text-center">
        <h3 className="text-xl font-semibold text-white">
          {type === "formal" ? "还没有正式模板" : "还没有草稿模板"}
        </h3>
        <p className="mt-2 text-sm text-stone-400">
          {type === "formal"
            ? "草稿确认启用后会进入正式模板列表。"
            : "先创建一个草稿，把你的分化循环搭起来。"}
        </p>
      </div>
    );
  }

  return (
    <div className="grid gap-4 lg:grid-cols-2">
      {type === "formal"
        ? formalTemplates?.map((template) => (
            <FormalTemplateCard
              key={template.templateId}
              template={template}
              onCopy={onCopy}
              onDelete={onDelete}
              onActivate={onActivate}
            />
          ))
        : draftTemplates?.map((template) => (
            <DraftTemplateCard
              key={template.templateId}
              template={template}
              onCopy={onCopy}
              onDelete={onDelete}
              onActivate={onActivate}
            />
          ))}
    </div>
  );
}

function FormalTemplateCard({
  template,
  onCopy,
  onDelete,
  onActivate
}: {
  template: FormalTemplateListItem;
  onCopy: (templateId: number, templateName: string) => void;
  onDelete: (templateId: number, templateName: string) => void;
  onActivate: (templateId: number, templateName: string) => void;
}) {
  return (
    <article className="rounded-[28px] border border-white/10 bg-white/8 p-5">
      <div className="flex items-start justify-between gap-4">
        <div>
          <p className="text-xs uppercase tracking-[0.2em] text-amber-300">
            {formatStatus(template.status)}
          </p>
          <h3 className="mt-2 text-2xl font-semibold text-white">
            {template.templateName}
          </h3>
        </div>
        {template.isActive ? (
          <span className="rounded-full bg-amber-400 px-3 py-1 text-xs font-semibold text-stone-950">
            当前启用
          </span>
        ) : null}
      </div>

      <div className="mt-4 grid gap-2 text-sm text-stone-300">
        <span>{formatCycleLength(template.cycleLength)}</span>
        <span>目标：{formatGoalType(template.goalType)}</span>
        <span>
          {template.currentDayIndex ? `运行到 Day ${template.currentDayIndex}` : "未运行"}
        </span>
        <span>更新：{formatDateTime(template.updatedAt)}</span>
      </div>

      <CardActions
        templateId={template.templateId}
        templateName={template.templateName}
        canDelete={!template.isActive}
        canActivate={!template.isActive}
        onCopy={onCopy}
        onDelete={onDelete}
        onActivate={onActivate}
      />
    </article>
  );
}

function DraftTemplateCard({
  template,
  onCopy,
  onDelete,
  onActivate
}: {
  template: DraftTemplateListItem;
  onCopy: (templateId: number, templateName: string) => void;
  onDelete: (templateId: number, templateName: string) => void;
  onActivate: (templateId: number, templateName: string) => void;
}) {
  return (
    <article className="rounded-[28px] border border-white/10 bg-white/8 p-5">
      <p className="text-xs uppercase tracking-[0.2em] text-amber-300">草稿</p>
      <h3 className="mt-2 text-2xl font-semibold text-white">
        {template.templateName}
      </h3>
      <div className="mt-4 grid gap-2 text-sm text-stone-300">
        <span>{formatCycleLength(template.cycleLength)}</span>
        <span>已配置 {template.configuredDayCount} 个训练日</span>
        <span>创建：{formatDateTime(template.createdAt)}</span>
        <span>更新：{formatDateTime(template.updatedAt)}</span>
      </div>

      <CardActions
        templateId={template.templateId}
        templateName={template.templateName}
        canDelete
        canActivate
        onCopy={onCopy}
        onDelete={onDelete}
        onActivate={onActivate}
      />
    </article>
  );
}

function CardActions({
  templateId,
  templateName,
  canDelete,
  canActivate,
  onCopy,
  onDelete,
  onActivate
}: {
  templateId: number;
  templateName: string;
  canDelete: boolean;
  canActivate: boolean;
  onCopy: (templateId: number, templateName: string) => void;
  onDelete: (templateId: number, templateName: string) => void;
  onActivate: (templateId: number, templateName: string) => void;
}) {
  return (
    <div className="mt-5 flex flex-wrap gap-2">
      <Link to={`/cycle-templates/${templateId}`} className={secondaryButtonClass}>
        详情
      </Link>
      <Link to={`/cycle-templates/${templateId}/edit`} className={secondaryButtonClass}>
        编辑
      </Link>
      <button
        type="button"
        onClick={() => onCopy(templateId, templateName)}
        className={secondaryButtonClass}
      >
        复制
      </button>
      {canActivate ? (
        <button
          type="button"
          onClick={() => onActivate(templateId, templateName)}
          className={primaryButtonClass}
        >
          启用
        </button>
      ) : null}
      {canDelete ? (
        <button
          type="button"
          onClick={() => onDelete(templateId, templateName)}
          className="rounded-full border border-rose-300/25 px-4 py-2 text-sm text-rose-100 transition hover:bg-rose-400/10"
        >
          删除
        </button>
      ) : null}
    </div>
  );
}

const primaryButtonClass =
  "rounded-full bg-amber-400 px-4 py-2 text-sm font-semibold text-stone-950 transition hover:bg-amber-300";

const secondaryButtonClass =
  "rounded-full border border-white/10 bg-white/8 px-4 py-2 text-sm text-stone-100 transition hover:bg-white/12";
