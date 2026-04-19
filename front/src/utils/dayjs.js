import dayjs from 'dayjs'
import relativeTime from 'dayjs/plugin/relativeTime'
import 'dayjs/locale/zh-cn'

dayjs.extend(relativeTime)
dayjs.locale('zh-cn')

export function formatTime(time, format = 'YYYY-MM-DD HH:mm:ss') {
  if (!time) {return ''}
  return dayjs(time).format(format)
}

export function fromNow(time) {
  if (!time) {return ''}
  return dayjs(time).fromNow()
}

export default dayjs
