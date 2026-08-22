/**
 * 生成一个 UUID v4，兼容非安全上下文（纯 HTTP + IP 访问）。
 *
 * `crypto.randomUUID()` 仅在安全上下文（HTTPS / localhost）可用；通过纯 HTTP
 * 访问时它是 undefined，调用会抛 "crypto.randomUUID is not a function"。
 * 这里回退到 `crypto.getRandomValues()`（所有上下文均可用）手动构造 UUID v4。
 */
export function generateUuid(): string {
  if (typeof crypto !== "undefined" && typeof crypto.randomUUID === "function") {
    return crypto.randomUUID();
  }

  const bytes = crypto.getRandomValues(new Uint8Array(16));
  bytes[6] = (bytes[6]! & 0x0f) | 0x40; // UUID version 4
  bytes[8] = (bytes[8]! & 0x3f) | 0x80; // RFC 4122 variant
  const hex = Array.from(bytes, (byte) =>
    byte.toString(16).padStart(2, "0")
  ).join("");
  return `${hex.slice(0, 8)}-${hex.slice(8, 12)}-${hex.slice(12, 16)}-${hex.slice(16, 20)}-${hex.slice(20)}`;
}
