# 部署文档

> 项目：智能耗 · 工业企业能耗画像与节能诊断平台
> 适用版本：1.0.0（P8 + 全链路联调 + UI 统一）
> 本机验收环境：Windows 11、JDK 17.0.21、Maven 3.8.2、Node 24.18.0、MySQL 8.0.31

---

## 1. 组件与端口

| 组件 | 技术 | 默认端口 | 说明 |
| --- | --- | --- | --- |
| MySQL | 8.0 | 3306 | 库 `smarten_energy` / 测试库 `smarten_energy_test` |
| 后端 | Spring Boot 3.2 + JDK 17 | 8080（context-path `/api`） | 业务 API、文件上传 |
| Web | Vue 3 + Vite 构建，Nginx 托管 | 5173（开发）/ 8081（Compose） | 管理端 |
| 小程序 | 原生微信小程序 | — | 微信开发者工具导入 `miniprogram/` |

---

## 2. 本地开发启动（已实测）

### 2.1 数据库

```powershell
# 创建库（如尚未创建）
mysql -uroot -p -e "CREATE DATABASE smarten_energy DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;"
mysql -uroot -p -e "CREATE DATABASE smarten_energy_test DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;"

# 初始化（幂等，可重复执行）
mysql -uroot -p smarten_energy < backend/src/main/resources/db/schema.sql
mysql -uroot -p smarten_energy < backend/src/main/resources/db/data.sql
```

- `schema.sql`：25 张业务表（含 `sys_file` 与 P8 核算字段）。
- `data.sql`：能源类型 5、角色 4、管理员/工程师账号、诊断规则 8 条。
- 升级已有库时按顺序执行 `db/migration/V4__baseline.sql` … `V7__saving.sql`；全新部署直接使用 `schema.sql` 即可（已验证与迁移后的结构一致）。

### 2.2 后端

```powershell
$env:JAVA_HOME='D:\Dev\Java\jdk-17'
$env:Path="D:\Dev\Java\jdk-17\bin;$env:Path"
$env:SMARTEN_DB_PASSWORD='你的本机数据库密码'
$env:SMARTEN_JWT_SECRET='至少32字节的随机密钥'
cd backend
mvn -DskipTests package
java -jar target\smarten-backend.jar --spring.profiles.active=dev
```

> `dev` / `test` 配置不包含明文密码或默认 JWT 密钥，`SMARTEN_DB_PASSWORD` 与 `SMARTEN_JWT_SECRET` 必须设置；运行 `mvn test` 前同样需要设置。

- 健康检查：`GET http://127.0.0.1:8080/api/system/health`
- Swagger（dev）：`http://127.0.0.1:8080/api/swagger-ui.html`
- 默认账号：`admin / Admin@123`、`engineer / Engineer@123`

### 2.3 前端

```powershell
cd web
npm.cmd ci
npm.cmd run dev          # http://localhost:5173
# 生产构建
npm.cmd run build        # 产物 web/dist
```

Vite 开发代理：`/api`、`/uploads` → `http://127.0.0.1:8080`。

### 2.4 小程序

1. 微信开发者工具 → 导入项目 → 选择 `miniprogram/`。
2. AppID 使用测试号；详情 → 本地设置 → 勾选「不校验合法域名」。
3. 修改 `utils/config.js` 的 `baseUrl` 为可访问的后端地址（默认 `http://127.0.0.1:8080/api`）。
4. 演示工程师：`demo_engineer / Engineer@123`（联调企业），或 `engineer / Engineer@123`。

---

## 3. Docker Compose 部署（配置已提供，运行未实测）

> 本机未安装 Docker / Nginx，**未实际启动容器**，以下命令需在具备 Docker 的环境执行。

文件：

- `docker-compose.yml`
- `deploy/backend.Dockerfile`
- `deploy/web.Dockerfile`
- `deploy/nginx.conf`
- `.env.example`

### 3.1 启动

```bash
cp .env.example .env
# 修改 .env：MySQL 密码、JWT 密钥、端口、CORS 来源
docker compose up -d --build
docker compose ps
```

访问：

- Web：`http://localhost:8081`
- 后端健康：`http://localhost:8080/api/system/health`
- MySQL：`localhost:3307`（映射到容器 3306）

### 3.2 初始化行为

- MySQL 首次启动自动执行：
  - `backend/src/main/resources/db/schema.sql` → `/docker-entrypoint-initdb.d/01-schema.sql`
  - `backend/src/main/resources/db/data.sql` → `/docker-entrypoint-initdb.d/02-data.sql`
- 仅当 `mysql-data` 卷为空时执行；重建需 `docker compose down -v`（会清空数据）。
- 后端以 `prod` profile 启动：关闭 Swagger、必须提供 `SMARTEN_JWT_SECRET`、`SMARTEN_DB_*`、`SMARTEN_CORS_ORIGINS`。
- 上传目录 `uploads` 卷挂载到 `/app/uploads`，Nginx 将 `/uploads/` 反代到后端。

### 3.3 生成演示数据（可选）

```bash
# 先登录获取 token
curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"Admin@123"}'

curl -X POST "http://localhost:8080/api/demo/full-chain?days=30" \
  -H "Authorization: Bearer <accessToken>"
```

生成 1 企业 / 3 车间 / 6 产线 / 30 设备 / 40 计量点、30 天四类能源数据，并联动基线、诊断、整改、核算。数据均标记 `SIMULATOR`。

### 3.4 运维命令

```bash
docker compose logs -f backend
docker compose restart backend
docker compose down            # 保留数据卷
docker compose down -v         # 清空数据（谨慎）
```

### 3.5 生产安全清单

1. 修改 `MYSQL_ROOT_PASSWORD`，生产建议使用独立业务账号并限制权限。
2. `SMARTEN_JWT_SECRET` 使用 ≥32 字节随机值，禁止沿用示例。
3. `SMARTEN_CORS_ORIGINS` 仅填写真实前端域名。
4. 生产 profile 已关闭 Swagger；如需保留，请通过网关限制来源。
5. Nginx 前增加 HTTPS（证书挂载与 443 监听）。
6. 定期备份 `mysql-data` 与 `uploads` 卷。
7. 演示数据（`DEMO-FULL`）不得作为真实计量依据，生产环境不执行 `/api/demo/**`。

---

## 4. Nginx 配置说明

`deploy/nginx.conf`：

- `location /`：SPA 回退 `index.html`。
- `/assets/`：30 天强缓存（构建产物带 hash）。
- `/api/`：反代 `backend:8080`，保留 `/api` 前缀，超时 300s（批量导入/诊断）。
- `/uploads/`：反代后端文件访问。
- `/healthz`：容器健康检查。
- 开启 gzip；`client_max_body_size 20m` 支持凭证图片。

---

## 5. 故障排查

| 现象 | 排查 |
| --- | --- |
| 后端 401/403 | 确认 `Authorization: Bearer <token>`；角色权限码是否包含接口所需权限 |
| 前端接口 404 | 确认 Vite 代理或 Nginx `/api/` 反代，后端 context-path 为 `/api` |
| 数据库表缺失 | 重新执行 `schema.sql`；确认 `MYSQL_DATABASE` 指向正确库 |
| 上传 404 | 检查 `smarten.upload.dir` 与 `/uploads/**` 映射、卷权限 |
| 集成测试失败 | 确认 MySQL 可用、测试库存在、8080 未被占用（`SmartenApplicationTests` 会启动上下文） |
| 图表空白 | 确认接口返回数据；空数据应为空态而非报错 |

---

## 6. 验收状态

| 项 | 状态 |
| --- | --- |
| 本地 MySQL + 脚本初始化 | 已执行通过（含空库重复执行） |
| 本地后端启动 / 健康检查 | 已执行通过 |
| 本地前端 `npm ci` + `npm run build` | 已执行通过 |
| 小程序语法与页面检查 | 已执行通过（14 JS / 11 JSON / 8 页面） |
| Docker Compose 启动 | **未测试**（本机无 Docker） |
| Nginx 语法检查 | **未测试**（本机无 Nginx） |
| 微信开发者工具真机 | **未测试**（本机无开发者工具） |
