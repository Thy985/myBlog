/**
 * 全局指令注册
 */
import lazyDirective from './lazy'
import { codeCopyDirective } from './codeCopy'
import { codeLangDirective } from './codeLang'

export function setupDirectives(app) {
  // 注册懒加载指令
  app.directive('lazy', lazyDirective)

  // 注册代码复制指令
  app.directive('code-copy', codeCopyDirective)

  // 注册代码语言标签指令
  app.directive('code-lang', codeLangDirective)

  // 可以在这里注册更多全局指令
}

export { lazyDirective, codeCopyDirective, codeLangDirective }
