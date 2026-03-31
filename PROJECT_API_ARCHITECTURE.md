# AI Hub 后端架构与前端对接文档

> 生成时间：2026-03-31  
> 项目：`aihub-backend`  
> 文档用途：前端项目初始化、接口联调、TypeScript 类型生成参考

## 1. 项目概览

AI Hub 后端是一个基于 Spring Boot 3 + JDK 21 的统一 AI 能力网关，核心目标是：

1. 提供用户、权限、资源授权体系。
2. 对接 Dify（应用聊天、会话、知识库、文件上传）并做统一鉴权。
3. 提供稳定统一的 API 响应格式给前端。

## 2. 技术架构

### 2.1 技术栈

- `Spring Boot 3.5.13`
- `Spring Security + JWT`
- `Spring Data JPA`（Repository）
- `MyBatis-Plus`（Mapper + XML）
- `MySQL 8`（业务库）
- `Redis`（token 黑名单、资源授权缓存、验证码）
- `WebClient`（统一代理 Dify API）
- `Knife4j / OpenAPI3`

### 2.2 包结构（`com.aihub`）

- `config`：配置类（Security、Redis、JPA、MyBatis、WebClient 等）
- `controller`：REST API 层
- `service`：业务服务层
- `dify/service`：Dify 代理服务
- `repository`：JPA 数据访问
- `mapper`：MyBatis-Plus 数据访问
- `entity`：数据库实体
- `dto`：请求/响应对象
- `common`：统一返回、异常、枚举
- `security`：JWT 与认证相关
- `annotation + aspect`：资源授权注解与切面

## 3. 运行与访问信息

### 3.1 服务地址

- 服务端口：`8088`
- 服务前缀：`/aihub-api`
- 本地基础地址：`http://localhost:8088/aihub-api`

### 3.2 文档地址

- Knife4j：`http://localhost:8088/aihub-api/doc.html`
- OpenAPI：`http://localhost:8088/aihub-api/v3/api-docs`

### 3.3 默认开发配置

- MySQL：`127.0.0.1:3306/aihub`，`root/123456`
- Redis：`127.0.0.1:6379`

## 4. 认证与权限模型

### 4.1 JWT 认证

- 登录成功后返回 `token`。
- 后续请求头：`Authorization: Bearer <token>`。
- 退出登录会把 token 放入 Redis 黑名单。

### 4.2 角色枚举

- `super_admin`：超级管理员
- `admin`：管理员
- `user`：普通用户

### 4.3 资源类型枚举

- `app`：Dify 应用
- `knowledge`：Dify 知识库

### 4.4 权限规则

- 接口级角色限制：`@PreAuthorize`
- 资源级限制：`@RequireResource(type=APP/KNOWLEDGE, paramName=...)`
- `super_admin/admin` 默认拥有全部资源权限

## 5. 统一响应与错误码

### 5.1 统一响应结构

```json
{
  "code": 200,
  "message": "success",
  "data": {}
}
```

### 5.2 常见错误码

- `200`：成功
- `400`：参数错误 / 业务校验失败
- `401`：未登录或认证失败
- `403`：无权限
- `404`：资源不存在
- `500`：系统异常

## 6. 接口总览

说明：以下路径均为完整路径（已含 `/aihub-api` 前缀）。

## 6.1 认证模块 `AuthController`

| 接口 | 方法 | 路径 | 是否鉴权 | 请求参数 | 响应 |
|---|---|---|---|---|---|
| 获取图形验证码 | GET | `/aihub-api/auth/captcha/image` | 否 | 无 | `R<CaptchaResponse>` |
| 登录 | POST | `/aihub-api/auth/login` | 否 | `LoginRequest` | `R<LoginResponse>` |
| 登出 | POST | `/aihub-api/auth/logout` | 是 | Header `Authorization` | `R<Void>` |

登录请求示例：

```json
{
  "username": "admin",
  "password": "admin123",
  "captchaId": "f7f5e2...",
  "captchaCode": "ABCD"
}
```

## 6.2 用户模块 `UserController`

| 接口 | 方法 | 路径 | 权限 | 请求 | 响应 |
|---|---|---|---|---|---|
| 当前用户信息 | GET | `/aihub-api/users/me` | 登录用户 | 无 | `R<UserInfoVO>` |
| 更新当前用户 | PUT | `/aihub-api/users/me` | 登录用户 | `UpdateUserRequest` | `R<UserInfoVO>` |
| 修改密码 | PUT | `/aihub-api/users/me/password` | 登录用户 | `ChangePasswordRequest` | `R<Void>` |
| 用户列表 | GET | `/aihub-api/users/list` | `admin/super_admin` | `keyword,page,size` | `R<List<UserInfoVO>>` |
| 创建用户 | POST | `/aihub-api/users/create` | `admin/super_admin` | `RegisterUserRequest` | `R<UserInfoVO>` |
| 修改用户状态 | PUT | `/aihub-api/users/{id}/status` | `admin/super_admin` | `status` | `R<Void>` |
| 修改用户角色 | PUT | `/aihub-api/users/{id}/role` | `super_admin` | `roleId` | `R<Void>` |
| 删除用户 | DELETE | `/aihub-api/users/{id}` | `super_admin` | 无 | `R<Void>` |

## 6.3 应用模块 `AppConfigController`

| 接口 | 方法 | 路径 | 权限 | 请求 | 响应 |
|---|---|---|---|---|---|
| 应用列表（按权限） | GET | `/aihub-api/apps/list` | 登录用户 | 无 | `R<List<AppConfigVO>>` |
| 应用详情 | GET | `/aihub-api/apps/{id}` | 资源权限 `app` | 无 | `R<AppConfigVO>` |
| 创建应用 | POST | `/aihub-api/apps/create` | `admin/super_admin` | `CreateAppRequest` | `R<AppConfigVO>` |
| 更新应用 | PUT | `/aihub-api/apps/{id}` | `admin/super_admin` | `CreateAppRequest` | `R<AppConfigVO>` |
| 删除应用 | DELETE | `/aihub-api/apps/{id}` | `admin/super_admin` | 无 | `R<Void>` |
| 获取 Dify 应用参数 | GET | `/aihub-api/apps/{id}/parameters` | 资源权限 `app` | 无 | `R<Object>` |

## 6.4 资源授权模块 `ResourceAuthController`

| 接口 | 方法 | 路径 | 权限 | 请求 | 响应 |
|---|---|---|---|---|---|
| 批量授权 | POST | `/aihub-api/resource-auth/grant` | `admin/super_admin` | `GrantRequest` | `R<Void>` |
| 撤销授权 | POST | `/aihub-api/resource-auth/revoke` | `admin/super_admin` | `userId,resourceType` | `R<Void>` |
| 查询用户授权资源 | GET | `/aihub-api/resource-auth/user/{userId}` | `admin/super_admin` | 无 | `R<{app:number[],knowledge:number[]}>` |

## 6.5 聊天模块 `ChatController`

| 接口 | 方法 | 路径 | 权限 | 请求 | 响应 |
|---|---|---|---|---|---|
| 流式聊天 | POST | `/aihub-api/chat/stream?appId=1` | 资源权限 `app` | `ChatRequest` | `text/event-stream` |
| 停止生成 | POST | `/aihub-api/chat/stop` | 资源权限 `app` | `appId,taskId` | `R<Void>` |
| 消息反馈 | POST | `/aihub-api/chat/feedback` | 资源权限 `app` | `appId,messageId,rating` | `R<Void>` |
| 建议问题 | GET | `/aihub-api/chat/suggested` | 资源权限 `app` | `appId,messageId` | `R<Object>` |
| 语音转文字 | POST | `/aihub-api/chat/audio-to-text` | 资源权限 `app` | `appId,file`(multipart) | `R<Object>` |

### SSE 事件说明（`/chat/stream`）

后端向前端推送事件名：

- `message`：增量回答片段
- `message_end`：本轮结束，带 metadata
- `message_replace`：替换回答
- `ping`：心跳
- `error`：错误

`message` 事件数据结构（示例）：

```json
{
  "answer": "增量文本",
  "conversationId": "conv_xxx",
  "messageId": "msg_xxx",
  "taskId": "task_xxx"
}
```

## 6.6 对话模块 `ConversationController`

| 接口 | 方法 | 路径 | 权限 | 请求 | 响应 |
|---|---|---|---|---|---|
| 对话列表 | GET | `/aihub-api/conversations` | 资源权限 `app` | `appId,page,limit` | `R<Object>` |
| 对话消息列表 | GET | `/aihub-api/conversations/{conversationId}/messages` | 资源权限 `app` | `appId,firstId,limit` | `R<Object>` |
| 重命名对话 | PUT | `/aihub-api/conversations/{conversationId}/name` | 资源权限 `app` | `appId,name,autoGenerate` | `R<Object>` |
| 删除对话 | DELETE | `/aihub-api/conversations/{conversationId}` | 资源权限 `app` | `appId` | `R<Void>` |

## 6.7 知识库模块 `KnowledgeController`

| 接口 | 方法 | 路径 | 权限 | 请求 | 响应 |
|---|---|---|---|---|---|
| 知识库列表 | GET | `/aihub-api/knowledge/list` | 登录用户 | 无 | `R<List<KnowledgeBaseVO>>` |
| 知识库详情 | GET | `/aihub-api/knowledge/{id}` | 资源权限 `knowledge` | 无 | `R<KnowledgeBaseVO>` |
| 创建知识库 | POST | `/aihub-api/knowledge/create` | `admin/super_admin` | `CreateKnowledgeBaseRequest` | `R<KnowledgeBaseVO>` |
| 更新知识库 | PUT | `/aihub-api/knowledge/{id}` | `admin/super_admin` | `CreateKnowledgeBaseRequest` | `R<KnowledgeBaseVO>` |
| 删除知识库 | DELETE | `/aihub-api/knowledge/{id}` | `admin/super_admin` | 无 | `R<Void>` |
| 文档列表 | GET | `/aihub-api/knowledge/{id}/documents` | 资源权限 `knowledge` | `keyword,page,limit` | `R<Object>` |
| 文本创建文档 | POST | `/aihub-api/knowledge/{id}/documents/text` | 资源权限 `knowledge` | `Map<String,Object>` | `R<Object>` |
| 文件创建文档 | POST | `/aihub-api/knowledge/{id}/documents/file` | 资源权限 `knowledge` | `file,processRule` | `R<Object>` |
| 文本更新文档 | PUT | `/aihub-api/knowledge/{id}/documents/{docId}/text` | 资源权限 `knowledge` | `Map<String,Object>` | `R<Object>` |
| 文件更新文档 | PUT | `/aihub-api/knowledge/{id}/documents/{docId}/file` | 资源权限 `knowledge` | `file,processRule` | `R<Object>` |
| 删除文档 | DELETE | `/aihub-api/knowledge/{id}/documents/{docId}` | 资源权限 `knowledge` | 无 | `R<Object>` |
| 索引状态 | GET | `/aihub-api/knowledge/{id}/documents/indexing-status` | 资源权限 `knowledge` | `batch` | `R<Object>` |
| 文档分段 | GET | `/aihub-api/knowledge/{id}/documents/{docId}/segments` | 资源权限 `knowledge` | 无 | `R<Object>` |
| 检索测试 | POST | `/aihub-api/knowledge/{id}/retrieve` | 资源权限 `knowledge` | `Map<String,Object>` | `R<Object>` |

## 6.8 文件模块 `FileController`

| 接口 | 方法 | 路径 | 权限 | 请求 | 响应 |
|---|---|---|---|---|---|
| 上传文件到 Dify | POST | `/aihub-api/files/upload` | 资源权限 `app` | `appId,file`(multipart) | `R<Object>` |

## 6.9 系统模块 `SystemController`

| 接口 | 方法 | 路径 | 权限 | 响应 |
|---|---|---|---|---|
| 健康检查 | GET | `/aihub-api/system/health` | 无 | `R<{status:'UP',timestamp:string}>` |

## 7. 前端调用流程（推荐）

1. 调用 `/auth/captcha/image` 获取验证码。
2. 用户输入验证码后调用 `/auth/login`。
3. 保存返回 `token`（建议存内存或短期存储）。
4. 所有受保护接口带 `Authorization: Bearer <token>`。
5. 首屏先调 `/users/me` 获取当前用户信息与角色。
6. 业务页根据路由调用 `/apps/list`、`/knowledge/list` 等。
7. 聊天页用 `fetch + ReadableStream` 处理 `/chat/stream` 的 SSE。
8. 退出时调用 `/auth/logout`。

## 8. 前端请求封装建议

```ts
// axios 示例
import axios from 'axios'

export const http = axios.create({
  baseURL: 'http://localhost:8088/aihub-api',
  timeout: 30000,
})

http.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

export type R<T> = {
  code: number
  message: string
  data: T
}
```

## 9. DTO / VO 模型定义（含字段注释）

> 以下是前端最常用模型，建议直接转成 TypeScript 类型。

```ts
export interface CaptchaResponse {
  captchaId: string // 验证码唯一ID，登录时必须回传
  imageBase64: string // base64 图片，形如 data:image/png;base64,...
  expireIn: number // 过期秒数
}

export interface LoginRequest {
  username: string // 登录用户名
  password: string // 登录密码
  captchaId: string // 验证码ID
  captchaCode: string // 用户输入的验证码
}

export interface LoginResponse {
  token: string // JWT token
  tokenType: string // 固定为 Bearer
  expiresIn: number // 过期时间（秒）
  userId: number // 用户ID
  username: string // 用户名
  nickname?: string // 昵称（当前版本可能为空）
  roleCode: 'super_admin' | 'admin' | 'user' // 角色编码
}

export interface RegisterUserRequest {
  username: string // 新用户名，3-50
  password: string // 新密码，6-100
  nickname?: string // 昵称
  email?: string // 邮箱
  phone?: string // 手机号
}

export interface UpdateUserRequest {
  nickname?: string // 昵称
  email?: string // 邮箱
  phone?: string // 手机号
  avatar?: string // 头像URL
}

export interface ChangePasswordRequest {
  oldPassword: string // 旧密码
  newPassword: string // 新密码
}

export interface UserInfoVO {
  id: number // 用户ID
  username: string // 用户名
  nickname?: string // 昵称
  avatar?: string // 头像URL
  email?: string // 邮箱
  phone?: string // 手机号
  roleCode: 'super_admin' | 'admin' | 'user' // 角色编码
  roleName?: string // 角色名称
  status: number // 1启用，0禁用
  lastLoginAt?: string // 最后登录时间
  createdAt: string // 创建时间
}

export interface CreateAppRequest {
  appName: string // 应用名称
  appCode: string // 应用编码（唯一）
  appType: string // 应用类型
  difyApiKey: string // Dify API Key
  difyAppId?: string // Dify 应用ID
  difyBaseUrl?: string // Dify 基础地址
  description?: string // 描述
  icon?: string // 图标URL
  sort?: number // 排序值
}

export interface AppConfigVO {
  id: number // 应用ID
  appName: string // 应用名称
  appCode: string // 应用编码
  appType: string // 应用类型
  description?: string // 描述
  icon?: string // 图标URL
  sort: number // 排序值
  enabled: boolean // 是否启用
  createdAt: string // 创建时间
}

export interface CreateKnowledgeBaseRequest {
  kbName: string // 知识库名称
  kbCode: string // 知识库编码（唯一）
  difyApiKey: string // Dify API Key
  difyDatasetId?: string // 已有 Dify datasetId（可选）
  difyBaseUrl?: string // Dify 基础地址
  description?: string // 描述
  icon?: string // 图标URL
  indexingTechnique?: string // 索引方式
  embeddingModel?: string // 向量模型
  sort?: number // 排序值
}

export interface KnowledgeBaseVO {
  id: number // 知识库ID
  kbName: string // 名称
  kbCode: string // 编码
  description?: string // 描述
  icon?: string // 图标URL
  documentCount: number // 文档数量
  wordCount: number // 词数统计
  indexingTechnique?: string // 索引方式
  embeddingModel?: string // 向量模型
  sort: number // 排序值
  enabled: boolean // 是否启用
  createdAt: string // 创建时间
}

export interface GrantRequest {
  userId: number // 被授权用户ID
  resourceType: 'APP' | 'KNOWLEDGE' // 注意：按后端枚举名传值
  resourceIds: number[] // 资源ID集合
}

export interface ChatRequest {
  query: string // 用户提问
  conversationId?: string // 继续会话时传
  inputs?: Record<string, any> // Dify 自定义变量
}
```

## 10. 核心实体（Entity）字段注释参考

> 用于前端理解业务数据来源与字段语义，不建议前端直接依赖 Entity 作为 API 契约。

```ts
export interface UserEntity {
  id: number // 主键ID
  username: string // 用户名（唯一）
  password: string // 密码哈希
  nickname?: string // 昵称
  avatar?: string // 头像
  email?: string // 邮箱
  phone?: string // 手机号
  roleId: number // 角色ID
  status: number // 状态：1启用/0禁用
  lastLoginAt?: string // 最后登录时间
  lastLoginIp?: string // 最后登录IP
  deleted: boolean // 逻辑删除
  createdAt: string // 创建时间
  updatedAt: string // 更新时间
}

export interface RoleEntity {
  id: number // 角色ID
  code: 'super_admin' | 'admin' | 'user' // 角色编码
  name: string // 角色名
  description?: string // 描述
  sort?: number // 排序
  status: number // 状态
  deleted: boolean // 逻辑删除
  createdAt: string // 创建时间
  updatedAt: string // 更新时间
}

export interface AppConfigEntity {
  id: number // 应用ID
  appName: string // 应用名称
  appCode: string // 应用编码
  appType: string // 应用类型
  difyAppId?: string // Dify 应用ID
  difyApiKey: string // Dify API Key
  difyBaseUrl?: string // Dify 基础URL
  description?: string // 描述
  icon?: string // 图标
  sort: number // 排序
  enabled: boolean // 是否启用
  deleted: boolean // 逻辑删除
  createdBy: number // 创建人ID
  createdAt: string // 创建时间
  updatedAt: string // 更新时间
}

export interface KnowledgeBaseEntity {
  id: number // 知识库ID
  kbName: string // 名称
  kbCode: string // 编码
  difyDatasetId: string // Dify 数据集ID
  difyApiKey: string // Dify API Key
  difyBaseUrl?: string // Dify 基础URL
  description?: string // 描述
  icon?: string // 图标
  documentCount: number // 文档数
  wordCount: number // 字数
  indexingTechnique?: string // 索引方式
  embeddingModel?: string // 向量模型
  sort: number // 排序
  enabled: boolean // 是否启用
  deleted: boolean // 逻辑删除
  createdBy: number // 创建人ID
  createdAt: string // 创建时间
  updatedAt: string // 更新时间
}
```

## 11. 聊天流式调用示例（前端）

> `POST + SSE` 不是原生 `EventSource` 的典型场景，建议 `fetch` 读取流。

```ts
async function streamChat(appId: number, body: { query: string; conversationId?: string; inputs?: Record<string, any> }) {
  const token = localStorage.getItem('token')
  const res = await fetch(`http://localhost:8088/aihub-api/chat/stream?appId=${appId}`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${token}`,
    },
    body: JSON.stringify(body),
  })

  const reader = res.body?.getReader()
  const decoder = new TextDecoder('utf-8')
  if (!reader) return

  while (true) {
    const { done, value } = await reader.read()
    if (done) break
    const chunk = decoder.decode(value, { stream: true })
    console.log('SSE chunk =>', chunk)
  }
}
```

## 12. 文件上传调用示例

```ts
async function uploadFile(appId: number, file: File) {
  const token = localStorage.getItem('token')
  const form = new FormData()
  form.append('appId', String(appId))
  form.append('file', file)

  const res = await fetch('http://localhost:8088/aihub-api/files/upload', {
    method: 'POST',
    headers: {
      Authorization: `Bearer ${token}`,
    },
    body: form,
  })

  return await res.json() // R<Object>
}
```

## 13. 前端生成代码建议

1. 以 `R<T>` 作为统一响应泛型。
2. 先生成“稳定 DTO/VO”类型（第9章），`Object` 类型接口先用 `Record<string, any>`。
3. 将 `roleCode`、`resourceType` 提取为前端枚举常量。
4. 对 `R<Object>` 的 Dify 透传接口，在联调后逐步收敛成精确类型。
5. 聊天流事件建议建立 `message/message_end/error` 三类处理器。

## 14. 当前已知注意项

1. `security.ignore-urls` 包含 `/auth/register`，但当前控制器未提供该公开注册接口。
2. `GrantRequest.resourceType` 走 Spring 枚举绑定，建议前端按 `APP/KNOWLEDGE` 传值。
3. 部分接口返回 `R<Object>`，结构取决于 Dify 返回。

---

如需继续，我可以基于本文件再给你生成一份 `frontend-api.ts`（含完整 TypeScript 类型 + axios API 函数）。
