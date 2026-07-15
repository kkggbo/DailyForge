import { MetricEditor } from "./MetricEditor";
import type {
  CycleTemplateFieldErrors,
  EditorItemForm,
  EditorMetricForm,
  StructureType
} from "../types/cycle-template";

type ExerciseItemEditorProps = {
  item: EditorItemForm;
  itemIndex: number;
  structureType: StructureType | null;
  locked: boolean;
  fieldErrors: CycleTemplateFieldErrors;
  errorPrefix: string;
  onChange: (patch: Partial<EditorItemForm>) => void;
  onAddMetric: () => void;
  onUpdateMetric: (metricLocalId: string, patch: Partial<EditorMetricForm>) => void;
  onRemoveMetric: (metricLocalId: string) => void;
  onMoveMetric: (metricLocalId: string, direction: -1 | 1) => void;
  onMoveItem: (direction: -1 | 1) => void;
  onRemoveItem: () => void;
};

export function ExerciseItemEditor({
  item,
  itemIndex,
  structureType,
  locked,
  fieldErrors,
  errorPrefix,
  onChange,
  onAddMetric,
  onUpdateMetric,
  onRemoveMetric,
  onMoveMetric,
  onMoveItem,
  onRemoveItem
}: ExerciseItemEditorProps) {
  return (
    <div className="rounded-2xl border border-white/10 bg-stone-950/45 p-4">
      <div className="flex flex-col gap-3 lg:flex-row lg:items-start lg:justify-between">
        <div>
          <p className="text-xs uppercase tracking-[0.2em] text-amber-300">
            执行项 {itemIndex + 1}
          </p>
          <p className="mt-1 text-sm text-stone-300">
            {structureType === "set_based" ? "按组动作" : "单段动作"}
          </p>
        </div>
        <div className="flex flex-wrap gap-2">
          {structureType === "set_based" ? (
            <>
              <button
                type="button"
                disabled={locked}
                onClick={() => onMoveItem(-1)}
                className={secondaryButtonClass}
              >
                上移
              </button>
              <button
                type="button"
                disabled={locked}
                onClick={() => onMoveItem(1)}
                className={secondaryButtonClass}
              >
                下移
              </button>
              <button
                type="button"
                disabled={locked}
                onClick={onRemoveItem}
                className={dangerButtonClass}
              >
                删除执行项
              </button>
            </>
          ) : null}
        </div>
      </div>

      <div className="mt-4 grid gap-3 lg:grid-cols-2">
        <label className="space-y-2">
          <span className="text-xs text-stone-300">执行项名称</span>
          <input
            value={item.itemName}
            disabled={locked}
            onChange={(event) => onChange({ itemName: event.target.value })}
            className={inputClass}
          />
          <FieldError message={fieldErrors[`${errorPrefix}.itemName`]} />
        </label>

        <label className="space-y-2">
          <span className="text-xs text-stone-300">执行项备注</span>
          <input
            value={item.note}
            disabled={locked}
            onChange={(event) => onChange({ note: event.target.value })}
            className={inputClass}
            placeholder="例如：热身组"
          />
          <FieldError message={fieldErrors[`${errorPrefix}.note`]} />
        </label>
      </div>

      <div className="mt-4 space-y-3">
        {item.metrics.map((metric, metricIndex) => {
          const metricPrefix = `${errorPrefix}.metric.${metricIndex}`;
          return (
            <MetricEditor
              key={metric.localId}
              metric={metric}
              structureType={structureType}
              locked={locked}
              errorKey={fieldErrors[`${metricPrefix}.metricKey`]}
              errorValue={fieldErrors[`${metricPrefix}.metricValueNumberText`]}
              onChange={(patch) => onUpdateMetric(metric.localId, patch)}
              onRemove={() => onRemoveMetric(metric.localId)}
              onMove={(direction) => onMoveMetric(metric.localId, direction)}
            />
          );
        })}
      </div>

      <FieldError message={fieldErrors[`${errorPrefix}.metrics`]} />

      <div className="mt-4">
        <button
          type="button"
          disabled={locked}
          onClick={onAddMetric}
          className={secondaryButtonClass}
        >
          添加指标
        </button>
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
  "rounded-full border border-white/10 bg-white/8 px-4 py-2 text-sm text-stone-100 transition hover:bg-white/12 disabled:cursor-not-allowed disabled:opacity-50";

const dangerButtonClass =
  "rounded-full border border-rose-300/25 px-4 py-2 text-sm text-rose-100 transition hover:bg-rose-400/10 disabled:cursor-not-allowed disabled:opacity-50";
