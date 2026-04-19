module.exports = {
  root: true,
  env: {
    browser: true,
    es2021: true,
    node: true
  },
  globals: {
    defineProps: 'readonly',
    defineEmits: 'readonly',
    defineExpose: 'readonly',
    defineOptions: 'readonly',
    defineSlots: 'readonly',
    withDefaults: 'readonly'
  },
  extends: [
    'plugin:vue/vue3-recommended',
    'eslint:recommended',
    'prettier'
  ],
  parserOptions: {
    ecmaVersion: 'latest',
    sourceType: 'module',
    parser: '@babel/eslint-parser',
    requireConfigFile: false
  },
  plugins: ['vue'],
  rules: {
    // Vue规则
    'vue/multi-word-component-names': 'off',
    'vue/no-v-html': 'warn',
    'vue/require-default-prop': 'warn',
    'vue/require-prop-types': 'warn',
    'vue/component-name-in-template-casing': ['error', 'PascalCase'],
    'vue/custom-event-name-casing': ['error', 'camelCase', {
      ignores: [
        // Element Plus 事件（kebab-case）
        'selection-change', 'size-change', 'page-change', 'current-change',
        'prev-click', 'next-click',
        // 项目自定义事件（kebab-case）
        'quick-command', 'test-connection', 'go-home', 'go-category',
        'submit-comment', 'reply-comment', 'like-comment', 'delete-comment',
        'go-article', 'reload', 'update:visible', 'update:modelValue',
        'go-tag', 'mermaid-rendered', 'category-click', 'tag-click',
        'article-click', 'toc-click', 'load-replies', 'forgot-password',
        'social-login', 'social-register', 'generate-secret', 'verify-code',
        'generate-backup-codes', 'regenerate-backup-codes', 'save-mfa',
        'secondary-action', 'onUploadImg', 'goArticleDetail', 'goTagArticleListPage',
        'goCategoryArticleListPage', 'pageChange'
      ]
    }],
    'vue/no-unused-vars': 'warn',
    'vue/no-unused-components': 'warn',
    
    // JavaScript规则
    'no-console': process.env.NODE_ENV === 'production' ? 'warn' : 'off',
    'no-debugger': process.env.NODE_ENV === 'production' ? 'error' : 'off',
    'no-unused-vars': ['warn', { 
      argsIgnorePattern: '^_',
      varsIgnorePattern: '^_'
    }],
    'no-var': 'error',
    'prefer-const': 'warn',
    'prefer-arrow-callback': 'warn',
    'arrow-spacing': 'error',
    'comma-dangle': ['error', 'never'],
    'semi': ['error', 'never'],
    'quotes': ['error', 'single', { avoidEscape: true }],
    'indent': ['error', 2, { SwitchCase: 1 }],
    'no-multiple-empty-lines': ['error', { max: 1, maxEOF: 0 }],
    'eol-last': ['error', 'always'],
    'object-curly-spacing': ['error', 'always'],
    'array-bracket-spacing': ['error', 'never'],
    'space-before-function-paren': ['error', {
      anonymous: 'always',
      named: 'never',
      asyncArrow: 'always'
    }],
    
    // 最佳实践
    'eqeqeq': ['error', 'always'],
    'curly': ['error', 'all'],
    'no-eval': 'error',
    'no-implied-eval': 'error',
    'no-with': 'error',
    'no-new-func': 'error',
    'no-return-await': 'error',
    'require-await': 'warn',
    
    // 禁止硬编码颜色值
    'no-restricted-syntax': [
      'error',
      {
        selector: 'Literal[value=/#[0-9a-fA-F]{3,8}/]',
        message: '禁止使用硬编码颜色值，请使用CSS变量'
      }
    ]
  },
  overrides: [
    {
      files: ['*.vue'],
      rules: {
        'indent': 'off',
        'no-restricted-syntax': 'off'
      }
    }
  ],
  ignorePatterns: [
    'node_modules/',
    'dist/',
    'build/',
    '*.min.js'
  ]
}
