// pages/anomaly-detail/anomaly-detail.js
const { http } = require('../../utils/request')

Page({
  data: {
    item: null,
    evidence: '',
    loading: true
  },

  onLoad(options) {
    this.id = options.id
    this.load()
  },

  async load() {
    try {
      const item = await http.get('/mobile/anomalies/' + this.id)
      this.setData({
        item,
        evidence: item.evidence ? JSON.stringify(item.evidence, null, 2) : ''
      })
    } catch (error) {
      console.error('[anomaly-detail]', error)
    } finally {
      this.setData({ loading: false })
    }
  },

  back() {
    wx.navigateBack()
  }
})
