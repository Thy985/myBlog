declare class Logger {
  constructor(name: string)
  prefix: string
  debug: (...args: any[]) => void
  info: (...args: any[]) => void
  log: (...args: any[]) => void
  warn: (...args: any[]) => void
  error: (...args: any[]) => void
}

declare function createLogger(name: string): Logger

declare const logger: {
  debug: (...args: any[]) => void
  info: (...args: any[]) => void
  log: (...args: any[]) => void
  warn: (...args: any[]) => void
  error: (...args: any[]) => void
}

declare const _default: Logger

export { createLogger, logger }
export default _default
