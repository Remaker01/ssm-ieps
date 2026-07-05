# ssm-ieps

大学生创新创业项目管理系统（IEPS），当前为 `Spring Boot 2.7.18 + MyBatis + Layui` 的半前后端分离项目：后端提供页面路由和 JSON API，前端使用静态 HTML + AJAX 调用。

## 当前状态

- 鉴权已从旧的 `HttpSession` 方案迁移为 `JWT`。登录接口返回 token，前端通过 `src/main/resources/static/static/js/ieps-jwt.js` 统一存储、注入 `Authorization` 请求头，并在 `401` 时清理登录态和跳回登录页。
- 控制器当前用户获取方式已统一为 `request.getAttribute(Const.REQUEST_CURRENT_USER)`，由 `JwtAuthenticationFilter` 完成 token 校验和用户注入。
- Redis 目前用于忘记密码链路，而不是 Spring Session：
  - 验证码存储在 Redis，带发送冷却、过期时间、失败次数控制。
  - 验证码校验通过后，后端生成短时效 `forgetPwdToken` 写入 Redis，再用于 `/forget-password` 最终重置密码。
- 数据源连接池已切换为 `HikariCP`，配置在 `src/main/resources/application.yml`。
- JSON 处理已统一使用 Spring Boot 自带 `Jackson`，代码中通过 `ObjectMapper` 处理 JSON。
- 密码策略已升级：
  - 默认密码为 `Ieps@123`
  - 新注册、修改密码、忘记密码重置都走统一密码复杂度校验
  - 持久化密码使用 `PBKDF2WithHmacSHA256`

## 近期改造摘要

- 新增 JWT 基础设施：`JwtUtil`、`JwtAuthenticationFilter`、前端 `ieps-jwt.js`。
- 登录页、注册页、首页已支持“检测到有效 token 自动跳转 `/index`”。
- iframe 场景下的未登录跳转已统一改为 `window.top`，避免只在子页面内跳转。
- 忘记密码流程已改为后端校验验证码，不再依赖前端持有验证码明文。
- 多个前端 AJAX 调用已从旧回调形式整理为 `.done()` / `.fail()` 链式写法。
- 修复了若干前端问题，包括 `setTimetout` 拼写错误，以及 `item/applyItem.html` 内脚本区误写 HTML 注释导致 JavaScript 不执行的问题。
- `FileAdminController` 已做一轮清理和日志补强，文件预览/下载链路现在基于 Jackson 解析返回结果。
- 部署脚本已整理为仓库内 `deploy/` 目录和 `scripts/*.ps1`，支持按需重建后端、发布前端、启动 Redis、重载 Nginx。

## 运行与部署

### 本地开发

```powershell
mvn spring-boot:run
```

### 构建 JAR

```powershell
mvn clean package -DskipTests
```

### 一键启动

```powershell
pwsh -File .\scripts\start-ieps.ps1
```

脚本会按需执行这些步骤：

- 构建后端 JAR 并复制到 `deploy/app/ieps.jar`
- 发布 `src/main/resources/static/static` 到 `deploy/static`
- 检查并启动 Redis
- 启动后端服务
- 如果系统中存在 `nginx` 命令，则自动校验并启动/重载 Nginx

### 一键停止

```powershell
pwsh -File .\scripts\stop-ieps.ps1
```

## Redis 与 Nginx 说明

- 应用默认连接 `127.0.0.1:6379` 的 Redis，库号为 `1`。
- `scripts/start-ieps.ps1` 当前依赖系统可直接找到 `redis-server` 命令；如果 Redis 已经在本机 `6379` 运行，脚本会直接复用。
- Nginx 当前采用“部分动静分离”：
  - `/static/**`、`/favicon.ico` 由 Nginx 直接处理
  - 页面入口、业务接口、`/hub/**`、`/upload/**` 继续反向代理到 Spring Boot
- 如果系统里没有 `nginx` 命令，或 Nginx 校验失败，脚本会退回到仅后端模式，此时可直接访问 `http://127.0.0.1:8080/`。

可直接使用仓库内的部署文件：

- Nginx 配置样例：`deploy/nginx/ieps.partial-static.conf`
- Windows 启动脚本：`scripts/start-ieps.ps1`
- Windows 停止脚本：`scripts/stop-ieps.ps1`
- 静态资源发布脚本：`scripts/publish-static.ps1`
- 详细部署说明：`docs/nginx-partial-separation.md`
