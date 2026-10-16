import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    host: '0.0.0.0',
    port: Number(process.env.FRONTEND_PORT ?? 5173),
    strictPort: true,
    proxy: {
      '/api': {
        target: `http://127.0.0.1:${Number(process.env.BACKEND_PORT ?? 8080)}`,
        changeOrigin: true,
      },
    },
    watch: {
      usePolling: true,
    },
  },
  test: {
    environment: 'jsdom',
    globals: true,
    clearMocks: true,
    exclude: ['node_modules/**', 'dist/**', 'tests/e2e/**', 'e2e/**'],
    setupFiles: './src/test/setup.js',
  },
})
