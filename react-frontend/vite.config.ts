import { defineConfig } from "vitest/config";
import react from "@vitejs/plugin-react";
import tailwindcss from "@tailwindcss/vite";
import { tanstackRouter } from "@tanstack/router-plugin/vite";
import path from "path";
import { visualizer } from "rollup-plugin-visualizer";

export default defineConfig({
  plugins: [
    tanstackRouter({
      target: "react",
      autoCodeSplitting: true,
    }),
    visualizer({
      filename: "bundle-analysis.html",
      open: false,
    }),

    react(),
    tailwindcss(),
  ],
  server: {
    port: 3000,
  },
  resolve: {
    alias: {
      "@": path.resolve(__dirname, "./src"),
      "@components": path.resolve(__dirname, "./src/components"),
      "@hooks": path.resolve(__dirname, "./src/hooks"),
      "@api": path.resolve(__dirname, "./src/api"),
      "@store": path.resolve(__dirname, "./src/store"),
      "@types": path.resolve(__dirname, "./src/types"),
      "@lib": path.resolve(__dirname, "./src/lib"),
      "@pages": path.resolve(__dirname, "./src/pages"),
    },
  },
  build: {
    chunkSizeWarningLimit: 600,
    rollupOptions: {
      output: {
        manualChunks(id) {
          if (id.includes("@sentry")) return "sentry";
          if (id.includes("@azure/msal")) return "vendor-msal";
          if (id.includes("@tanstack")) return "vendor-tanstack";
          if (id.includes("@radix-ui")) return "vendor-radix";

          // Be explicit — only react and react-dom, nothing else
          if (id.includes("node_modules/react-dom")) return "vendor-react";
          if (id.includes("node_modules/react/")) return "vendor-react";

          if (id.includes("node_modules")) return "vendor";
        },
      },
    },
  },
  test: {
    reporters: ["verbose"],
    globals: true,
    environment: "jsdom",
    setupFiles: "./tests/setup.ts",
    coverage: {
      provider: "v8",
      reporter: ["text", "json", "html"],
      exclude: [
        "node_modules/",
        "tests/",
        "**/*.d.ts",
        "**/*.config.*",
        "**/mockData",
      ],
    },
  },
});
