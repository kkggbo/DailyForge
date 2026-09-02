import { Link } from "react-router-dom";
import { mapDietMissingFields } from "../lib/diet-formatters";

type DietMissingFieldsNoticeProps = {
  missingFields: string[];
  /** 补齐后可查看的内容文案，默认「每日目标进度」 */
  targetText?: string;
};

export function DietMissingFieldsNotice({
  missingFields,
  targetText = "每日目标进度"
}: DietMissingFieldsNoticeProps) {
  const labels = mapDietMissingFields(missingFields);

  return (
    <div className="rounded-2xl border border-amber-300/20 bg-amber-300/10 px-4 py-3">
      <p className="text-sm text-amber-100">
        补充
        {labels.length > 0 ? labels.join("、") : "相关"}资料后即可查看{targetText}；不影响记录饮食。
      </p>
      <Link
        to="/profile/edit"
        className="mt-2 inline-flex rounded-full border border-amber-300/30 px-4 py-2 text-sm font-semibold text-amber-100 transition hover:bg-amber-300/10"
      >
        去补齐资料
      </Link>
    </div>
  );
}
