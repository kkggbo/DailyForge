import { profileTabOptions } from "../lib/profile-enums";
import type { ProfileTab } from "../types/profile";

type ProfileTabNavProps = {
  activeTab: ProfileTab;
  onChange: (tab: ProfileTab) => void;
};

export function ProfileTabNav({ activeTab, onChange }: ProfileTabNavProps) {
  return (
    <div className="inline-flex rounded-full border border-white/10 bg-black/25 p-1">
      {profileTabOptions.map((option) => (
        <button
          key={option.value}
          type="button"
          onClick={() => onChange(option.value)}
          className={[
            "rounded-full px-4 py-2 text-sm transition",
            activeTab === option.value
              ? "bg-amber-400 text-stone-950"
              : "text-stone-300 hover:bg-white/8 hover:text-white"
          ].join(" ")}
        >
          {option.label}
        </button>
      ))}
    </div>
  );
}
