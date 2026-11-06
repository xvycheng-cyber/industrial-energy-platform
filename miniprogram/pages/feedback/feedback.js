// pages/feedback/feedback.js
const { http } = require('../../utils/request')
const { config } = require('../../utils/config')
const auth = require('../../utils/auth')

Page({
  data: {
    taskId: null,
    content: '',
    beforeDesc: '',
    afterDesc: '',
    actualSaving: '',
    complete: true,
    images: [],
    uploading: false,
    submitting: false
  },

  onLoad(options) {
    this.setData({ taskId: options.id })
  },

  onContent(event) {
    this.setData({ content: event.detail.value })
  },

  onBefore(event) {
    this.setData({ beforeDesc: event.detail.value })
  },

  onAfter(event) {
    this.setData({ afterDesc: event.detail.value })
  },

  onSaving(event) {
    this.setData({ actualSaving: event.detail.value })
  },

  onComplete(event) {
    this.setData({ complete: event.detail.value })
  },

  chooseImages() {
    const remain = 9 - this.data.images.length
    if (remain <= 0) {
      wx.showToast({ title: '最多 9 张', icon: 'none' })
      return
    }
    wx.chooseMedia({
      count: remain,
      mediaType: ['image'],
      sizeType: ['compressed'],
      success: (res) => this.uploadAll(res.tempFiles)
    })
  },

  async uploadAll(files) {
    this.setData({ uploading: true })
    try {
      for (const file of files) {
        if (file.size > config.maxUploadSize) {
          wx.showToast({ title: '单张图片不能超过 5MB', icon: 'none' })
          continue
        }
        const url = await this.uploadOne(file.tempFilePath)
        this.setData({ images: this.data.images.concat(url) })
      }
    } finally {
      this.setData({ uploading: false })
    }
  },

  uploadOne(filePath) {
    return new Promise((resolve, reject) => {
      wx.uploadFile({
        url: config.baseUrl + config.uploadPath,
        filePath,
        name: 'file',
        header: { Authorization: 'Bearer ' + auth.getToken() },
        success: (res) => {
          try {
            const body = JSON.parse(res.data)
            if (body.code === 200 && body.data && body.data.url) {
              resolve(body.data.url)
            } else {
              wx.showToast({ title: body.message || '上传失败', icon: 'none' })
              reject(new Error(body.message))
            }
          } catch (error) {
            reject(error)
          }
        },
        fail: (error) => reject(error)
      })
    })
  },

  removeImage(event) {
    const index = event.currentTarget.dataset.index
    const images = this.data.images.slice()
    images.splice(index, 1)
    this.setData({ images })
  },

  preview(event) {
    wx.previewImage({ current: event.currentTarget.dataset.src, urls: this.data.images })
  },

  async submit() {
    if (!this.data.content.trim()) {
      wx.showToast({ title: '请填写整改说明', icon: 'none' })
      return
    }
    this.setData({ submitting: true })
    try {
      await http.post('/mobile/tasks/' + this.data.taskId + '/feedback', {
        content: this.data.content,
        beforeDesc: this.data.beforeDesc,
        afterDesc: this.data.afterDesc,
        actualSaving: this.data.actualSaving ? Number(this.data.actualSaving) : null,
        images: this.data.images,
        complete: this.data.complete,
        progress: this.data.complete ? 100 : 60
      })
      wx.showToast({ title: '提交成功', icon: 'success' })
      setTimeout(() => wx.navigateBack(), 500)
    } catch (error) {
      console.error('[feedback]', error)
    } finally {
      this.setData({ submitting: false })
    }
  }
})
