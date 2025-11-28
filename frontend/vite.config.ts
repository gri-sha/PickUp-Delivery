import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import { visualizer } from "rollup-plugin-visualizer";

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [
    react(), 
    visualizer({ open: true }) // This will open a chart in your browser after build
  ],
  build: {
    rollupOptions: {
      output: {
        manualChunks: {
          // Split React and React DOM into a separate chunk
          vendor: ['react', 'react-dom'],
          // Split Leaflet (map library) into a separate chunk
          leaflet: ['leaflet', 'react-leaflet'],
          // Split Lucide icons if they are heavy
          icons: ['lucide-react']
        }
      }
    },
    // Optional: Increase the warning limit if you are okay with slightly larger chunks
    chunkSizeWarningLimit: 1000
  },
  server: {
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      }
    }
  }
})
