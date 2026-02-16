# Ghost Author - 私有化知识库 / 文档系统

一个可私有化部署的知识库系统，技术栈：

- 前端：Vue 3 + Vite + Markdown 编辑器（`md-editor-v3`）
- 后端：Spring Boot 3 + JPA
- 文档存储：本地文件系统（Markdown 文件）
- 全文检索：Elasticsearch（默认）/ OpenSearch（可扩展）

## 功能

- 文档创建 / 编辑 / 删除 / 列表
- Markdown 可视化编辑与预览
- 文档版本历史（查看版本、回滚版本）
- 文档版本对比（Diff）
- 父子页面层级（`parentSlug`）
- 页面拖拽移动（调整父子层级）
- 文档标签（`labels`）
- 页面状态（草稿 / 已发布）
- 页面可见性（空间可见 / 私有）
- 页面评论（按页面保存）
- 页面附件（上传/引用/下载）
- 页面模板（会议纪要/评审/变更）
- 全文搜索（标题/摘要/正文）

## 正式完成定义（v1.0 收口标准）

满足以下全部条件，即视为项目“正式完成”，不再算烂尾：

1. 可部署：本地 `docker compose up -d --build` 可启动前后端；Railway 双服务可部署成功。
2. 可登录：账号登录、登出、改密、角色权限（ADMIN/EDITOR/VIEWER）可用。
3. 可创作：Markdown 编辑、实时预览、页面状态（草稿/发布/归档）可用。
4. 可协作：评论、附件、版本历史、版本回滚、页面分享可用。
5. 可组织：父子层级、页面树、收藏、最近访问、模板中心可用。
6. 可检索：标题/摘要/正文搜索可用（可选 ES；禁用搜索时系统可正常运行）。
7. 可运维：数据卷挂载后重启不丢数据，健康检查可访问。
8. 文档完整：README 包含本地部署、Railway 部署、环境变量、常见问题与 API 示例。

当前仓库按以上标准已达到 v1.0 可交付状态。后续改动默认按“增强项”处理。

## 项目结构

```text
.
├── backend
│   ├── src/main/java/com/ghostauthor/knowledge
│   └── src/main/resources/application.yml
├── Dockerfile
├── Dockerfile.backend
├── frontend
│   ├── src
│   ├── nginx.conf
│   └── Dockerfile
├── railway.toml
└── docker-compose.yml
```

## 一键私有化部署（推荐）

```bash
cd Ghost-Author
docker compose up -d --build
```

服务地址：

- 前端：[http://localhost:5173](http://localhost:5173)
- 后端 API：[http://localhost:8080/api/documents](http://localhost:8080/api/documents)
- Elasticsearch：[http://localhost:9200](http://localhost:9200)

## 在 Railway 部署（推荐云部署方案）

建议在 Railway 建 2 个服务（都指向同一个 GitHub 仓库）：

1. `backend`（Spring Boot）
2. `frontend`（Vue + Nginx）

可选第 3 个服务：`elasticsearch`（如需全文搜索）

仓库根目录已提供：

- `Dockerfile`（前端镜像）
- `Dockerfile.backend`（后端镜像）

如果你在 Railway 里找不到 `Source` 页面，可直接在每个服务的 `Variables` 配置 `RAILWAY_DOCKERFILE_PATH`。

### 1) 部署 backend 服务（无需改 Root Directory）

- New Service -> Deploy from GitHub Repo
- Builder 使用：`Dockerfile`
- Start Command 留空（使用镜像默认 `ENTRYPOINT`）
- 在 Variables 增加：
  - `RAILWAY_DOCKERFILE_PATH=Dockerfile.backend`

建议为 backend 挂载 Volume：

- 挂载路径：`/app/data`
- 挂载路径：`/app/storage/docs`

backend 环境变量建议：

- `SEARCH_ENABLED=false`（如果你暂时不部署 Elasticsearch）
- `SEARCH_URIS=http://<your-es-host>:9200`（如果启用搜索）
- `DOCS_BASE_DIR=/app/storage/docs`
- `CORS_ALLOWED_ORIGIN=https://<your-frontend-domain>`
- `AUTH_USERS=admin:admin123:ADMIN,liupeng:{bcrypt}$2a$...:EDITOR`（账号密码，逗号分隔；支持明文和 bcrypt；可选角色）
- `AUTH_TOKEN_TTL_HOURS=168`（登录 token 有效时长，单位小时）
- `AUTH_REMEMBER_TOKEN_TTL_HOURS=720`（勾选“记住我”时的 token 有效时长，单位小时）
- `AUTH_MAX_FAILURES=8`（连续失败多少次后临时锁定）
- `AUTH_LOCK_MINUTES=10`（达到失败阈值后的锁定分钟数）
- `JAVA_TOOL_OPTIONS=-XX:MaxRAMPercentage=70 -XX:InitialRAMPercentage=20 -XX:+UseG1GC -XX:+ExitOnOutOfMemoryError`（避免小规格实例被 OOM 杀掉）

后端健康后会得到一个 Railway 域名，例如：

- `https://ghost-backend-production.up.railway.app`

可用下面地址快速检查后端状态：

- `https://<your-backend-domain>/actuator/health`

### 2) 部署 frontend 服务（无需改 Root Directory）

- New Service -> Deploy from GitHub Repo
- Builder 使用：`Dockerfile`
- Start Command 留空（使用 Nginx 默认启动）
- 在 Variables 增加：
  - `RAILWAY_DOCKERFILE_PATH=Dockerfile`

前端环境变量（必须）：

- `VITE_API_BASE_URL=https://<your-backend-domain>/api`

说明：

- 前端构建期会读取 `VITE_API_BASE_URL`
- 若未配置，会回退到 `http://localhost:8080/api`（仅适合本地）

### 3) （可选）部署 Elasticsearch 服务

如果你需要全文搜索，可额外创建 ES 服务，并将其地址填入 backend 的 `SEARCH_URIS`。

若不需要搜索，保持：

- `SEARCH_ENABLED=false`

### 4) Railway 上的常见问题

1. 后端启动成功但外网 502/无法访问  
确认后端监听端口使用了 `PORT`（本仓库已支持 `server.port=${PORT:8080}`）。
同时访问 `https://<your-backend-domain>/actuator/health`，确认返回 `{"status":"UP"}`。

2. 前端可打开但请求后端失败  
检查 `VITE_API_BASE_URL` 是否带 `/api`，并在 frontend 服务重新部署。

3. 出现 CORS 报错  
把 backend 的 `CORS_ALLOWED_ORIGIN` 设为 frontend 实际域名（含 `https://`）。

4. 构建日志提示 `Dockerfile does not exist`  
在服务 Variables 里设置 `RAILWAY_DOCKERFILE_PATH`：前端为 `Dockerfile`，后端为 `Dockerfile.backend`。

5. 重启或重新部署后数据丢失  
确认 backend 服务已挂载 Volume 到 `/app/data`（H2 数据库）和 `/app/storage/docs`（Markdown 文件）。未挂载时实例重建会丢数据。

6. 后端日志出现 `Table "DOCUMENTS" not found` 并重启  
请确保已部署最新代码版本（已修复初始化脚本在空库首启时的兼容问题），然后重新 Deploy 一次 backend。

## 本地开发

### 1) 启动 Elasticsearch（可选）

```bash
docker run -d --name elasticsearch \
  -p 9200:9200 \
  -e discovery.type=single-node \
  -e xpack.security.enabled=false \
  docker.elastic.co/elasticsearch/elasticsearch:8.14.3
```

### 2) 启动后端

```bash
cd backend
mvn spring-boot:run
```

### 3) 启动前端

```bash
cd frontend
npm install
npm run dev
```

打开 [http://localhost:5173](http://localhost:5173)

## 关键环境变量

- `SEARCH_URIS`：检索引擎地址（如 `http://elasticsearch:9200`）
- `SEARCH_ENABLED`：是否开启检索（`true/false`）
- `DOCS_BASE_DIR`：Markdown 文档目录
- `CORS_ALLOWED_ORIGIN`：前端跨域地址
- `AUTH_USERS`：登录账号密码（格式：`user1:pass1:ADMIN,user2:{bcrypt}$2a$...:VIEWER`，角色可选 `ADMIN/EDITOR/VIEWER`）
- `AUTH_TOKEN_TTL_HOURS`：登录 token 有效时长（小时）
- `AUTH_REMEMBER_TOKEN_TTL_HOURS`：记住我 token 有效时长（小时）
- `AUTH_MAX_FAILURES`：连续登录失败阈值
- `AUTH_LOCK_MINUTES`：登录失败锁定时长（分钟）
- `VITE_API_BASE_URL`：前端调用后端 API 基地址（Railway 前端服务建议配置）

## API 示例

### 登录

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{
    "username":"admin",
    "password":"admin123"
  }'
```

如果接口返回 `mustChangePassword=true`，表示该账号需要先改密再继续使用。

### 退出登录

```bash
curl -X POST http://localhost:8080/api/auth/logout \
  -H "Authorization: Bearer <token>"
```

### 修改当前用户密码

```bash
curl -X POST http://localhost:8080/api/auth/password/change \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "currentPassword":"oldPass123",
    "newPassword":"newPass123"
  }'
```

`AUTH_USERS` 的密码支持两种格式：

- 明文：`admin:admin123`
- bcrypt：`liupeng:{bcrypt}$2a$10$xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx`

`AUTH_USERS` 可附带角色（默认 `ADMIN`）：

- `admin:admin123:ADMIN`
- `editor:{bcrypt}$2a$...:EDITOR`
- `guest:{bcrypt}$2a$...:VIEWER`（只读，禁止写操作）

角色权限说明：

- `ADMIN`：可读写所有页面，可管理模板
- `EDITOR`：可创建页面；仅可修改自己可编辑的页面（owner 或 editors 包含自己）
- `VIEWER`：只读（后端拒绝写接口）

管理员可在前端「用户管理」里在线增删改用户（写入后端数据库；挂载 `/app/data` 后重启可保留）。
密码策略：至少 8 位，且需包含字母和数字（bcrypt 密文可直接配置/导入）。
任意已登录用户可在右上角「修改密码」完成自主改密；首次登录/管理员重置后会触发强制改密弹窗。

### 管理用户（ADMIN）

```bash
curl http://localhost:8080/api/auth/users \
  -H "Authorization: Bearer <admin-token>"
```

### 创建文档

```bash
curl -X POST http://localhost:8080/api/documents \
  -H 'Content-Type: application/json' \
  -d '{
    "slug":"spring-guide",
    "title":"Spring 指南",
    "summary":"Spring Boot 快速入门",
    "content":"# Spring Guide\\n\\nHello",
    "parentSlug":null,
    "labels":["spring","backend"],
    "status":"DRAFT"
  }'
```

### 页面评论

```bash
curl -X POST http://localhost:8080/api/documents/spring-guide/comments \
  -H 'Content-Type: application/json' \
  -d '{
    "author":"liupeng",
    "content":"这篇文档很清晰，继续补充部署细节"
  }'
```

### 页面附件上传

```bash
curl -X POST http://localhost:8080/api/documents/spring-guide/attachments \
  -F "file=@./demo.png"
```

### 搜索

```bash
curl "http://localhost:8080/api/documents/search?q=Spring"
```

### 查看版本历史

```bash
curl "http://localhost:8080/api/documents/spring-guide/versions"
```

### 回滚到指定版本

```bash
curl -X POST "http://localhost:8080/api/documents/spring-guide/versions/1/restore"
```

### 对比两个版本

```bash
curl "http://localhost:8080/api/documents/spring-guide/versions/diff?from=1&to=2"
```
