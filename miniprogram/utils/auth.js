const TOKEN_KEY = 'smarten_token'
const REFRESH_TOKEN_KEY = 'smarten_refresh_token'
const USER_KEY = 'smarten_user'

function getToken() {
  return wx.getStorageSync(TOKEN_KEY) || ''
}

function setToken(token) {
  wx.setStorageSync(TOKEN_KEY, token || '')
}

function getRefreshToken() {
  return wx.getStorageSync(REFRESH_TOKEN_KEY) || ''
}

function setRefreshToken(token) {
  wx.setStorageSync(REFRESH_TOKEN_KEY, token || '')
}

function getUser() {
  const user = wx.getStorageSync(USER_KEY)
  return user || null
}

function setUser(user) {
  wx.setStorageSync(USER_KEY, user || null)
}

function isLoggedIn() {
  return !!getToken()
}

function clearAuth() {
  wx.removeStorageSync(TOKEN_KEY)
  wx.removeStorageSync(REFRESH_TOKEN_KEY)
  wx.removeStorageSync(USER_KEY)
}

/**
 * 跳转登录页。
 * P7 阶段接入登录页后改为 '/pages/login/login'，当前回退到工程自检首页。
 */
function redirectToLogin() {
  wx.reLaunch({ url: '/pages/login/login' })
}

/** 需要登录的操作前调用，未登录时提示并返回 false */
function ensureLogin() {
  if (isLoggedIn()) {
    return true
  }
  wx.showToast({ title: '请先登录', icon: 'none' })
  wx.reLaunch({ url: '/pages/login/login' })
  return false
}

module.exports = {
  TOKEN_KEY,
  REFRESH_TOKEN_KEY,
  USER_KEY,
  getToken,
  setToken,
  getRefreshToken,
  setRefreshToken,
  getUser,
  setUser,
  isLoggedIn,
  clearAuth,
  redirectToLogin,
  ensureLogin
}
