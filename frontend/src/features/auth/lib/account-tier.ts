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
