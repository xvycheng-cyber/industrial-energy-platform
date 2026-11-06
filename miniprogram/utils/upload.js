const { config } = require('./config')
const { buildUrl } = require('./request')
const auth = require('./auth')

/**
 * 选择图片（最多 count 张），返回临时文件路径数组。
 */
function chooseImages(count) {
  return new Promise(function (resolve, reject) {
    wx.chooseMedia({
      count: count || 1,
      mediaType: ['image'],
      sizeType: ['compressed'],
      sourceType: ['album', 'camera'],
      success: function (res) {
        const files = res.tempFiles || []
        const invalid = files.filter(function (file) {
          return file.size > config.maxUploadSize
        })
        if (invalid.length > 0) {
          wx.showToast({ title: '图片不能超过 10MB', icon: 'none' })
          reject(new Error('图片超出大小限制'))
          return
        }
        resolve(
          files.map(function (file) {
            return file.tempFilePath
          })
        )
      },
      fail: function (err) {
        reject(new Error(err.errMsg || '选择图片失败'))
      }
    })
  })
}

/**
 * 上传单个文件到后端统一上传接口。
 * 后端返回 { fileId, url }（P7 阶段实现）。
 */
function uploadFile(filePath, formData) {
  return new Promise(function (resolve, reject) {
    const header = {}
    const token = auth.getToken()
    if (token) {
      header.Authorization = 'Bearer ' + token
    }

    wx.uploadFile({
      url: buildUrl(config.uploadPath),
      filePath: filePath,
      name: 'file',
      formData: formData || {},
      header: header,
      timeout: config.timeout,
      success: function (res) {
        let body = res.data
        try {
          body = JSON.parse(res.data)
        } catch (e) {
          // 非 JSON 响应按失败处理
        }
        if (res.statusCode === 401) {
          auth.clearAuth()
          auth.redirectToLogin()
          reject(new Error('登录已过期'))
          return
        }
        if (res.statusCode >= 200 && res.statusCode < 300 && body && body.code === 200) {
          resolve(body.data)
          return
        }
        reject(new Error((body && body.message) || '上传失败'))
      },
      fail: function (err) {
        reject(new Error(err.errMsg || '上传失败'))
      }
    })
  })
}

/**
 * 批量上传，任意一张失败即中断并提示。
 */
async function uploadImages(filePaths, formData) {
  const results = []
  for (let i = 0; i < filePaths.length; i += 1) {
    wx.showLoading({ title: '上传中 ' + (i + 1) + '/' + filePaths.length, mask: true })
    try {
      const uploaded = await uploadFile(filePaths[i], formData)
      results.push(uploaded)
    } catch (error) {
      wx.hideLoading()
      wx.showToast({ title: error.message || '上传失败', icon: 'none' })
      throw error
    }
  }
  wx.hideLoading()
  return results
}

module.exports = { chooseImages: chooseImages, uploadFile: uploadFile, uploadImages: uploadImages }
