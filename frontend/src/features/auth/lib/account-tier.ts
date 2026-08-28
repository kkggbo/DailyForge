export type AccountTierMeta = {
  label: string;
  description: string;
};

const accountTierMetaMap: Record<string, AccountTierMeta> = {
  basic: { label: "普通用户", description: "暂未开通 AI 功能" },
  invited_ai: {
    label: "AI 体验版",
    description: "已解锁 AI 训练模板生成与周期总结"
  },
  premium: { label: "尊享版", description: "已解锁全部功能" }
};

export function getAccountTierMeta(
  tier: string | null | undefined
): AccountTierMeta {
  if (tier && accountTierMetaMap[tier]) {
    return accountTierMetaMap[tier];
  }

  return { label: tier || "未知", description: "" };
}

export function getAccountTierLabel(tier: string | null | undefined): string {
  return getAccountTierMeta(tier).label;
}

/**
 * A short Chinese expiry hint for a time-limited granted tier, or null when the tier is
 * permanent (or already basic). For example: "将于 2026-09-25 到期".
 */
export function getAccountTierExpiryLabel(
  tier: string | null | undefined,
  expiresAt: string | null | undefined
): string | null {
  if (!tier || tier === "basic" || !expiresAt) {
    return null;
  }
  const date = new Date(expiresAt);
  if (Number.isNaN(date.getTime())) {
    return null;
  }
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");
  return `将于 ${year}-${month}-${day} 到期`;
}
