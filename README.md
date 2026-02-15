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
- 文档标签（`labels`）
- 页面状态（草稿 / 已发布）
- 页面评论（按页面保存）
- 全文搜索（标题/摘要/正文）

## 项目结构

```text
.
├── backend
│   ├── src/main/java/com/ghostauthor/knowledge
│   └── src/main/resources/application.yml
├── frontend
│   ├── src
│   ├── nginx.conf
│   └── vercel.json
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

## 在 Vercel 部署（前端可直接部署）

> 说明：Vercel 负责部署 `frontend`。`backend`（Spring Boot）建议继续用 Docker/云主机部署，然后把 API 地址配置给前端。

### 1) 先部署后端（任一可公网访问环境）

例如继续使用你的服务器 Docker：

```bash
docker compose up -d --build backend elasticsearch
```

确保后端可通过公网域名访问，例如：

- `https://api.your-domain.com/api`

并设置后端 CORS（环境变量）：

- `CORS_ALLOWED_ORIGIN=https://your-frontend.vercel.app`

### 2) 在 Vercel 导入本仓库

在 Vercel 新建项目时使用以下配置：

- Framework Preset: `Vite`
- Root Directory: `frontend`（推荐）
- Build Command: `npm run build`
- Output Directory: `dist`

> 如果你直接把仓库根目录作为 Root Directory，也可以。仓库根目录已提供 `vercel.json`，会自动构建 `frontend` 并做 SPA 路由回退。

### 3) 配置前端环境变量（Vercel Project Settings -> Environment Variables）

- `VITE_API_BASE_URL=https://api.your-domain.com/api`

### 4) 重新部署

触发一次 Redeploy 后即可访问 Vercel 域名。

### 常见问题：`404: NOT_FOUND`

出现 `Code: NOT_FOUND` 时，通常是项目根目录或输出目录配置不匹配。按下面检查：

1. Vercel Project Settings -> General -> Root Directory  
`frontend`（推荐）或仓库根目录（已支持）
2. Build & Output Settings  
`Build Command = npm run build`，`Output Directory = dist`（当 Root Directory 为 `frontend`）
3. 环境变量  
确认 `VITE_API_BASE_URL` 已设置并重新部署
4. 触发 Redeploy  
配置修改后需要重新部署，旧部署仍可能返回 404

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
- `VITE_API_BASE_URL`：前端调用后端 API 基地址（Vercel 必配）

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
