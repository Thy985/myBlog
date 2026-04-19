/**
 * 日志工具 - 统一管理日志输出
 * 生产环境自动禁用 debug/info/log level
 */

const isDev = import.meta.env.DEV
const isProd = !isDev

/**
 * 日志级别
 */
const LogLevel = {
  DEBUG: 0,
  INFO: 1,
  LOG: 2,
  WARN: 3,
  ERROR: 4
}

/**
 * 当前日志级别
 * 生产环境默认 WARN，开发环境 DEBUG
 */
const currentLevel = isProd ? LogLevel.WARN : LogLevel.DEBUG

/**
 * 格式化日志输出
 */
const formatMessage = (prefix, ...args) => {
  const timestamp = new Date().toLocaleTimeString('zh-CN', {
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
    fractionalSecondDigits: 3
  })
  return `[${timestamp}] [${prefix}]`.concat(
    args.map(arg => {
      if (typeof arg === 'object') {
        try {
          return JSON.stringify(arg)
        } catch {
          return String(arg)
        }
      }
      return String(arg)
    })
  )
}

/**
 * Logger 类
 */
class Logger {
  constructor(name) {
    this.name = name
    this.prefix = name
  }

  debug = (...args) => {
    if (currentLevel <= LogLevel.DEBUG) {
      console.debug(...formatMessage(this.prefix, ...args))
    }
  }

  info = (...args) => {
    if (currentLevel <= LogLevel.INFO) {
      console.info(...formatMessage(this.prefix, ...args))
    }
  }

  log = (...args) => {
    if (currentLevel <= LogLevel.LOG) {
      console.log(...formatMessage(this.prefix, ...args))
    }
  }

  warn = (...args) => {
    if (currentLevel <= LogLevel.WARN) {
      console.warn(...formatMessage(this.prefix, ...args))
    }
  }

  error = (...args) => {
    if (currentLevel <= LogLevel.ERROR) {
      console.error(...formatMessage(this.prefix, ...args))
    }
  }
}

/**
 * 创建 Logger 实例
 */
export const createLogger = (name) => new Logger(name)

/**
 * 默认导出 - 通用 logger
 */
export default createLogger('APP')

/**
 * 快捷方法
 */
export const logger = {
  debug: createLogger('DEBUG').debug,
  info: createLogger('INFO').info,
  log: createLogger('LOG').log,
  warn: createLogger('WARN').warn,
  error: createLogger('ERROR').error
}
