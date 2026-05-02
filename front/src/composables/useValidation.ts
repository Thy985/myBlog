import type { FormItemRule } from 'element-plus'

const validationMessages: Record<string, string> = {
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

function getMessage(key: string, params: Record<string, string> = {}): string {
  let message = validationMessages[key] || '输入格式不正确'
  Object.keys(params).forEach(param => {
    message = message.replace(`{${param}`, params[param])
  })
  return message
}

type ValidatorCallback = (error?: Error) => void

export function useValidationRules() {
  const required = (message = getMessage('required')): FormItemRule => ({
    required: true,
    message,
    trigger: ['blur', 'change']
  })

  const username = (): FormItemRule => ({
    validator: (_rule: any, value: string, callback: ValidatorCallback) => {
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

  const email = (): FormItemRule => ({
    validator: (_rule: any, value: string, callback: ValidatorCallback) => {
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

  const phone = (): FormItemRule => ({
    validator: (_rule: any, value: string, callback: ValidatorCallback) => {
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

  const password = (): FormItemRule => ({
    validator: (_rule: any, value: string, callback: ValidatorCallback) => {
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

  const strongPassword = (): FormItemRule => ({
    validator: (_rule: any, value: string, callback: ValidatorCallback) => {
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

  const account = (): FormItemRule => ({
    validator: (_rule: any, value: string, callback: ValidatorCallback) => {
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
        callback()
      }
    },
    trigger: ['blur']
  })

  const captcha = (): FormItemRule => ({
    validator: (_rule: any, value: string, callback: ValidatorCallback) => {
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

  const minLength = (min: number, message?: string): FormItemRule => ({
    min,
    message: message || getMessage('minLength'),
    trigger: ['blur', 'change']
  })

  const maxLength = (max: number, message?: string): FormItemRule => ({
    max,
    message: message || getMessage('maxLength'),
    trigger: ['blur', 'change']
  })

  const url = (): FormItemRule => ({
    validator: (_rule: any, value: string, callback: ValidatorCallback) => {
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