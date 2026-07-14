import { useEffect, useState, type FormEvent, type ReactNode } from "react";
import type {
  BodyMetricFormValues,
  BodyMetricSnapshotResponse,
  CreateBodyMetricPayload
} from "../types/profile";
import {
  createDefaultBodyMetricFormValues,
  getLocalTodayDateString,
  toBodyMetricFormValues
} from "../lib/profile-mappers";

type BodyMetricFormProps = {
  title?: string;
  description?: string;
  initialValue?: BodyMetricSnapshotResponse | null;
  submitLabel: string;
  submitSuccessMessage?: string;
  isSubmitting: boolean;
  allowEmptySubmit?: boolean;
  showClearAction?: boolean;
  onSubmitEmpty?: () => Promise<void> | void;
  onSubmit: (payload: CreateBodyMetricPayload) => Promise<void>;
};

type FieldErrorMap = Partial<Record<keyof BodyMetricFormValues, string>>;

export function BodyMetricForm({
  title = "录入身体指标",
  description = "记录日期会自动使用你提交当天的本地日期，其余字段按掌握情况填写即可，但至少需要填写一个身体指标。",
  initialValue,
  submitLabel,
  submitSuccessMessage = "身体指标已记录",
  isSubmitting,
  allowEmptySubmit = false,
  showClearAction = false,
  onSubmitEmpty,
  onSubmit
}: BodyMetricFormProps) {
  const [form, setForm] = useState<BodyMetricFormValues>(() =>
    initialValue ? toBodyMetricFormValues(initialValue) : createDefaultBodyMetricFormValues()
  );
  const [fieldErrors, setFieldErrors] = useState<FieldErrorMap>({});
  const [formError, setFormError] = useState<string | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);
  const showNoteField = hasAtLeastOneMetric(form);

  useEffect(() => {
    setForm(
      initialValue ? toBodyMetricFormValues(initialValue) : createDefaultBodyMetricFormValues()
    );
    setFieldErrors({});
    setFormError(null);
    setSuccessMessage(null);
  }, [initialValue]);

  function updateField<K extends keyof BodyMetricFormValues>(
    key: K,
    value: BodyMetricFormValues[K]
  ) {
    setForm((previous) => ({
      ...previous,
      [key]: value
    }));
    setFieldErrors((previous) => ({
      ...previous,
      [key]: undefined
    }));
    setFormError(null);
    setSuccessMessage(null);
  }

  function handleClear() {
    setForm(createDefaultBodyMetricFormValues());
    setFieldErrors({});
    setFormError(null);
    setSuccessMessage(null);
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();

    if (!hasAtLeastOneMetric(form)) {
      if (allowEmptySubmit) {
        setFieldErrors({});
        setFormError(null);
        setSuccessMessage(null);

        try {
          await onSubmitEmpty?.();
        } catch (error) {
          setFormError(error instanceof Error ? error.message : "提交失败，请稍后重试");
        }
        return;
      }

      setFieldErrors({});
      setFormError("请至少填写一个身体指标后再提交");
      setSuccessMessage(null);
      return;
    }

    const nextErrors = validateBodyMetricForm(form);

    if (Object.keys(nextErrors).length > 0) {
      setFieldErrors(nextErrors);
      setFormError(null);
      return;
    }

    setFieldErrors({});
    setFormError(null);

    try {
      await onSubmit(toBodyMetricPayload(form));
      setSuccessMessage(submitSuccessMessage);
    } catch (error) {
      setFormError(error instanceof Error ? error.message : "身体指标提交失败，请稍后重试");
    }
  }

  return (
    <section className="rounded-[32px] border border-white/10 bg-stone-950/70 p-6 shadow-[0_20px_60px_rgba(0,0,0,0.35)]">
      <div>
        <h3 className="text-2xl font-semibold text-white">{title}</h3>
        <p className="mt-2 max-w-3xl leading-7 text-stone-300">{description}</p>
      </div>

      <form className="mt-6 grid gap-5 md:grid-cols-2 xl:grid-cols-3" onSubmit={handleSubmit}>
        <MetricField label="体重（kg）" error={fieldErrors.weightKg}>
          <input
            type="number"
            min="0"
            max="9999.99"
            step="0.1"
            value={form.weightKg}
            onChange={(event) => updateField("weightKg", event.target.value)}
            className={inputClassName}
          />
        </MetricField>

        <MetricField label="体脂率（%）" error={fieldErrors.bodyFatPercent}>
          <input
            type="number"
            min="0"
            max="100"
            step="0.1"
            value={form.bodyFatPercent}
            onChange={(event) => updateField("bodyFatPercent", event.target.value)}
            className={inputClassName}
          />
        </MetricField>

        <MetricField label="BMI" error={fieldErrors.bmi}>
          <input
            type="number"
            min="0"
            max="999.99"
            step="0.1"
            value={form.bmi}
            onChange={(event) => updateField("bmi", event.target.value)}
            className={inputClassName}
          />
        </MetricField>

        <MetricField label="骨骼肌率（%）" error={fieldErrors.skeletalMusclePercent}>
          <input
            type="number"
            min="0"
            max="100"
            step="0.1"
            value={form.skeletalMusclePercent}
            onChange={(event) =>
              updateField("skeletalMusclePercent", event.target.value)
            }
            className={inputClassName}
          />
        </MetricField>

        <MetricField label="身体水分（%）" error={fieldErrors.bodyWaterPercent}>
          <input
            type="number"
            min="0"
            max="100"
            step="0.1"
            value={form.bodyWaterPercent}
            onChange={(event) => updateField("bodyWaterPercent", event.target.value)}
            className={inputClassName}
          />
        </MetricField>

        <MetricField
          label="基础代谢（kcal）"
          error={fieldErrors.basalMetabolicRateKcal}
        >
          <input
            type="number"
            min="0"
            max="999999.99"
            step="0.1"
            value={form.basalMetabolicRateKcal}
            onChange={(event) =>
              updateField("basalMetabolicRateKcal", event.target.value)
            }
            className={inputClassName}
          />
        </MetricField>

        <MetricField label="腰围（cm）" error={fieldErrors.waistCm}>
          <input
            type="number"
            min="0"
            max="9999.99"
            step="0.1"
            value={form.waistCm}
            onChange={(event) => updateField("waistCm", event.target.value)}
            className={inputClassName}
          />
        </MetricField>

        <MetricField label="臀围（cm）" error={fieldErrors.hipCm}>
          <input
            type="number"
            min="0"
            max="9999.99"
            step="0.1"
            value={form.hipCm}
            onChange={(event) => updateField("hipCm", event.target.value)}
            className={inputClassName}
          />
        </MetricField>

        <MetricField label="腰臀比" error={fieldErrors.waistHipRatio}>
          <input
            type="number"
            min="0"
            max="999.99"
            step="0.1"
            value={form.waistHipRatio}
            onChange={(event) => updateField("waistHipRatio", event.target.value)}
            className={inputClassName}
          />
        </MetricField>

        <MetricField label="身体年龄" error={fieldErrors.bodyAge}>
          <input
            type="number"
            min="0"
            max="150"
            step="1"
            value={form.bodyAge}
            onChange={(event) => updateField("bodyAge", event.target.value)}
            className={inputClassName}
          />
        </MetricField>

        <MetricField label="体型" error={fieldErrors.bodyType}>
          <input
            type="text"
            value={form.bodyType}
            onChange={(event) => updateField("bodyType", event.target.value)}
            placeholder="例如：健康、偏瘦、偏胖、强壮"
            className={inputClassName}
          />
        </MetricField>

        {showNoteField ? (
          <MetricField className="md:col-span-2 xl:col-span-3" label="备注" error={fieldErrors.note}>
            <textarea
              value={form.note}
              onChange={(event) => updateField("note", event.target.value)}
              rows={4}
              placeholder="例如：健身房体测、状态一般、饭后测量等。"
              className={`${inputClassName} resize-y`}
            />
          </MetricField>
        ) : null}

        {successMessage ? (
          <div className="md:col-span-2 xl:col-span-3 rounded-2xl border border-emerald-400/20 bg-emerald-400/10 px-4 py-3 text-sm text-emerald-200">
            {successMessage}
          </div>
        ) : null}

        {formError ? (
          <div className="md:col-span-2 xl:col-span-3 rounded-2xl border border-rose-400/20 bg-rose-400/10 px-4 py-3 text-sm text-rose-200">
            {formError}
          </div>
        ) : null}

        <div className="md:col-span-2 xl:col-span-3 flex flex-wrap gap-3">
          {showClearAction ? (
            <button
              type="button"
              onClick={handleClear}
              disabled={isSubmitting}
              className="rounded-2xl border border-white/10 px-5 py-3 text-sm text-stone-200 transition hover:bg-white/8 disabled:cursor-not-allowed disabled:opacity-60"
            >
              清空
            </button>
          ) : null}

          <button
            type="submit"
            disabled={isSubmitting}
            className="rounded-2xl bg-amber-400 px-5 py-3 font-medium text-stone-950 transition hover:bg-amber-300 disabled:cursor-not-allowed disabled:opacity-60"
          >
            {isSubmitting ? "提交中..." : submitLabel}
          </button>
        </div>
      </form>
    </section>
  );
}

type MetricFieldProps = {
  label: string;
  error?: string;
  className?: string;
  children: ReactNode;
};

function MetricField({ label, error, className, children }: MetricFieldProps) {
  return (
    <div className={className}>
      <label className="mb-2 block text-sm text-stone-300">{label}</label>
      {children}
      {error ? <p className="mt-2 text-sm text-rose-300">{error}</p> : null}
    </div>
  );
}

function validateBodyMetricForm(form: BodyMetricFormValues) {
  const errors: FieldErrorMap = {};

  validatePositiveDecimal(errors, "weightKg", form.weightKg, 0.01, 9999.99, "体重");
  validateDecimal(errors, "bodyFatPercent", form.bodyFatPercent, 0, 100, "体脂率");
  validateDecimal(errors, "bmi", form.bmi, 0, 999.99, "BMI");
  validateDecimal(
    errors,
    "skeletalMusclePercent",
    form.skeletalMusclePercent,
    0,
    100,
    "骨骼肌率"
  );
  validateDecimal(
    errors,
    "bodyWaterPercent",
    form.bodyWaterPercent,
    0,
    100,
    "身体水分"
  );
  validateDecimal(
    errors,
    "basalMetabolicRateKcal",
    form.basalMetabolicRateKcal,
    0,
    999999.99,
    "基础代谢"
  );
  validateDecimal(errors, "waistCm", form.waistCm, 0, 9999.99, "腰围");
  validateDecimal(errors, "hipCm", form.hipCm, 0, 9999.99, "臀围");
  validateDecimal(errors, "waistHipRatio", form.waistHipRatio, 0, 999.99, "腰臀比");

  if (form.bodyAge) {
    const age = Number(form.bodyAge);

    if (!Number.isInteger(age) || age < 0 || age > 150) {
      errors.bodyAge = "身体年龄必须是 0 到 150 之间的整数";
    }
  }

  if (form.bodyType.length > 32) {
    errors.bodyType = "体型不能超过 32 个字符";
  }

  if (form.note.length > 1000) {
    errors.note = "备注不能超过 1000 个字符";
  }

  return errors;
}

function hasAtLeastOneMetric(form: BodyMetricFormValues) {
  const metricKeys: Array<keyof BodyMetricFormValues> = [
    "weightKg",
    "bodyFatPercent",
    "bmi",
    "skeletalMusclePercent",
    "bodyWaterPercent",
    "basalMetabolicRateKcal",
    "waistCm",
    "hipCm",
    "waistHipRatio",
    "bodyAge",
    "bodyType"
  ];

  return metricKeys.some((key) => form[key].trim() !== "");
}

function validatePositiveDecimal(
  errors: FieldErrorMap,
  key: keyof BodyMetricFormValues,
  rawValue: string,
  min: number,
  max: number,
  label: string
) {
  if (!rawValue) {
    return;
  }

  const value = Number(rawValue);

  if (!Number.isFinite(value) || value < min || value > max) {
    errors[key] = `${label}必须在 ${min} 到 ${max} 之间`;
  }
}

function validateDecimal(
  errors: FieldErrorMap,
  key: keyof BodyMetricFormValues,
  rawValue: string,
  min: number,
  max: number,
  label: string
) {
  if (!rawValue) {
    return;
  }

  const value = Number(rawValue);

  if (!Number.isFinite(value) || value < min || value > max) {
    errors[key] = `${label}必须在 ${min} 到 ${max} 之间`;
  }
}

function toBodyMetricPayload(form: BodyMetricFormValues): CreateBodyMetricPayload {
  return {
    recordDate: getLocalTodayDateString(),
    weightKg: toNullableNumber(form.weightKg),
    bodyFatPercent: toNullableNumber(form.bodyFatPercent),
    bmi: toNullableNumber(form.bmi),
    skeletalMusclePercent: toNullableNumber(form.skeletalMusclePercent),
    bodyWaterPercent: toNullableNumber(form.bodyWaterPercent),
    basalMetabolicRateKcal: toNullableNumber(form.basalMetabolicRateKcal),
    waistCm: toNullableNumber(form.waistCm),
    hipCm: toNullableNumber(form.hipCm),
    waistHipRatio: toNullableNumber(form.waistHipRatio),
    bodyAge: form.bodyAge ? Number(form.bodyAge) : null,
    bodyType: form.bodyType.trim() || null,
    note: form.note.trim() || null
  };
}

function toNullableNumber(value: string) {
  return value ? Number(value) : null;
}

const inputClassName =
  "w-full rounded-2xl border border-white/10 bg-white/5 px-4 py-3 text-white outline-none transition placeholder:text-stone-500 focus:border-amber-300";
