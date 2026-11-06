/**
 * 小程序全局配置。
 * 模拟器与真机都可能无法直接访问 127.0.0.1：
 * - 模拟器：一般可用 127.0.0.1，但需关闭域名校验；
 * - 真机：127.0.0.1 指向手机自身，必须使用电脑局域网 IP。
 * request.js 会按 baseUrl → fallbackBaseUrls 顺序自动重试，并记住可用的地址。
 */
const config = {
  appName: '智能耗',
  version: '1.0.0',
  baseUrl: 'http://127.0.0.1:8080/api',
  fallbackBaseUrls: [
    'http://192.168.0.2:8080/api'
  ],
  timeout: 8000,
  uploadPath: '/mobile/upload',
  maxUploadSize: 5 * 1024 * 1024,
  allowedImageTypes: ['jpg', 'jpeg', 'png', 'webp']
}

module.exports = { config }
