function pad(value) {
  return value < 10 ? '0' + value : '' + value
}

/** 格式化为 yyyy-MM-dd HH:mm:ss */
function formatDateTime(input) {
  if (!input) {
    return '-'
  }
  const date = input instanceof Date ? input : new Date(String(input).replace(/-/g, '/'))
  if (isNaN(date.getTime())) {
    return String(input)
  }
  return (
    date.getFullYear() +
    '-' +
    pad(date.getMonth() + 1) +
    '-' +
    pad(date.getDate()) +
    ' ' +
    pad(date.getHours()) +
    ':' +
    pad(date.getMinutes()) +
    ':' +
    pad(date.getSeconds())
  )
}

/** 保留指定小数位，默认 2 位 */
function formatNumber(value, digits) {
  const number = Number(value)
  if (!isFinite(number)) {
    return '-'
  }
  return number.toFixed(typeof digits === 'number' ? digits : 2)
}

/** 数值转百分比展示 */
function formatPercent(value, digits) {
  const number = Number(value)
  if (!isFinite(number)) {
    return '-'
  }
  return formatNumber(number, typeof digits === 'number' ? digits : 2) + '%'
}

module.exports = {
  formatDateTime: formatDateTime,
  formatNumber: formatNumber,
  formatPercent: formatPercent
}
