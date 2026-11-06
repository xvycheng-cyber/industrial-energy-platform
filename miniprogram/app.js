const { config } = require('./utils/config')

App({
  globalData: {
    appName: config.appName,
    version: config.version,
    baseUrl: config.baseUrl,
    launchTime: Date.now()
  },

  onLaunch() {
    // 启动时清理过期时间戳，token 本身由 utils/auth 管理
    console.info('[智能耗] 小程序启动，后端地址：', config.baseUrl)
  },

  onError(error) {
    console.error('[智能耗] 未捕获错误：', error)
  }
})
