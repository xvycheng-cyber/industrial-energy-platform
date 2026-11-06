const { config } = require('./config')
const auth = require('./auth')

const API_BASE_KEY = 'smarten_api_base'

function normalize(base) {
  return (base || '').replace(/\/+$/, '')
}

function getStoredBase() {
  try {
    return normalize(wx.getStorageSync(API_BASE_KEY) || '')
  } catch (e) {
    return ''
  }
}

function rememberBase(base) {
  try {
    wx.setStorageSync(API_BASE_KEY, normalize(base))
  } catch (e) {
    // 存储失败不影响请求
  }
}

function currentPlatform() {
  try {
    return (wx.getSystemInfoSync() && wx.getSystemInfoSync().platform) || ''
  } catch (e) {
    return ''
  }
}

function candidateBases() {
  const stored = getStoredBase()
  const fallbacks = (config.fallbackBaseUrls || []).map(normalize)
  // 真机优先局域网地址；开发者工具优先 127.0.0.1
  const list = currentPlatform() === 'devtools'
    ? [stored, config.baseUrl].concat(fallbacks)
    : [stored].concat(fallbacks, [config.baseUrl])
  const result = []
  list.forEach(function (base) {
    const normalized = normalize(base)
    if (normalized && result.indexOf(normalized) === -1) {
      result.push(normalized)
    }
  })
  return result
}

function getBaseUrl() {
  return getStoredBase() || normalize(config.baseUrl)
}

function buildUrl(url, base) {
  if (/^https?:\/\//.test(url)) {
    return url
  }
  const prefix = normalize(base) || getBaseUrl()
  return prefix + (url.indexOf('/') === 0 ? url : '/' + url)
}

function buildError(message, code) {
  const error = new Error(message)
  error.code = code
  return error
}

function wxRequest(base, settings) {
  return new Promise(function (resolve, reject) {
    const header = Object.assign(
      { 'Content-Type': 'application/json' },
      settings.header || {}
    )
    const token = auth.getToken()
    if (token) {
      header.Authorization = 'Bearer ' + token
    }
    wx.request({
      url: buildUrl(settings.url, base),
      method: settings.method || 'GET',
      data: settings.data || {},
      header: header,
      timeout: config.timeout,
      success: resolve,
      fail: reject
    })
  })
}

function handleResponse(res) {
  const body = res.data || {}
  if (res.statusCode === 401) {
    auth.clearAuth()
    wx.showToast({ title: body.message || '登录已过期', icon: 'none' })
    auth.redirectToLogin()
    throw buildError(body.message || '未登录', 401)
  }
  if (res.statusCode >= 200 && res.statusCode < 300 && body.code === 200) {
    return body.data
  }
  const message = body.message || ('请求失败（' + res.statusCode + '）')
  throw buildError(message, body.code || res.statusCode)
}

/**
 * 统一请求：网络失败时按候选地址重试，业务错误不重试。
 */
async function request(settings) {
  const options = settings || {}
  const showError = options.showError !== false
  const loading = !!options.loading
  if (loading) {
    wx.showLoading({ title: options.loadingText || '加载中', mask: true })
  }

  const bases = candidateBases()
  let lastError = null

  try {
    for (let i = 0; i < bases.length; i += 1) {
      const base = bases[i]
      let res
      try {
        res = await wxRequest(base, options)
      } catch (err) {
        lastError = err
        console.warn('[request] network fail', base, err && err.errMsg)
        continue
      }

      rememberBase(base)
      try {
        return handleResponse(res)
      } catch (error) {
        if (showError) {
          wx.showToast({ title: error.message, icon: 'none' })
        }
        throw error
      }
    }

    const detail = (lastError && lastError.errMsg) || '无法连接后端'
    const message = '网络异常：' + detail + '（已尝试 ' + bases.join('、') + '）'
    console.error('[request] all bases failed', bases, lastError)
    if (showError) {
      wx.showToast({ title: '网络异常，请检查后端地址', icon: 'none' })
    }
    throw buildError(message, 0)
  } finally {
    if (loading) {
      wx.hideLoading()
    }
  }
}

async function testConnection() {
  const started = Date.now()
  const health = await request({ url: '/system/health', showError: false })
  return {
    baseUrl: getBaseUrl(),
    elapsed: Date.now() - started,
    status: health && health.status,
    javaVersion: health && health.javaVersion,
    serverTime: health && health.serverTime
  }
}

const http = {
  get: function (url, params, options) {
    return request(Object.assign({}, options, { url: url, method: 'GET', data: params }))
  },
  post: function (url, data, options) {
    return request(Object.assign({}, options, { url: url, method: 'POST', data: data }))
  },
  put: function (url, data, options) {
    return request(Object.assign({}, options, { url: url, method: 'PUT', data: data }))
  },
  del: function (url, params, options) {
    return request(Object.assign({}, options, { url: url, method: 'DELETE', data: params }))
  }
}

module.exports = {
  request: request,
  buildUrl: buildUrl,
  getBaseUrl: getBaseUrl,
  testConnection: testConnection,
  http: http
}
