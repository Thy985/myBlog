import nprogress from 'nprogress'

let requestCount = 0

export function showPageLoading() {
  requestCount++
  nprogress.start()
}

export function hidePageLoading() {
  requestCount--
  if (requestCount <= 0) {
    requestCount = 0
    nprogress.done()
  }
}

export function resetPageLoading() {
  requestCount = 0
  nprogress.done()
  nprogress.start()
}

export function getRequestCount() {
  return requestCount
}
