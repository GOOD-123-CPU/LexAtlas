import { fileURLToPath } from 'node:url'
import { mergeConfig, defineConfig as defineViteConfig } from 'vite'
import { defineConfig as defineVitestConfig } from 'vitest/config'
import vue from '@vitejs/plugin-vue'

// 复用 vite 主配置的别名，叠加 vitest 专属配置
const viteConfig = defineViteConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
})

export default mergeConfig(
  viteConfig,
  defineVitestConfig({
    test: {
      environment: 'node',
      include: ['src/**/*.{test,spec}.ts'],
    },
  }),
)
