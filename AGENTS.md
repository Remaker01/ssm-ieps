# IEPS - Innovation and Entrepreneurship Project Management System

## 项目概述

大学生创新创业项目管理系统（IEPS），采用 **Spring Boot 2.7.18 + MyBatis + Layui** 半前后端分离架构。前端为静态 HTML 页面（无 JSP/模板引擎），通过 AJAX 调用后端 RESTful API。

---

## 技术栈

| 层 | 技术 | 说明 |
|---|------|------|
| **后端框架** | Spring Boot 2.7.18 | Java 11, 内嵌 Tomcat, JAR 打包 |
| **ORM** | MyBatis + mybatis-spring-boot-starter 2.3.0 | XML 映射文件在 `src/main/resources/mappers/` |
| **分页** | PageHelper (pagehelper-spring-boot-starter 1.4.6) | |
| **连接池** | Druid (druid-spring-boot-starter 1.2.16) | 监控台 `/druid/index.html` (admin/admin) |
| **数据库** | MySQL 8.0+ | 数据库名 `ieps` |
| **前端** | Layui 2.7 + jQuery 3.7.1 + Bootstrap 4.6 | 纯静态 HTML，CDN 引用 |
| **验证码** | Kaptcha (Google) | |
| **缓存** | EhCache | |

---

## 项目结构

```
ssm-ieps/
├── pom.xml                          # Maven 构建 (Spring Boot Parent)
├── AGENTS.md                        # 本文件 - Agent 指导
├── src/main/java/com/ieps/
│   ├── IepsApplication.java         # Spring Boot 主启动类
│   ├── config/                      # Java 配置类
│   │   ├── WebMvcConfig.java        # 视图解析器 + 静态资源
│   │   └── KaptchaConfig.java       # 验证码 Bean
│   ├── controller/                  # 控制器 (11个)
│   ├── service/                     # Service 层 (合并后的具体类)
│   ├── mapper/                      # MyBatis Mapper 接口
│   ├── pojo/                        # 实体类 (13个)
│   ├── dto/                         # 数据传输对象
│   ├── common/                      # 公共类
│   │   ├── ServerResponse.java      # 统一响应封装
│   │   └── Const.java               # 常量定义
│   ├── enums/                       # 枚举
│   ├── exception/                   # 自定义异常
│   └── util/                        # 工具类
├── src/main/resources/
│   ├── application.yml              # Spring Boot 主配置
│   ├── mappers/                     # MyBatis XML 映射文件 (12个)
│   ├── ehcache/                     # EhCache 缓存配置
│   ├── static/                      # 前端静态资源
│   │   ├── static/                  # CSS/JS/图片 (嵌套结构)
│   │   ├── pages/                   # HTML 页面
│   │   └── hub/                     # 上传文件目录
│   └── generator/                   # MyBatis Generator 配置
└── upload/                          # 文件上传目录 (运行态)
```

---

## 核心约定

### 1. 后端架构模式

**Controller → Service → Mapper (MyBatis XML)**

- **Controller**: 纯 `@Controller` + `@ResponseBody`，返回 `ServerResponse<T>` 统一格式
- **Service**: 具体类（无接口），标注 `@Service`，注入 Mapper
- **Mapper**: 接口 + XML 映射，标注 `@Mapper`（由 `@MapperScan` 扫描）
- **返回格式**: 所有 API 返回 `com.ieps.common.ServerResponse<T>`
  ```java
  ServerResponse.createBySuccess(data);
  ServerResponse.createBySuccessMessage("成功");
  ServerResponse.createByErrorMessage("失败");
  ```

### 2. 路由风格

- 全部使用 `.do` 后缀：`@RequestMapping("/login.do")`
- 方法类型：`GET` 查询 / `POST` 操作
- 视图跳转：返回字符串，由 `InternalResourceViewResolver` 解析为 `/pages/{name}.html`

### 3. 数据访问

- MyBatis XML 映射文件在 `src/main/resources/mappers/`
- 实体类在 `com.ieps.pojo`，DTO 在 `com.ieps.dto`
- 分页：统一使用 `PageHelper.startPage(pageNum, pageSize)` + `PageInfo`
- 分页参数命名：`page` (页码), `limit` (每页条数)

### 4. 事务管理

- 通过 `@EnableTransactionManagement` + AOP 实现声明式事务
- Service 层方法命名约定：
  - `save*/add*/insert*/delete*/remove*/update*/modify*` → REQUIRED (默认事务)
  - `select*/find*/get*` → SUPPORTS (只读事务)

### 5. 安全校验

- 用户信息存储在 `HttpSession` 中，key 为 `"activeUser"`
- 控制器通过 `session.getAttribute("activeUser")` 获取当前用户
- 角色 ID 定义在 `Const.java` 中：
  - `ROLEID_STU = 200001` (学生)
  - `ROLEID_TUTOR = 200002` (指导老师)
  - `ROLEID_ACADEMY_EXPERT = 200003` (院内专家)
  - `ROLEID_ACADEMY = 200004` (学院管理员)
  - `ROLEID_COLLEGE_EXPERT = 200005` (校内专家)
  - `ROLEID_COLLEGE = 200006` (学校管理员)

### 6. 项目状态

```
1: 申请中 → 2: 立项评审 → 3: 已立项 → 4: 立项失败
→ 5: 中期检查 → 6: 待结题 → 7: 结题评审 → 8: 结题成功 → 9: 结题失败
```

### 7. 评审级别

```
1: 院级评审 → 2: 校级评审 → 3: 省区级评审 → 4: 国家级评审
```

### 8. 用户身份标识 (UserItem)

```
1: 成员 / 2: 负责人 / 3: 指导老师 / 4: 院内评委
5: 院内评委组长 / 6: 校内评委 / 7: 校内评委组长
```

---

## 构建与运行

```bash
# 开发运行
mvn spring-boot:run

# 构建 JAR
mvn clean package -DskipTests

# 运行 JAR
java -jar target/ieps.jar

# 指定端口
java -jar target/ieps.jar --server.port=8080
```

---

## 数据库

- **数据库名**: `ieps`
- **连接**: `jdbc:mysql://localhost:3306/ieps?characterEncoding=utf-8&useSSL=false&serverTimezone=Asia/Shanghai`
- **初始化 SQL**: 项目根目录 `ieps.sql`
- **表前缀**: `ieps_` (如 `ieps_user`, `ieps_perm`)

---

## 关键配置

### application.yml 核心配置项

```yaml
# 数据源 (默认 localhost:3306/ieps)
spring.datasource.url
spring.datasource.username
spring.datasource.password

# 文件上传路径 (上传文件实际存储在服务器)
# application.yml 中 upload/ 路径映射
spring.web.resources.static-locations[2]: file:./upload/
```

### 静态资源路径说明

HTML 页面中引用静态资源使用 `../../static/...` 路径格式：
- `/pages/common/login.html` → `../../static/css/background.css` → `/static/css/background.css`
- 实际文件位于 `src/main/resources/static/static/css/background.css`

---

## 常见开发任务

### 添加新功能

1. 在 `pojo/` 中创建/修改实体类
2. 在 `mapper/` 中创建 Mapper 接口 + `resources/mappers/` 中创建 XML
3. 在 `service/` 中创建 Service 类
4. 在 `controller/` 中创建 Controller（`@Controller` + `@ResponseBody`）
5. 在 `static/pages/` 下创建 HTML 页面

### 修改现有功能

- 控制器 → `controller/` 包
- 业务逻辑 → `service/` 包
- 数据访问 → `mapper/` 接口 + `resources/mappers/` XML
- 前端页面 → `resources/static/pages/` 目录
