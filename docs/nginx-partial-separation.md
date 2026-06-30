# IEPS Nginx 部分动静分离部署说明

## 目标

- Nginx 作为唯一对外入口，监听 `80`
- Spring Boot 仅监听 `127.0.0.1:8080`
- Nginx 直接提供 `/static/**` 和 `/favicon.ico`
- 页面入口、业务接口、`/hub/**`、`/upload/**` 继续转发给 Spring Boot

## 仓库内文件

- Nginx 配置样例：`deploy/nginx/ieps.partial-static.conf`
- 一键启动脚本：`scripts/start-ieps.ps1`
- 一键停止脚本：`scripts/stop-ieps.ps1`
- 静态资源发布脚本：`scripts/publish-static.ps1`
- Spring Boot 监听地址：`src/main/resources/application.yml`

## 1. 发布静态资源

在项目根目录执行：

```powershell
.\scripts\publish-static.ps1
```

默认会把以下内容复制到 `D:\deploy\ieps-static`：

- `src/main/resources/static/static/` -> `D:\deploy\ieps-static\static\`
- `src/main/resources/static/favicon.ico` -> `D:\deploy\ieps-static\favicon.ico`

如果你要改目标目录：

```powershell
.\scripts\publish-static.ps1 -TargetRoot 'D:\custom\ieps-static'
```

## 2. 配置 Nginx

将 `deploy/nginx/ieps.partial-static.conf` 放入 Nginx 的 `http {}` 配置上下文中使用。

如果你沿用默认静态目录 `D:\deploy\ieps-static`，样例无需改路径；否则请同步修改：

- `location /static/` 中的 `alias`
- `location = /favicon.ico` 中的 `alias`

关键路由分工如下：

- `/static/**`：Nginx 直接返回
- `/favicon.ico`：Nginx 直接返回
- `/hub/**`：反向代理到 Spring Boot
- `/upload/**`：反向代理到 Spring Boot
- 其余请求：反向代理到 Spring Boot

## 3. 启动 Spring Boot

当前默认配置已绑定本机地址：

```yaml
server:
  address: ${IEPS_SERVER_ADDRESS:127.0.0.1}
  port: 8080
```

直接运行即可：

```powershell
mvn spring-boot:run
```

或者：

```powershell
mvn clean package -DskipTests
java -jar target/ieps.jar
```

如果需要临时改地址：

```powershell
$env:IEPS_SERVER_ADDRESS='0.0.0.0'
mvn spring-boot:run
```

## 4. 验证清单

- 打开 `/`、`/login`、`/register`、`/index`、`/items`
- 检查 `/static/css/**`、`/static/js/**`、`/static/images/**` 返回 `200`
- 登录后刷新页面，确认会话未丢失
- 访问 `/hub/default.jpg`，确认历史资源可见
- 验证文件上传、下载、批量下载、CKEditor 上传
- 验证 `.do` 兼容接口仍可用

## 5. 后续手动启动

在完成一次部署发布后，可直接运行：

```powershell
.\scripts\start-ieps.ps1
```

停止服务：

```powershell
.\scripts\stop-ieps.ps1
```

## 6. 现阶段注意事项

- 本项目当前文件读写主路径仍是 `/hub/**`，不是统一的 `./upload/`
- 因此第一阶段不要让 Nginx 直接接管 `/hub/**`
- `/upload/**` 仍保留代理，避免与现有历史逻辑冲突
- 如果第二阶段要接管 `/hub/**`，需要先统一上传落盘目录和资源映射
