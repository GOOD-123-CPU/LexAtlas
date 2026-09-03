import pluginVue from 'eslint-plugin-vue'
import tseslint from 'typescript-eslint'

export default tseslint.config(
  // 忽略构建产物与依赖
  { ignores: ['dist/**', 'node_modules/**', '*.d.ts'] },

  // TS + Vue 文件基础规则
  ...tseslint.configs.recommended.map(config => ({
    ...config,
    files: ['**/*.ts', '**/*.vue'],
  })),

  // Vue 推荐规则集（扁平优先级）
  ...pluginVue.configs['flat/recommended'],

  {
    files: ['**/*.ts', '**/*.vue'],
    languageOptions: {
      parserOptions: {
        parser: tseslint.parser,
        sourceType: 'module',
      },
    },
    rules: {
      // 项目约定：未使用变量在开发期常见，降级为警告
      '@typescript-eslint/no-unused-vars': ['warn', { argsIgnorePattern: '^_' }],
      '@typescript-eslint/no-explicit-any': 'warn',
      // Vue 组件名需多词（Home/App 等视图组件合理豁免）
      'vue/multi-word-component-names': ['warn', { ignores: ['App', 'Home', 'Login', 'Register'] }],
      // 单行属性数量与缩进交给 Prettier 类工具，关闭冲突规则
      'vue/max-attributes-per-line': 'off',
      'vue/singleline-html-element-content-newline': 'off',
      'vue/html-self-closing': 'off',
    },
  },
)
