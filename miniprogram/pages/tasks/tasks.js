// pages/tasks/tasks.js
const { http } = require('../../utils/request')
const auth = require('../../utils/auth')

Page({
  data: {
    tabs: [
      { value: null, label: '全部' },
      { value: 1, label: '待整改' },
      { value: 2, label: '整改中' },
      { value: 3, label: '待审核' },
      { value: 4, label: '已完成' }
    ],
    active: null,
    list: [],
    pageNum: 1,
    pageSize: 10,
    total: 0,
    loading: false
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

  changeTab(event) {
    const value = event.currentTarget.dataset.value
    this.setData({ active: value === 'null' || value === null ? null : Number(value) })
    this.load(true)
  },

  async load(reset) {
    if (this.data.loading) return
    this.setData({ loading: true })
    const pageNum = reset ? 1 : this.data.pageNum + 1
    try {
      const params = { pageNum, pageSize: this.data.pageSize }
      if (this.data.active !== null) params.status = this.data.active
      const page = await http.get('/mobile/tasks', params)
      this.setData({
        list: reset ? page.records : this.data.list.concat(page.records),
        pageNum,
        total: page.total
      })
    } catch (error) {
      console.error('[tasks]', error)
    } finally {
      this.setData({ loading: false })
    }
  },

  openDetail(event) {
    wx.navigateTo({ url: '/pages/task-detail/task-detail?id=' + event.currentTarget.dataset.id })
  }
})
