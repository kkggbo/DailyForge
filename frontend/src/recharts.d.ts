/**
 * 临时 recharts 类型声明（仅作为本沙箱环境无法安装 recharts 时的本地兜底）。
 *
 * 当在联网环境执行 `pnpm install` / `pnpm add recharts` 后，recharts 自带的类型
 * 会优先于本声明被 TypeScript 采用（环境模块声明仅在模块无法被解析到真实文件时生效），
 * 因此本文件不影响真实构建；如需干净可随时删除。
 */
declare module "recharts" {
  export const ResponsiveContainer: import("react").ComponentType<any>;
  export const LineChart: import("react").ComponentType<any>;
  export const Line: import("react").ComponentType<any>;
  export const XAxis: import("react").ComponentType<any>;
  export const YAxis: import("react").ComponentType<any>;
  export const Tooltip: import("react").ComponentType<any>;
  export const CartesianGrid: import("react").ComponentType<any>;
  export const AreaChart: import("react").ComponentType<any>;
  export const Area: import("react").ComponentType<any>;
  export const PieChart: import("react").ComponentType<any>;
  export const Pie: import("react").ComponentType<any>;
  export const Cell: import("react").ComponentType<any>;
  export const Legend: import("react").ComponentType<any>;
}
