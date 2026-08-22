import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";
import tailwindcss from "@tailwindcss/vite";

export default defineConfig({
  plugins: [react(), tailwindcss()],
  server: {
    host: "0.0.0.0",
    port: 5173,
    // DSH 的 write/edit 工具采用「临时文件 + 重命名」的原子写，会在 Windows 上触发
    // fs.watch 的 EBUSY 并导致 dev server 崩溃。改用轮询监听，并忽略临时文件。
    watch: {
      usePolling: true,
      ignored: ["**/*.tmpdir/**", "**/*.tmp"]
    },
    proxy: {
      "/api": {
        target: "http://localhost:8080",
        changeOrigin: true
      }
    }
  }
});
