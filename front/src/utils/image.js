/**
 * 图片处理工具函数
 * 用于生成响应式图片的URL，提高页面加载速度和用户体验
 */

/**
 * 生成响应式图片的URL
 * @param {string} src - 原始图片URL
 * @param {number} width - 图片宽度
 * @param {string} format - 图片格式，默认为webp
 * @returns {string} - 响应式图片的URL
 */
export function generateResponsiveImageUrl(src, width, format = 'webp') {
  if (!src) {return ''}
  
  // 检查是否已经是响应式图片URL
  if (src.includes('width=') || src.includes('format=')) {
    return src
  }
  
  // 检查是否是相对路径
  if (src.startsWith('/')) {
    // 对于相对路径，我们可以在前端处理，或者在后端配置
    // 这里我们假设后端已经配置了响应式图片服务
    return `${src}?width=${width}&format=${format}`
  } else if (src.startsWith('http')) {
    // 对于绝对路径，我们可以在前端处理，或者在后端配置
    // 这里我们假设后端已经配置了响应式图片服务
    return `${src}?width=${width}&format=${format}`
  } else {
    // 对于相对路径（相对于src），我们可以在前端处理，或者在后端配置
    // 这里我们假设后端已经配置了响应式图片服务
    return `/src/${src}?width=${width}&format=${format}`
  }
}

/**
 * 生成图片的srcset属性
 * @param {string} src - 原始图片URL
 * @param {number[]} widths - 图片宽度数组
 * @returns {string} - 图片的srcset属性
 */
export function generateSrcset(src, widths = [320, 640, 960, 1280]) {
  if (!src) {return ''}
  
  return widths.map(width => {
    const responsiveUrl = generateResponsiveImageUrl(src, width)
    return `${responsiveUrl} ${width}w`
  }).join(', ')
}

/**
 * 生成图片的sizes属性
 * @param {string} sizes - 图片尺寸规则
 * @returns {string} - 图片的sizes属性
 */
export function generateSizes(sizes = '(max-width: 640px) 100vw, (max-width: 1280px) 50vw, 33vw') {
  return sizes
}

/**
 * 转换图片为WebP格式
 * @param {string} src - 原始图片URL
 * @returns {string} - WebP格式的图片URL
 */
export function convertToWebp(src) {
  if (!src || src === 'null' || src === 'undefined') {return ''}

  // 检查是否已经是WebP格式
  if (src.endsWith('.webp')) {
    return src
  }
  
  // 转换为WebP格式
  return src.replace(/\.(jpg|jpeg|png|gif)$/i, '.webp')
}
