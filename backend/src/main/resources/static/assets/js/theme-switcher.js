/**
 * 主题切换功能
 * 支持手动切换深色/浅色模式，并持久化到localStorage
 */

class ThemeSwitcher {
  constructor() {
    this.currentTheme = this.getSavedTheme() || this.getSystemTheme();
    this.init();
  }

  /**
   * 初始化主题切换功能
   */
  init() {
    this.applyTheme(this.currentTheme);
    this.createThemeToggle();
    this.listenForSystemThemeChanges();
  }

  /**
   * 获取系统主题
   * @returns {string} 系统主题 ('light' 或 'dark')
   */
  getSystemTheme() {
    return window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light';
  }

  /**
   * 从localStorage获取保存的主题
   * @returns {string|null} 保存的主题或null
   */
  getSavedTheme() {
    return localStorage.getItem('theme');
  }

  /**
   * 保存主题到localStorage
   * @param {string} theme 主题 ('light' 或 'dark')
   */
  saveTheme(theme) {
    localStorage.setItem('theme', theme);
  }

  /**
   * 应用主题
   * @param {string} theme 主题 ('light' 或 'dark')
   */
  applyTheme(theme) {
    this.currentTheme = theme;
    this.saveTheme(theme);
    
    // 更新文档根元素的类
    document.documentElement.classList.remove('light', 'dark');
    document.documentElement.classList.add(theme);
    
    // 更新主题切换按钮的状态
    this.updateThemeToggle();
    
    // 触发主题变更事件
    window.dispatchEvent(new CustomEvent('themeChange', { detail: { theme } }));
  }

  /**
   * 切换主题
   */
  toggleTheme() {
    const newTheme = this.currentTheme === 'light' ? 'dark' : 'light';
    this.applyTheme(newTheme);
  }

  /**
   * 创建主题切换按钮
   */
  createThemeToggle() {
    // 检查是否已经存在主题切换按钮
    if (document.getElementById('theme-toggle')) {
      return;
    }

    // 创建主题切换按钮
    const toggleButton = document.createElement('button');
    toggleButton.id = 'theme-toggle';
    toggleButton.className = 'theme-toggle';
    toggleButton.setAttribute('aria-label', '切换主题');
    toggleButton.setAttribute('aria-pressed', this.currentTheme === 'dark');
    toggleButton.innerHTML = this.getThemeIcon();
    
    // 添加样式
    toggleButton.style.background = 'none';
    toggleButton.style.border = 'none';
    toggleButton.style.cursor = 'pointer';
    toggleButton.style.fontSize = '1.25rem';
    toggleButton.style.padding = '0.5rem';
    toggleButton.style.borderRadius = '50%';
    toggleButton.style.transition = 'all 0.3s ease';
    toggleButton.style.color = 'var(--text-primary)';
    
    // 添加悬停效果
    toggleButton.addEventListener('mouseenter', () => {
      toggleButton.style.backgroundColor = 'var(--background-tertiary)';
    });
    
    toggleButton.addEventListener('mouseleave', () => {
      toggleButton.style.backgroundColor = 'transparent';
    });
    
    // 添加点击事件
    toggleButton.addEventListener('click', () => {
      this.toggleTheme();
    });
    
    // 将按钮添加到导航栏或其他合适的位置
    const navbar = document.querySelector('.navbar');
    if (navbar) {
      const navbarLinks = navbar.querySelector('.navbar-links');
      if (navbarLinks) {
        navbarLinks.appendChild(toggleButton);
      } else {
        navbar.appendChild(toggleButton);
      }
    } else {
      // 如果没有导航栏，将按钮添加到页面右上角
      toggleButton.style.position = 'fixed';
      toggleButton.style.top = '1rem';
      toggleButton.style.right = '1rem';
      toggleButton.style.zIndex = '1000';
      document.body.appendChild(toggleButton);
    }
  }

  /**
   * 获取主题图标
   * @returns {string} 图标HTML
   */
  getThemeIcon() {
    if (this.currentTheme === 'dark') {
      return '<svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="5"/><line x1="12" y1="1" x2="12" y2="3"/><line x1="12" y1="21" x2="12" y2="23"/><line x1="4.22" y1="4.22" x2="5.64" y2="5.64"/><line x1="18.36" y1="18.36" x2="19.78" y2="19.78"/><line x1="1" y1="12" x2="3" y2="12"/><line x1="21" y1="12" x2="23" y2="12"/><line x1="4.22" y1="19.78" x2="5.64" y2="18.36"/><line x1="18.36" y1="5.64" x2="19.78" y2="4.22"/></svg>';
    } else {
      return '<svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z"></path></svg>';
    }
  }

  /**
   * 更新主题切换按钮的状态
   */
  updateThemeToggle() {
    const toggleButton = document.getElementById('theme-toggle');
    if (toggleButton) {
      toggleButton.innerHTML = this.getThemeIcon();
      toggleButton.setAttribute('aria-pressed', this.currentTheme === 'dark');
    }
  }

  /**
   * 监听系统主题变化
   */
  listenForSystemThemeChanges() {
    window.matchMedia('(prefers-color-scheme: dark)').addEventListener('change', (e) => {
      // 只有当用户没有手动设置主题时，才响应系统主题变化
      if (!this.getSavedTheme()) {
        const newTheme = e.matches ? 'dark' : 'light';
        this.applyTheme(newTheme);
      }
    });
  }
}

// 初始化主题切换功能
document.addEventListener('DOMContentLoaded', () => {
  new ThemeSwitcher();
});
