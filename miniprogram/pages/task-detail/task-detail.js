// pages/task-detail/task-detail.js
const { http } = require('../../utils/request')

Page({
  data: {
    task: null,
    loading: true,
    starting: false
  },

  onLoad(options) {
    this.id = options.id
  },

  onShow() {
    this.load()
  },

  async load() {
    try {
      const task = await http.get('/mobile/tasks/' + this.id)
      this.setData({ task })
    } catch (error) {
      console.error('[task-detail]', error)
    } finally {
      this.setData({ loading: false })
    }
  },

  async start() {
    this.setData({ starting: true })
    try {
      await http.post('/mobile/tasks/' + this.id + '/start')
      wx.showToast({ title: '已开始整改', icon: 'success' })
      await this.load()
    } catch (error) {
      console.error('[task-detail] start', error)
    } finally {
      this.setData({ starting: false })
    }
  },

  goFeedback() {
    wx.navigateTo({ url: '/pages/feedback/feedback?id=' + this.id })
  },

  preview(event) {
    wx.previewImage({ current: event.currentTarget.dataset.src, urls: event.currentTarget.dataset.urls })
  }
})
