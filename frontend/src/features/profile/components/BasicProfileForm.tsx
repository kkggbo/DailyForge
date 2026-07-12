import { useEffect, useState, type FormEvent, type ReactNode } from "react";
import {
  genderOptions,
  goalTypeOptions,
  trainingLevelOptions
} from "../lib/profile-enums";
import { toBasicProfileFormValues } from "../lib/profile-mappers";
import type {
  BasicProfileFormValues,
  ProfileBasicResponse,
  UpdateProfileBasicPayload
} from "../types/profile";

type BasicProfileFormProps = {
  initialValue?: ProfileBasicResponse | UpdateProfileBasicPayload | null;
  title?: string;
  description?: string;
  submitLabel: string;
  submitSuccessMessage?: string;
  isSubmitting: boolean;
  onSubmit: (payload: UpdateProfileBasicPayload) => Promise<void>;
};

export function BasicProfileForm({
  initialValue,
  title = "基础档案",
  description = "这些资料会帮助系统生成更贴合你的训练与饮食建议，当前不强制一次填满。",
  submitLabel,
  submitSuccessMessage = "基础档案已保存",
  isSubmitting,
  onSubmit
}: BasicProfileFormProps) {
  const [form, setForm] = useState<BasicProfileFormValues>(() =>
    toBasicProfileFormValues(initialValue)
  );
  const [fieldErrors, setFieldErrors] = useState<
    Partial<Record<keyof BasicProfileFormValues, string>>
  >({});
  const [formError, setFormError] = useState<string | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);

  useEffect(() => {
    setForm(toBasicProfileFormValues(initialValue));
    setFieldErrors({});
    setFormError(null);
  }, [initialValue]);

  function updateField<K extends keyof BasicProfileFormValues>(
    key: K,
    value: BasicProfileFormValues[K]
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

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const nextErrors = validateBasicProfileForm(form);

    if (Object.keys(nextErrors).length > 0) {
      setFieldErrors(nextErrors);
      return;
    }

    setFieldErrors({});
    setFormError(null);

    try {
      await onSubmit(toBasicProfilePayload(form));
      setSuccessMessage(submitSuccessMessage);
    } catch (error) {
      setFormError(
        error instanceof Error ? error.message : "基础档案保存失败，请稍后重试"
      );
    }
  }

  return (
    <section className="rounded-[32px] border border-white/10 bg-stone-950/70 p-6 shadow-[0_20px_60px_rgba(0,0,0,0.35)]">
      <div className="flex items-start justify-between gap-4">
        <div>
          <h3 className="text-2xl font-semibold text-white">{title}</h3>
          <p className="mt-2 max-w-3xl leading-7 text-stone-300">{description}</p>
        </div>
      </div>

      <form className="mt-6 grid gap-5 md:grid-cols-2" onSubmit={handleSubmit}>
        <FormField label="性别" error={fieldErrors.gender}>
          <select
            value={form.gender}
            onChange={(event) => updateField("gender", event.target.value)}
            className={inputClassName}
          >
            <option value="">暂不填写</option>
            {genderOptions.map((option) => (
              <option key={option.value} value={option.value}>
                {option.label}
              </option>
            ))}
          </select>
        </FormField>

        <FormField label="出生日期" error={fieldErrors.birthDate}>
          <input
            type="date"
            value={form.birthDate}
            onChange={(event) => updateField("birthDate", event.target.value)}
            className={inputClassName}
          />
        </FormField>

        <FormField label="身高（cm）" error={fieldErrors.heightCm}>
          <input
            type="number"
            min="0.01"
            max="300"
            step="0.01"
            value={form.heightCm}
            onChange={(event) => updateField("heightCm", event.target.value)}
            placeholder="例如 178"
            className={inputClassName}
          />
        </FormField>

        <FormField label="训练目标" error={fieldErrors.goalType}>
          <select
            value={form.goalType}
            onChange={(event) => updateField("goalType", event.target.value)}
            className={inputClassName}
          >
            <option value="">暂不填写</option>
            {goalTypeOptions.map((option) => (
              <option key={option.value} value={option.value}>
                {option.label}
              </option>
            ))}
          </select>
        </FormField>

        <FormField label="训练经验" error={fieldErrors.trainingLevel}>
          <select
            value={form.trainingLevel}
            onChange={(event) => updateField("trainingLevel", event.target.value)}
            className={inputClassName}
          >
            <option value="">暂不填写</option>
            {trainingLevelOptions.map((option) => (
              <option key={option.value} value={option.value}>
                {option.label}
              </option>
            ))}
          </select>
        </FormField>

        <div className="hidden md:block" />

        <FormField
          className="md:col-span-2"
          label="伤病与注意事项"
          error={fieldErrors.injuryNotes}
        >
          <textarea
            value={form.injuryNotes}
            onChange={(event) => updateField("injuryNotes", event.target.value)}
            placeholder="例如：左膝旧伤，深蹲和跑跳类动作需要控制负荷。"
            rows={5}
            className={`${inputClassName} resize-y`}
          />
        </FormField>

        {successMessage ? (
          <div className="md:col-span-2 rounded-2xl border border-emerald-400/20 bg-emerald-400/10 px-4 py-3 text-sm text-emerald-200">
            {successMessage}
          </div>
        ) : null}

        {formError ? (
          <div className="md:col-span-2 rounded-2xl border border-rose-400/20 bg-rose-400/10 px-4 py-3 text-sm text-rose-200">
            {formError}
          </div>
        ) : null}

        <div className="md:col-span-2">
          <button
            type="submit"
            disabled={isSubmitting}
            className="rounded-2xl bg-amber-400 px-5 py-3 font-medium text-stone-950 transition hover:bg-amber-300 disabled:cursor-not-allowed disabled:opacity-60"
          >
            {isSubmitting ? "保存中..." : submitLabel}
          </button>
        </div>
      </form>
    </section>
  );
}

type FormFieldProps = {
  label: string;
  error?: string;
  className?: string;
  children: ReactNode;
};

function FormField({ label, error, className, children }: FormFieldProps) {
  return (
    <div className={className}>
      <label className="mb-2 block text-sm text-stone-300">{label}</label>
      {children}
      {error ? <p className="mt-2 text-sm text-rose-300">{error}</p> : null}
    </div>
  );
}

function validateBasicProfileForm(form: BasicProfileFormValues) {
  const errors: Partial<Record<keyof BasicProfileFormValues, string>> = {};

  if (form.heightCm) {
    const value = Number(form.heightCm);

    if (!Number.isFinite(value) || value <= 0 || value > 300) {
      errors.heightCm = "身高必须大于 0 且不超过 300 cm";
    }
  }

  if (form.injuryNotes.length > 1000) {
    errors.injuryNotes = "伤病与注意事项不能超过 1000 个字符";
  }

  return errors;
}

function toBasicProfilePayload(form: BasicProfileFormValues): UpdateProfileBasicPayload {
  return {
    gender: form.gender ? (form.gender as UpdateProfileBasicPayload["gender"]) : null,
    birthDate: form.birthDate || null,
    heightCm: form.heightCm ? Number(form.heightCm) : null,
    goalType: form.goalType
      ? (form.goalType as UpdateProfileBasicPayload["goalType"])
      : null,
    trainingLevel: form.trainingLevel
      ? (form.trainingLevel as UpdateProfileBasicPayload["trainingLevel"])
      : null,
    injuryNotes: form.injuryNotes.trim() || null
  };
}

const inputClassName =
  "w-full rounded-2xl border border-white/10 bg-white/5 px-4 py-3 text-white outline-none transition placeholder:text-stone-500 focus:border-amber-300";
