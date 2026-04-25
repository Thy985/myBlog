/** @type {import('tailwindcss').Config} */
module.exports = {
  content: [
    "./index.html",
    "./src/**/*.{vue,js,ts,jsx,tsx}"
  ],
  darkMode: 'class',
  theme: {
    extend: {
      colors: {
        // 品牌颜色 - 使用CSS变量引用
        primary: {
          DEFAULT: 'var(--color-primary)',
          hover: 'var(--color-primary-hover)',
          subtle: 'var(--color-primary-subtle)',
          glow: 'var(--color-primary-glow)',
        },
        accent: {
          DEFAULT: 'var(--color-accent)',
          hover: 'var(--color-accent-hover)',
          subtle: 'var(--color-accent-subtle)',
          glow: 'var(--color-accent-glow)',
        },
        success: {
          DEFAULT: 'var(--color-success)',
          subtle: 'var(--color-success-subtle)',
          glow: 'var(--color-success-glow)',
        },
        warning: {
          DEFAULT: 'var(--color-warning)',
          subtle: 'var(--color-warning-subtle)',
          glow: 'var(--color-warning-glow)',
        },
        error: {
          DEFAULT: 'var(--color-error)',
          subtle: 'var(--color-error-subtle)',
          glow: 'var(--color-error-glow)',
        },
        // Tech主题语义化颜色
        tech: {
          bg: 'var(--tech-bg)',
          'bg-secondary': 'var(--tech-bg-secondary)',
          card: 'var(--tech-bg-card)',
          elevated: 'var(--tech-bg-elevated)',
          border: 'var(--tech-border)',
          'border-subtle': 'var(--tech-border-subtle)',
          'border-hover': 'var(--tech-border-hover)',
          text: 'var(--tech-text)',
          'text-secondary': 'var(--tech-text-secondary)',
          muted: 'var(--tech-text-muted)',
          error: 'var(--color-error)',
          primary: 'var(--color-primary)',
        },
        // 发光颜色
        glow: {
          primary: 'var(--color-primary-glow)',
          accent: 'var(--color-accent-glow)',
          success: 'var(--color-success-glow)',
        }
      },
      boxShadow: {
        // 发光阴影 - 使用CSS变量
        'glow': '0 0 20px var(--color-primary-glow)',
        'glow-sm': '0 0 10px var(--color-primary-subtle)',
        'glow-lg': '0 0 40px var(--color-primary-glow)',
        'glow-accent': '0 0 20px var(--color-accent-glow)',
        'glow-success': '0 0 20px var(--color-success-glow)',
        // 玻璃阴影
        'glass': '0 8px 32px rgba(0, 0, 0, 0.3)',
        'glass-sm': '0 4px 16px rgba(0, 0, 0, 0.2)',
      },
      animation: {
        // 旋转
        'spin-slow': 'spin 8s linear infinite',
      },
      keyframes: {
        spin: {
          '0%': { transform: 'rotate(0deg)' },
          '100%': { transform: 'rotate(360deg)' },
        },
      },
      backdropBlur: {
        'glass': '12px',
      },
      borderRadius: {
        'glass': '12px',
      },
    },
  },
  plugins: [],
  corePlugins: {
    // 取消 tailwindcss 的默认样式
    preflight: false
  }
}

