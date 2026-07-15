import {
  getMetricMeta,
  getMetricOptionsByStructureType
} from "../lib/cycle-template-metric-config";
import type { EditorMetricForm, StructureType } from "../types/cycle-template";

type MetricEditorProps = {
  metric: EditorMetricForm;
  structureType: StructureType | null;
  locked: boolean;
  errorKey?: string;
  errorValue?: string;
  onChange: (patch: Partial<EditorMetricForm>) => void;
  onRemove: () => void;
  onMove: (direction: -1 | 1) => void;
};

export function MetricEditor({
  metric,
  structureType,
  locked,
  errorKey,
  errorValue,
  onChange,
  onRemove,
  onMove
}: MetricEditorProps) {
  const options = getMetricOptionsByStructureType(structureType);
  const metricMeta = getMetricMeta(metric.metricKey);

  return (
    <div className="rounded-2xl border border-white/10 bg-white/6 p-3">
      <div className="grid gap-3 lg:grid-cols-[1fr_1fr_auto]">
        <label className="space-y-2">
          <span className="text-xs text-stone-300">指标类型</span>
          <select
            value={metric.metricKey}
            disabled={locked}
            onChange={(event) =>
              onChange({ metricKey: event.target.value as EditorMetricForm["metricKey"] })
            }
            className={inputClass}
          >
            <option value="">请选择指标</option>
            {options.map((option) => (
              <option key={option.key} value={option.key}>
                {option.label}
              </option>
            ))}
          </select>
          <FieldError message={errorKey} />
        </label>

        <label className="space-y-2">
          <span className="text-xs text-stone-300">指标数值</span>
          <div className="flex items-center gap-2">
            <input
              type="number"
              step={metricMeta?.step ?? "0.1"}
              min={metricMeta?.min}
              max={metricMeta?.max}
              value={metric.metricValueNumberText}
              disabled={locked}
              onChange={(event) => onChange({ metricValueNumberText: event.target.value })}
              className={inputClass}
            />
            <span className="min-w-12 text-xs text-stone-400">
              {metricMeta?.unitLabel ?? "-"}
            </span>
          </div>
          <FieldError message={errorValue} />
        </label>

        <div className="flex flex-wrap items-end gap-2">
          <button
            type="button"
            disabled={locked}
            onClick={() => onMove(-1)}
            className={secondaryButtonClass}
          >
            上移
          </button>
          <button
            type="button"
            disabled={locked}
            onClick={() => onMove(1)}
            className={secondaryButtonClass}
          >
            下移
          </button>
          <button
            type="button"
            disabled={locked}
            onClick={onRemove}
            className={dangerButtonClass}
          >
            删除
          </button>
        </div>
      </div>
    </div>
  );
}

function FieldError({ message }: { message?: string }) {
  if (!message) {
    return null;
  }

  return <p className="text-xs text-rose-200">{message}</p>;
}

const inputClass =
  "w-full rounded-2xl border border-white/10 bg-stone-950/70 px-4 py-3 text-sm text-white outline-none transition placeholder:text-stone-500 focus:border-amber-300/60 disabled:cursor-not-allowed disabled:opacity-60";

const secondaryButtonClass =
  "rounded-full border border-white/10 bg-white/8 px-3 py-2 text-xs text-stone-100 transition hover:bg-white/12 disabled:cursor-not-allowed disabled:opacity-50";

const dangerButtonClass =
  "rounded-full border border-rose-300/25 px-3 py-2 text-xs text-rose-100 transition hover:bg-rose-400/10 disabled:cursor-not-allowed disabled:opacity-50";
