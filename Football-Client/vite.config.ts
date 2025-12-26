import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";
import { VitePWA } from "vite-plugin-pwa";

export default defineConfig({
  plugins: [
    react(),
    VitePWA({
      registerType: 'autoUpdate',
      includeAssets: ['favicon.ico', 'logo192.png', 'logo512.png'], 
      devOptions: {
        enabled: true // This helps debug why it's not loading in dev mode
      },
      manifest: {
        name: "Football",
        short_name: "Football",
        description: "A Football game",
        start_url: "/",
        display: "standalone",
        display_override: ["fullscreen", "minimal-ui"],
        orientation: "landscape",
        background_color: "#0000FF", // match your game background
        icons: [
          { src: "logo192.png", sizes: "192x192", type: "image/png", purpose: "any" },
          { src: "logo512.png", sizes: "512x512", type: "image/png", purpose: "maskable" }
        ]
      }
    })
  ],
});