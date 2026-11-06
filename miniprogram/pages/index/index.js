// pages/index/index.js
const { http } = require('../../utils/request')
const auth = require('../../utils/auth')

Page({
  data: {
    profile: null,
    tasks: [],
    loading: false
  },

  onShow() {
    if (!auth.isLoggedIn()) {
      wx.reLaunch({ url: '/pages/login/login' })
      return
    }
    this.load()
  },

  async load() {
    this.setData({ loading: true })
    try {
      const [profile, page] = await Promise.all([
        http.get('/mobile/profile'),
        http.get('/mobile/tasks', { pageNum: 1, pageSize: 3, status: 1 })
      ])
      this.setData({ profile, tasks: page.records || [] })
    } catch (error) {
      console.error('[index]', error)
    } finally {
      this.setData({ loading: false })
    }
  },

  goTasks() {
    wx.switchTab({ url: '/pages/tasks/tasks' })
  },

  goAnomalies() {
    wx.switchTab({ url: '/pages/anomalies/anomalies' })
  },

  goMine() {
    wx.switchTab({ url: '/pages/mine/mine' })
  },

  openTask(event) {
    const id = event.currentTarget.dataset.id
    wx.navigateTo({ url: '/pages/task-detail/task-detail?id=' + id })
  }
})
