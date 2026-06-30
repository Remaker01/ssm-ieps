# ssm-ieps
1.0.1 - 基于SSM + Layui 半前后端分离的大学生创新创业项目管理系统

## Nginx 部分动静分离

- Spring Boot 默认绑定 `127.0.0.1:8080`，由 Nginx 统一对外提供访问入口。
- 第一阶段仅由 Nginx 直接处理 `/static/**` 和 `/favicon.ico`。
- 页面入口、业务接口、`/hub/**`、`/upload/**` 继续反向代理到 Spring Boot。

可直接使用仓库内的部署文件：

- Nginx 配置样例：`deploy/nginx/ieps.partial-static.conf`
- Windows 一键启动脚本：`scripts/start-ieps.ps1`
- Windows 一键停止脚本：`scripts/stop-ieps.ps1`
- 静态资源发布脚本：`scripts/publish-static.ps1`
- 详细部署说明：`docs/nginx-partial-separation.md`
