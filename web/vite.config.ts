import { fileURLToPath, URL } from 'node:url'
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// 开发端口与代理：/api 转发到后端（后端 context-path 为 /api）
export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  server: {
    host: '0.0.0.0',
    port: 5173,
    watch: {
      // .build-tmp 用于存放构建/浏览器临时文件，避免被文件监听器占用导致 EBUSY
      ignored: ['**/.build-tmp/**', '**/node_modules/**', '**/dist/**']
    },
    proxy: {
      '/uploads': {
        target: 'http://127.0.0.1:8080',
        changeOrigin: true
      },
      '/api': {
        target: 'http://127.0.0.1:8080',
        changeOrigin: true
      }
    }
  },
  build: {
    outDir: 'dist',
    sourcemap: false,
    target: 'esnext',
    chunkSizeWarningLimit: 1500
  }
})
