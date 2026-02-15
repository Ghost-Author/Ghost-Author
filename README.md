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

后端健康后会得到一个 Railway 域名，例如：

- `https://ghost-backend-production.up.railway.app`

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

2. 前端可打开但请求后端失败  
检查 `VITE_API_BASE_URL` 是否带 `/api`，并在 frontend 服务重新部署。

3. 出现 CORS 报错  
把 backend 的 `CORS_ALLOWED_ORIGIN` 设为 frontend 实际域名（含 `https://`）。

4. 构建日志提示 `Dockerfile does not exist`  
在服务 Variables 里设置 `RAILWAY_DOCKERFILE_PATH`：前端为 `Dockerfile`，后端为 `Dockerfile.backend`。

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
- `VITE_API_BASE_URL`：前端调用后端 API 基地址（Railway 前端服务建议配置）

## API 示例

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
