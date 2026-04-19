/**
 * 表单验证规则 composable
 * 提供可复用的验证规则
 */

/**
 * 验证消息映射 - 用户友好的消息
 */
const validationMessages = {
  required: '此项为必填项',
  username: '用户名长度应在3-20个字符之间，只能包含字母、数字和下划线',
  email: '请输入有效的邮箱地址',
  phone: '请输入有效的手机号码',
  password: '密码长度至少为6个字符',
  passwordWeak: '密码应包含字母和数字',
  captcha: '验证码长度应为4-6个字符',
  url: '请输入有效的URL地址',
  number: '请输入有效的数字',
  integer: '请输入整数',
  minLength: '内容长度不足',
  maxLength: '内容长度超出限制',
  min: '数值过小',
  max: '数值过大'
}

/**
 * 获取验证消息
 * @param {string} key - 消息键
 * @param {object} params - 替换参数
 * @returns {string}
 */
function getMessage(key, params = {}) {
  let message = validationMessages[key] || '输入格式不正确'
  Object.keys(params).forEach(param => {
    message = message.replace(`{${param}`, params[param])
  })
  return message
}

/**
 * 验证规则工厂函数
 */
export function useValidationRules() {
  /**
   * 必填验证
   */
  const required = (message = getMessage('required')) => ({
    required: true,
    message,
    trigger: ['blur', 'change']
  })

  /**
   * 用户名验证（3-20个字符，字母、数字、下划线）
   */
  const username = () => ({
    validator: (rule, value, callback) => {
      if (!value) {
        callback(new Error(getMessage('required')))
      } else {
        const usernameRegex = /^[a-zA-Z0-9_]{3,20}$/
        if (!usernameRegex.test(value)) {
          callback(new Error(getMessage('username')))
        } else {
          callback()
        }
      }
    },
    trigger: ['blur', 'change']
  })

  /**
   * 邮箱验证
   */
  const email = () => ({
    validator: (rule, value, callback) => {
      if (!value) {
        callback()
      } else {
        const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
        if (!emailRegex.test(value)) {
          callback(new Error(getMessage('email')))
        } else {
          callback()
        }
      }
    },
    trigger: ['blur', 'change']
  })

  /**
   * 手机号验证
   */
  const phone = () => ({
    validator: (rule, value, callback) => {
      if (!value) {
        callback()
      } else {
        const phoneRegex = /^1[3-9]\d{9}$/
        if (!phoneRegex.test(value)) {
          callback(new Error(getMessage('phone')))
        } else {
          callback()
        }
      }
    },
    trigger: ['blur', 'change']
  })

  /**
   * 密码验证（至少6位）
   */
  const password = () => ({
    validator: (rule, value, callback) => {
      if (!value) {
        callback()
      } else if (value.length < 6) {
        callback(new Error(getMessage('password')))
      } else {
        callback()
      }
    },
    trigger: ['blur', 'change']
  })

  /**
   * 强密码验证（字母+数字）
   */
  const strongPassword = () => ({
    validator: (rule, value, callback) => {
      if (!value) {
        callback()
      } else if (value.length < 6) {
        callback(new Error(getMessage('password')))
      } else {
        const hasLetter = /[a-zA-Z]/.test(value)
        const hasNumber = /[0-9]/.test(value)
        if (!hasLetter || !hasNumber) {
          callback(new Error(getMessage('passwordWeak')))
        } else {
          callback()
        }
      }
    },
    trigger: ['blur', 'change']
  })

  /**
   * 账号验证（支持用户名/邮箱/手机号）
   */
  const account = () => ({
    validator: (rule, value, callback) => {
      if (!value) {
        callback(new Error(getMessage('required')))
      } else if (value.includes('@')) {
        const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
        if (!emailRegex.test(value)) {
          callback(new Error(getMessage('email')))
        } else {
          callback()
        }
      } else if (/^\d+$/.test(value)) {
        const phoneRegex = /^1[3-9]\d{9}$/
        if (!phoneRegex.test(value)) {
          callback(new Error(getMessage('phone')))
        } else {
          callback()
        }
      } else {
        const usernameRegex = /^[a-zA-Z0-9_]{3,20}$/
        if (!usernameRegex.test(value)) {
          callback(new Error(getMessage('username')))
        } else {
          callback()
        }
      }
    },
    trigger: ['blur', 'change']
  })

  /**
   * 验证码验证
   */
  const captcha = () => ({
    validator: (rule, value, callback) => {
      if (!value) {
        callback()
      } else if (value.length < 4 || value.length > 6) {
        callback(new Error(getMessage('captcha')))
      } else {
        callback()
      }
    },
    trigger: ['blur', 'change']
  })

  /**
   * 最小长度验证
   */
  const minLength = (min, message) => ({
    min,
    message: message || getMessage('minLength'),
    trigger: ['blur', 'change']
  })

  /**
   * 最大长度验证
   */
  const maxLength = (max, message) => ({
    max,
    message: message || getMessage('maxLength'),
    trigger: ['blur', 'change']
  })

  /**
   * URL验证
   */
  const url = () => ({
    validator: (rule, value, callback) => {
      if (!value) {
        callback()
      } else {
        try {
          new URL(value)
          callback()
        } catch {
          callback(new Error(getMessage('url')))
        }
      }
    },
    trigger: ['blur', 'change']
  })

  return {
    required,
    username,
    email,
    phone,
    password,
    strongPassword,
    account,
    captcha,
    minLength,
    maxLength,
    url,
    getMessage
  }
}
