import { defineConfig, loadEnv } from 'vite'
import react from '@vitejs/plugin-react'

// https://vite.dev/config/
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')

  const isDev = mode === 'development'
  const devPort = parseInt(env.VITE_DEV_PORT || '5173', 10)
  const previewPort = parseInt(env.VITE_PREVIEW_PORT || '3000', 10)
  const apiTarget = env.VITE_DEV_PROXY_TARGET || 'http://localhost:8080'

  return {
    plugins: [react()],
    server: {
      port: devPort,
      strictPort: false,
      proxy: {
        '/api': {
          target: apiTarget,
          changeOrigin: true,
          secure: false,
        },
      },
    },
    preview: {
      port: previewPort,
      strictPort: false,
    },
    build: {
      outDir: 'dist',
      sourcemap: isDev || env.VITE_SOURCEMAP === 'true',
      minify: !isDev,
      chunkSizeWarningLimit: 1000,
    },
  }
})
