// pages/login/login.js
const { http, getBaseUrl, testConnection } = require('../../utils/request')
const auth = require('../../utils/auth')

Page({
  data: {
    username: 'demo_engineer',
    password: '',
    loading: false,
    apiBase: ''
  },

  onLoad() {
    this.setData({ apiBase: getBaseUrl() })
    if (auth.isLoggedIn()) {
      wx.switchTab({ url: '/pages/index/index' })
    }
  },

  onUsernameInput(event) {
    this.setData({ username: event.detail.value })
  },

  onPasswordInput(event) {
    this.setData({ password: event.detail.value })
  },

  async onLogin() {
    const { username, password } = this.data
    if (!username || !password) {
      wx.showToast({ title: '请输入账号和密码', icon: 'none' })
      return
    }
    this.setData({ loading: true })
    try {
      const data = await http.post('/mobile/auth/login', { username, password }, { loadingText: '登录中' })
      auth.setToken(data.accessToken)
      auth.setRefreshToken(data.refreshToken)
      auth.setUser(data.user)
      wx.showToast({ title: '登录成功', icon: 'success' })
      setTimeout(() => wx.switchTab({ url: '/pages/index/index' }), 400)
    } catch (error) {
      console.error('[login]', error)
      wx.showModal({
        title: '登录失败',
        content: (error && error.message ? error.message : '请检查网络与后端地址') + '\n\n当前地址：' + getBaseUrl(),
        showCancel: false
      })
    } finally {
      this.setData({ loading: false })
    }
  },

  async onDiagnose() {
    wx.showLoading({ title: '检测中', mask: true })
    try {
      const result = await testConnection()
      wx.hideLoading()
      wx.showModal({
        title: '后端连接正常',
        content: '地址：' + result.baseUrl + '\n耗时：' + result.elapsed + 'ms\n状态：' + result.status + '\nJava：' + result.javaVersion,
        showCancel: false
      })
      this.setData({ apiBase: result.baseUrl })
    } catch (error) {
      wx.hideLoading()
      wx.showModal({
        title: '后端连接失败',
        content: '当前地址：' + getBaseUrl() + '\n\n' + (error && error.message ? error.message : '') +
          '\n\n请确认：1) 后端已启动；2) 模拟器关闭域名校验；3) 真机使用电脑局域网 IP 且同一 Wi-Fi；4) 防火墙放行 8080。',
        showCancel: false
      })
    }
  }
})
