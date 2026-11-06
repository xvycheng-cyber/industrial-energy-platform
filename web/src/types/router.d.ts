import 'vue-router'

declare module 'vue-router' {
  interface RouteMeta {
    /** 页面标题 */
    title?: string
    /** 是否无需登录即可访问 */
    public?: boolean
    /** 访问所需权限码，需全部满足 */
    permissions?: string[]
  }
}
