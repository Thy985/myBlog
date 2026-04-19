import nprogress from 'nprogress'

export function showPageLoading() {
  nprogress.start()
}

export function hidePageLoading() {
  nprogress.done()
}
