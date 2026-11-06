// pages/mine/mine.js
const { http } = require('../../utils/request')
const auth = require('../../utils/auth')

Page({
  data: {
    profile: null,
    user: null,
    avatarChar: '工'
  },

  onShow() {
    if (!auth.isLoggedIn()) {
      wx.reLaunch({ url: '/pages/login/login' })
      return
    }
    this.setData({ user: auth.getUser() })
    this.load()
  },

  async load() {
    try {
      const profile = await http.get('/mobile/profile')
      const name = profile.realName || profile.username || '工程师'
      this.setData({ profile, avatarChar: name.substring(0, 1) })
    } catch (error) {
      console.error('[mine]', error)
    }
  },

  goTasks() {
    wx.switchTab({ url: '/pages/tasks/tasks' })
  },

  goAnomalies() {
    wx.switchTab({ url: '/pages/anomalies/anomalies' })
  },

  logout() {
    wx.showModal({
      title: '退出登录',
      content: '确认退出当前账号？',
      success: (res) => {
        if (res.confirm) {
          auth.clearAuth()
          wx.reLaunch({ url: '/pages/login/login' })
        }
      }
    })
  }
})
