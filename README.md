# ssm-ieps

大学生创新创业项目管理系统（IEPS），`Spring Boot 2.7.18 + MyBatis + Layui` 半前后端分离项目。

> **Fork 自** [jiawuliang/ssm-ieps](https://github.com/jiawuliang/ssm-ieps)（1.0.1），主要改造如下：
> - **架构更新**：传统SSM → Spring Boot
> - **鉴权重构**：Session → JWT 双 Token（Access + Refresh），Redis 持久化刷新
> - **缓存替换**：Caffeine 本地缓存替代 Ehcache，提升热点数据查询性能
> - **文件上云**：对接腾讯云 COS 对象存储，支持分片上传、异步批量打包下载
> - **安全加固**：PBKDF2WithHmacSHA256 哈希密码，Redis 验证码防刷三重保护
> - **技术整理**：统一 Jackson，HikariCP，移除无用依赖；新增 Springdoc OpenAPI 接口文档
> - **部署脚本**：`deploy/` + `scripts/*.ps1`，一键启停 Redis / Nginx / 后端

## 关键配置

```yaml
# 数据源（默认 localhost:3306/ieps）
spring.datasource.url
spring.datasource.username
spring.datasource.password

# Redis（验证码防刷、Refresh Token 持久化）
spring.redis.host
spring.redis.port
spring.redis.database

# JWT 双 Token
ieps.jwt.secret              # 签名密钥
ieps.jwt.access-token-expiration   # Access Token 过期（默认 10 分钟）
ieps.jwt.refresh-token-expiration  # Refresh Token 过期（默认 7 天）

# 腾讯云 COS（文件存储）
ieps.storage.cos.secret-id
ieps.storage.cos.secret-key
ieps.storage.cos.bucket / region / app-id
```

完整配置见 `src/main/resources/application.yml`。

## API 文档

启动项目后访问：

- Swagger UI：[http://127.0.0.1:8080/docs](http://127.0.0.1:8080/docs)
- OpenAPI JSON：[http://127.0.0.1:8080/api-docs](http://127.0.0.1:8080/api-docs)

## 构建与运行

### 本地开发

```powershell
mvn spring-boot:run
```

### 构建 JAR

```powershell
mvn clean package -DskipTests
java -jar target/ieps.jar --server.port=8080
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
