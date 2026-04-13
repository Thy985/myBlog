/**
 * 性能优化功能
 * 包含图片懒加载、资源预加载、字体优化等性能优化措施
 */

class PerformanceOptimizer {
  constructor() {
    this.init();
  }

  /**
   * 初始化性能优化功能
   */
  init() {
    this.initLazyLoading();
    this.initCriticalCSS();
    this.initResourceHints();
    this.initImageOptimization();
    this.optimizeFontLoading();
    this.optimizeJavaScript();
    this.optimizePageLoad();
    this.initNetworkOptimization();
  }

  /**
   * 初始化图片懒加载
   */
  initLazyLoading() {
    // 检查浏览器是否支持IntersectionObserver
    if ('IntersectionObserver' in window) {
      const imageObserver = new IntersectionObserver((entries, observer) => {
        entries.forEach(entry => {
          if (entry.isIntersecting) {
            const image = entry.target;
            this.loadImage(image);
            observer.unobserve(image);
          }
        });
      }, {
        root: null,
        rootMargin: '200px 0px',
        threshold: 0.1
      });

      // 观察所有带有data-src属性的图片
      document.querySelectorAll('img[data-src]').forEach(img => {
        // 添加占位符样式
        img.classList.add('lazy-loading');
        imageObserver.observe(img);
      });
    } else {
      // 降级方案：直接加载所有图片
      document.querySelectorAll('img[data-src]').forEach(img => {
        this.loadImage(img);
      });
    }
  }

  /**
   * 加载图片
   * @param {HTMLImageElement} img 图片元素
   */
  loadImage(img) {
    const src = img.getAttribute('data-src');
    if (src) {
      // 添加加载状态
      img.classList.add('lazy-loading');
      
      // 图片加载完成处理
      img.onload = () => {
        img.classList.remove('lazy-loading');
        img.classList.add('lazy-loaded');
      };
      
      // 图片加载错误处理
      img.onerror = () => {
        img.classList.remove('lazy-loading');
        img.classList.add('lazy-error');
        // 可以设置默认错误图片
        // img.src = '/assets/images/error-image.png';
      };
      
      img.src = src;
      img.removeAttribute('data-src');
    }
  }

  /**
   * 初始化关键CSS
   * 关键CSS已经内联到HTML中，这里可以添加动态加载非关键CSS的逻辑
   */
  initCriticalCSS() {
    // 动态加载非关键CSS
    const loadCSS = (href) => {
      const link = document.createElement('link');
      link.rel = 'stylesheet';
      link.href = href;
      link.media = 'print';
      link.onload = () => {
        link.media = 'all';
      };
      link.onerror = () => {
        console.error(`Failed to load CSS: ${href}`);
      };
      document.head.appendChild(link);
    };

    // 这里可以添加需要懒加载的CSS文件
    // 例如：loadCSS('/assets/css/non-critical.css');
  }

  /**
   * 初始化资源提示
   * 添加preload、prefetch等资源提示
   */
  initResourceHints() {
    // 预加载关键资源
    const preloadResources = [
      {
        rel: 'preload',
        href: '/assets/js/vendor-CK3j12Sk.js',
        as: 'script'
      },
      {
        rel: 'preload',
        href: '/assets/js/ui-DCYccqnm.js',
        as: 'script'
      },
      {
        rel: 'preload',
        href: '/assets/js/utils-Bn4JO2lH.js',
        as: 'script'
      },
      {
        rel: 'preload',
        href: '/assets/images/carousel1-B4hKT_PB.webp',
        as: 'image'
      }
    ];

    preloadResources.forEach(resource => {
      const link = document.createElement('link');
      link.rel = resource.rel;
      link.href = resource.href;
      link.as = resource.as;
      if (resource.type) {
        link.type = resource.type;
      }
      if (resource.crossorigin) {
        link.crossorigin = resource.crossorigin;
      }
      document.head.appendChild(link);
    });

    // 预获取可能会用到的资源
    const prefetchResources = [
      '/assets/js/theme-switcher.js',
      '/assets/js/performance-optimizer.js'
    ];

    prefetchResources.forEach(resource => {
      const link = document.createElement('link');
      link.rel = 'prefetch';
      link.href = resource;
      document.head.appendChild(link);
    });

    // 预连接重要域名
    const preconnectDomains = [
      {
        href: 'https://fonts.googleapis.com'
      },
      {
        href: 'https://fonts.gstatic.com',
        crossorigin: true
      }
    ];

    preconnectDomains.forEach(domain => {
      const link = document.createElement('link');
      link.rel = 'preconnect';
      link.href = domain.href;
      if (domain.crossorigin) {
        link.crossorigin = domain.crossorigin;
      }
      document.head.appendChild(link);
    });
  }

  /**
   * 初始化图片优化
   * 包括响应式图片和WebP格式支持
   */
  initImageOptimization() {
    // 检查浏览器是否支持WebP
    this.checkWebPSupport().then(support => {
      if (support) {
        // 替换图片为WebP格式
        document.querySelectorAll('img[data-webp]').forEach(img => {
          const webpSrc = img.getAttribute('data-webp');
          if (webpSrc) {
            img.src = webpSrc;
          }
        });
      }
    });

    // 响应式图片处理
    this.initResponsiveImages();
  }

  /**
   * 初始化响应式图片
   */
  initResponsiveImages() {
    // 为图片添加适当的srcset和sizes属性
    document.querySelectorAll('img[data-srcset]').forEach(img => {
      const srcset = img.getAttribute('data-srcset');
      const sizes = img.getAttribute('data-sizes') || '100vw';
      
      if (srcset) {
        img.srcset = srcset;
        img.sizes = sizes;
        img.removeAttribute('data-srcset');
        img.removeAttribute('data-sizes');
      }
    });
  }

  /**
   * 检查浏览器是否支持WebP格式
   * @returns {Promise<boolean>} 是否支持WebP
   */
  checkWebPSupport() {
    return new Promise((resolve) => {
      const img = new Image();
      img.onload = () => resolve(true);
      img.onerror = () => resolve(false);
      img.src = 'data:image/webp;base64,UklGRiQAAABXRUJQVlA4IBgAAAAwAQCdASoBAAEAAwA0JaQAA3AA/vuUAAA=';
    });
  }

  /**
   * 优化字体加载
   */
  optimizeFontLoading() {
    // 字体预加载
    const fonts = [
      {
        href: 'https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700&display=swap',
        rel: 'preload',
        as: 'style'
      }
    ];

    fonts.forEach(font => {
      const link = document.createElement('link');
      link.rel = font.rel;
      link.href = font.href;
      link.as = font.as;
      link.onload = () => {
        link.rel = 'stylesheet';
      };
      document.head.appendChild(link);
    });

    // 字体显示策略：使用font-display: swap
    // 确保字体加载不会阻塞页面渲染
  }

  /**
   * 优化JavaScript执行
   */
  optimizeJavaScript() {
    // 延迟加载非关键JavaScript
    const scripts = document.querySelectorAll('script[data-defer]');
    scripts.forEach(script => {
      script.defer = true;
    });

    // 异步加载非关键JavaScript
    const asyncScripts = document.querySelectorAll('script[data-async]');
    asyncScripts.forEach(script => {
      script.async = true;
    });

    // 动态导入非关键JavaScript
    this.initDynamicImports();
  }

  /**
   * 初始化动态导入
   */
  initDynamicImports() {
    // 监听用户交互，动态加载需要的脚本
    const loadOnInteraction = () => {
      // 例如，当用户滚动到页面底部时加载评论系统
      const observer = new IntersectionObserver((entries) => {
        entries.forEach(entry => {
          if (entry.isIntersecting) {
            // 动态导入评论脚本
            // import('/assets/js/comments.js').then(module => {
            //   module.init();
            // });
            observer.disconnect();
          }
        });
      });

      const commentSection = document.querySelector('.comment-section');
      if (commentSection) {
        observer.observe(commentSection);
      }
    };

    loadOnInteraction();
  }

  /**
   * 优化页面加载速度
   */
  optimizePageLoad() {
    // 移除阻塞渲染的资源
    // 优化首屏内容
    
    // 减少首次绘制时间
    this.optimizeFirstPaint();
    
    // 优化首屏内容加载
    this.optimizeAboveTheFold();
  }

  /**
   * 优化首次绘制
   */
  optimizeFirstPaint() {
    // 确保关键CSS内联
    // 减少首屏JavaScript执行时间
    
    // 避免首屏不必要的重排重绘
    document.addEventListener('DOMContentLoaded', () => {
      // 延迟执行非关键操作
      setTimeout(() => {
        // 例如，初始化非首屏组件
      }, 1000);
    });
  }

  /**
   * 优化首屏内容
   */
  optimizeAboveTheFold() {
    // 优先加载首屏图片
    const aboveTheFoldImages = document.querySelectorAll('img[data-above-the-fold]');
    aboveTheFoldImages.forEach(img => {
      this.loadImage(img);
    });
  }

  /**
   * 初始化网络优化
   */
  initNetworkOptimization() {
    // 启用HTTP/2服务器推送（如果服务器支持）
    // 优化缓存策略
    this.optimizeCache();
  }

  /**
   * 优化缓存策略
   */
  optimizeCache() {
    // 检查本地存储是否可用
    if ('localStorage' in window) {
      // 可以在这里实现资源缓存策略
      // 例如，缓存字体、图片等静态资源
    }
  }
}

// 初始化性能优化功能
document.addEventListener('DOMContentLoaded', () => {
  new PerformanceOptimizer();
});
