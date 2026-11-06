// pages/anomalies/anomalies.js
const { http } = require('../../utils/request')
const auth = require('../../utils/auth')

const SEVERITY = [
  { value: 0, label: '全部' },
  { value: 1, label: '低风险' },
  { value: 2, label: '中风险' },
  { value: 3, label: '高风险' },
  { value: 4, label: '严重' }
]

Page({
  data: {
    list: [],
    pageNum: 1,
    pageSize: 10,
    total: 0,
    loading: false,
    severity: 0,
    severityOptions: SEVERITY
  },

  onShow() {
    if (!auth.isLoggedIn()) {
      wx.reLaunch({ url: '/pages/login/login' })
      return
    }
    this.load(true)
  },

  onPullDownRefresh() {
    this.load(true).then(() => wx.stopPullDownRefresh())
  },

  onReachBottom() {
    if (this.data.list.length < this.data.total) {
      this.load(false)
    }
  },

  changeSeverity(event) {
    this.setData({ severity: Number(event.currentTarget.dataset.value) })
    this.load(true)
  },

  async load(reset) {
    if (this.data.loading) return
    this.setData({ loading: true })
    const pageNum = reset ? 1 : this.data.pageNum + 1
    try {
      const params = { pageNum, pageSize: this.data.pageSize }
      if (this.data.severity) params.severity = this.data.severity
      const page = await http.get('/mobile/anomalies', params)
      this.setData({
        list: reset ? page.records : this.data.list.concat(page.records),
        pageNum,
        total: page.total
      })
    } catch (error) {
      console.error('[anomalies]', error)
    } finally {
      this.setData({ loading: false })
    }
  },

  openDetail(event) {
    wx.navigateTo({ url: '/pages/anomaly-detail/anomaly-detail?id=' + event.currentTarget.dataset.id })
  }
})
