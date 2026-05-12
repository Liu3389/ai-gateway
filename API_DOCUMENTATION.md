# AI Gateway Platform - 超级详细 API 接口文档

> **当前部署**: `http://10.211.55.10:8080/api`  
> **本地开发**: `http://localhost:8080/api`  
> **Content-Type**: `application/json`  
> **字符编码**: UTF-8  
> **架构**: 前后端分离，UUID Token 认证（登录获取，24h有效），API Key 认证（Chat接口）

---

## 目录

1. [通用规范](#通用规范)
2. [认证模块 AuthController](#1-认证模块-authcontroller)
3. [用户模块 UserController](#2-用户模块-usercontroller)
4. [API Key 管理 ApiKeyController](#3-api-key-管理-apikeycontroller)
5. [管理员模块 AdminController](#4-管理员模块-admincontroller)
6. [聊天对话 ChatController](#5-聊天对话-chatcontroller)
7. [错误码全集](#错误码全集)
8. [数据模型参考](#数据模型参考)

---

## 通用规范

### 统一响应格式 `Result<T>`

**所有接口**均返回此结构，前端据此判断成功/失败：

```json
{
  "code": 200,
  // Integer, 业务状态码
  "message": "操作成功",
  // String, 提示信息
  "data": {
    ...
  },
  // T | null, 实际数据（类型视接口而定）
  "timestamp": 1700000000000
  // Long, Unix毫秒时间戳
}
```

### 成功判断规则

```
code == 200  → 成功
code != 200  → 失败，显示 message 给用户
```

### HTTP 状态码说明

> **注意**：本系统 HTTP Status Code 始终返回 **200**。业务成功/失败通过响应体中的 `code` 字段判断，不是通过 HTTP
> Status。这是本系统的设计约定。

### 认证方式

| 接口类型    | 认证方式    | Header 键名      | 示例值                                   |
|---------|---------|----------------|---------------------------------------|
| Chat 对话 | API Key | `X-API-Key`    | `sk-2adbdb4f5ae04cacb9ea0466ac4b23f0` |
| 所有业务接口  | 登录Token | `X-User-Token` | `a3a9e1242e8547a58da8aaf44358caa2`    |
| Auth 认证 | 无（公开接口） | -              | -                                     |

> **重要变更**：用户/APIKey/Admin 操作不再通过 URL 参数 `userId` 或 `X-User-Id` Header 传递身份，改为 `X-User-Token`
> Header（登录时获得的 UUID 格式 Token，24h有效）。身份自动识别，无法伪造。

### 前端登录流程

```
1. POST /auth/register   → 注册账号
2. POST /auth/login      → 获取 token（存入 localStorage）
3. 所有后续请求 Header 带上 X-User-Token: {token}
4. Token 过期（24h）后重新登录
```

---

## 1. 认证模块 AuthController

> 路径前缀: `/auth`  
> 无需认证 Header

### 1.1 用户注册

```
POST /auth/register
```

**请求体：**

```json
{
  "username": "myuser",
  // String, 必填, 用户名
  "password": "mypassword123",
  // String, 必填, 密码（明文，后端BCrypt加密）
  "email": "user@example.com"
  // String, 选填, 邮箱
}
```

**成功响应 (code=200)：**

```json
{
  "code": 200,
  "message": "注册成功",
  "data": {
    "id": 7,
    "username": "myuser",
    "email": "user@example.com",
    "balance": 0.0,
    "status": 1,
    "role": "USER",
    "freeApiStrategy": null,
    "createTime": "2026-05-12T17:30:00",
    "updateTime": "2026-05-12T17:30:00"
  },
  "timestamp": 1778578200000
}
```

**data 字段说明：**

| 字段              | 类型              | 说明                         |
|-----------------|-----------------|----------------------------|
| id              | Long            | 用户ID（后续操作需要）               |
| username        | String          | 用户名                        |
| email           | String          | 邮箱                         |
| balance         | BigDecimal      | 余额（美元），新用户为 0.0            |
| status          | Integer         | 1=启用，0=禁用                  |
| role            | String          | USER / ADMIN / SUPER_ADMIN |
| freeApiStrategy | String\|null    | 免费策略类型，null=无              |
| createTime      | String(ISO8601) | 注册时间                       |
| updateTime      | String(ISO8601) | 更新时间                       |

**失败示例：**

json
{
"code": 400,
"message": "用户名已存在",
"data": null,
"timestamp": 1778578200000
}


---

### 1.2 用户登录

```
POST /auth/login
```

**请求体：**

```json
{
  "username": "myuser",
  // String, 必填
  "password": "mypassword123"
  // String, 必填
}
```

**成功响应 (code=200)：**

```json
{
  "code": 200,
  "message": "登录成功",
  "data": {
    "token": "a3a9e1242e8547a58da8aaf44358caa2",
    "userInfo": {
      "id": 7,
      "username": "myuser",
      "email": "user@example.com",
      "balance": 0.0,
      "status": 1,
      "role": "USER",
      "freeApiStrategy": null,
      "createTime": "2026-05-12T17:30:00",
      "updateTime": "2026-05-12T17:30:00"
    }
  },
  "timestamp": 1778578200000
}
```

> **重要**：Token 为 UUID 格式（如 `a3a9e1242e8547a58da8aaf44358caa2`），登录成功后存入 `X-User-Token` Header。Token 存入
> Redis，24小时过期，无法伪造。

**失败示例：**

```json
{
  "code": 401,
  "message": "用户名或密码错误",
  "data": null,
  "timestamp": 1778578200000
}
{
  "code": 403,
  "message": "用户已被禁用",
  "data": null,
  "timestamp": 1778578200000
}
```

---

### 1.3 查询用户信息

```
GET /auth/userinfo?userId={userId}
```

| 参数     | 类型   | 必填 | 说明   |
|--------|------|----|------|
| userId | Long | ✅  | 用户ID |

**成功响应 (code=200)：** data 结构与注册返回一致。

---

### 1.4 查询用户余额

```
GET /auth/balance?userId={userId}
```

| 参数     | 类型   | 必填 | 说明   |
|--------|------|----|------|
| userId | Long | ✅  | 用户ID |

**成功响应 (code=200)：**

```json
{
  "code": 200,
  "message": "操作成功",
  "data": 10.0,
  "timestamp": 1778578200000
}
```

> data 直接是 BigDecimal 数字

---

## 2. 用户模块 UserController

> 路径前缀: `/user`  
> **所有请求必须带 Header**: `X-User-Token: {token}`（登录返回的 Token，身份自动识别）

### 2.1 充值

```
POST /user/recharge?amount={amount}
```

| 参数     | 类型         | 必填 | 说明              |
|--------|------------|----|-----------------|
| userId | Long       | ✅  | 用户ID            |
| amount | BigDecimal | ✅  | 充值金额（美元），必须 > 0 |

**成功响应 (code=200)：**

```json
{
  "code": 200,
  "message": "充值成功",
  "data": null,
  "timestamp": 1778578200000
}
```

**失败示例：**

```json
{
  "code": 500,
  "message": "充值金额必须大于0",
  "data": null,
  "timestamp": 1778578200000
}
```

---

### 2.2 查询用户信息

```
GET /user/info?userId={userId}
```

| 参数     | 类型   | 必填 | 说明   |
|--------|------|----|------|
| userId | Long | ✅  | 用户ID |

返回结构与 `/auth/userinfo` 相同。

---

### 2.3 查询余额

```
GET /user/balance?userId={userId}
```

| 参数     | 类型   | 必填 | 说明   |
|--------|------|----|------|
| userId | Long | ✅  | 用户ID |

返回结构同 `/auth/balance`。

---

## 3. API Key 管理 ApiKeyController

> 路径前缀: `/api-key`  
> **所有请求都需要通过 URL 参数传递 userId**

### 3.1 生成 API Key

```
POST /api-key/generate?userId={userId}
```

| 参数     | 类型   | 必填 | 说明            |
|--------|------|----|---------------|
| userId | Long | ✅  | 用户ID（Query参数） |

**请求体（选填）：**

```json
{
  "name": "MyProductionKey",
  // String, 选填, Key名称/备注
  "rateLimit": 200
  // Integer, 选填, 每分钟限流次数，默认100
}
```

> 请求体可以为 `{}` 或省略，将使用默认值。

**成功响应 (code=200)：**

```json
{
  "code": 200,
  "message": "API Key生成成功",
  "data": {
    "id": 2,
    "apiKey": "sk-3457a754a5534a668f87fb33c367e6f0",
    "name": "MyProductionKey",
    "status": 1,
    "rateLimit": 200,
    "createTime": "2026-05-12T17:30:00",
    "expireTime": null
  },
  "timestamp": 1778578200000
}
```

**data 字段说明：**

| 字段         | 类型                    | 说明                                        |
|------------|-----------------------|-------------------------------------------|
| id         | Long                  | API Key 记录 ID                             |
| apiKey     | String                | 实际 Key 值，格式 `sk-{uuid}`，用于 Chat 请求 Header |
| name       | String\|null          | Key 名称备注                                  |
| status     | Integer               | 1=启用，0=禁用                                 |
| rateLimit  | Integer               | 每分钟请求限制次数                                 |
| createTime | String(ISO8601)       | 创建时间                                      |
| expireTime | String(ISO8601)\|null | 过期时间，null=永久                              |

---

### 3.2 查询 API Key 列表

```
GET /api-key/list?userId={userId}
```

| 参数     | 类型   | 必填 | 说明   |
|--------|------|----|------|
| userId | Long | ✅  | 用户ID |

**成功响应 (code=200)：**

```json
{
  "code": 200,
  "message": "操作成功",
  "data": [
    {
      "id": 1,
      "apiKey": "sk-2adbdb4f5ae04cacb9ea0466ac4b23f0",
      "name": "MyTestKey",
      "status": 1,
      "rateLimit": 200,
      "createTime": "2026-05-12T17:06:41",
      "expireTime": null
    }
  ],
  "timestamp": 1778578200000
}
```

> data 为 ApiKeyInfoVO 数组，按创建时间倒序排列。

---

### 3.3 删除 API Key

```
DELETE /api-key/{id}?userId={userId}
```

| 参数     | 类型   | 必填 | 说明                   |
|--------|------|----|----------------------|
| id     | Long | ✅  | API Key ID（Path参数）   |
| userId | Long | ✅  | 用户ID（Query参数，用于权限校验） |

**成功响应 (code=200)：**

```json
{
  "code": 200,
  "message": "删除成功",
  "data": null,
  "timestamp": 1778578200000
}
```

**失败示例（无权操作他人的Key）：**

```json
{
  "code": 403,
  "message": "无权操作此API Key",
  "data": null,
  "timestamp": 1778578200000
}
```

---

### 3.4 启用 API Key

```
PUT /api-key/{id}/enable?userId={userId}
```

| 参数     | 类型   | 必填 | 说明                 |
|--------|------|----|--------------------|
| id     | Long | ✅  | API Key ID（Path参数） |
| userId | Long | ✅  | 用户ID（Query参数）      |

---

### 3.5 禁用 API Key

```
PUT /api-key/{id}/disable?userId={userId}
```

参数同上。

---

### 3.6 更新 API Key 限流阈值

```
PUT /api-key/{id}/rate-limit?userId={userId}&rateLimit={rateLimit}
```

| 参数        | 类型      | 必填 | 说明                 |
|-----------|---------|----|--------------------|
| id        | Long    | ✅  | API Key ID（Path参数） |
| userId    | Long    | ✅  | 用户ID（Query参数）      |
| rateLimit | Integer | ✅  | 新的每分钟限流次数          |

---

## 4. 管理员模块 AdminController

> 路径前缀: `/admin`  
> **所有请求必须带 Header**: `X-User-Id: {userId}`  
> userId 必须是 SUPER_ADMIN 或 ADMIN 角色的用户

### 角色权限矩阵

| 操作         | SUPER_ADMIN | ADMIN | USER |
|------------|:-----------:|:-----:|:----:|
| 获取系统统计     |      ✅      |   ✅   |  ❌   |
| 获取所有用户列表   |      ✅      |   ✅   |  ❌   |
| 获取用户详情     |      ✅      |   ✅   |  ❌   |
| 禁用/启用用户    |      ✅      |   ✅   |  ❌   |
| 获取管理员列表    |      ✅      |   ❌   |  ❌   |
| 分配/取消管理员角色 |      ✅      |   ❌   |  ❌   |
| 设置免费策略     |      ✅      |   ❌   |  ❌   |

---

### 4.1 分配管理员角色（仅SUPER_ADMIN）

```
POST /admin/assign-role
```

**Header:**

```
X-User-Id: 6
Content-Type: application/json
```

**请求体：**

```json
{
  "userId": 3,
  // Long, 必填, 目标用户ID
  "role": "ADMIN"
  // String, 必填, "ADMIN" 或 "SUPER_ADMIN"
}
```

**成功响应：**

```json
{
  "code": 200,
  "message": "角色分配成功",
  "data": null,
  "timestamp": 1778578200000
}
```

**失败示例：**

```json
{
  "code": 403,
  "message": "只有超级管理员才能执行此操作",
  "data": null,
  "timestamp": 1778578200000
}
{
  "code": 400,
  "message": "无效的角色类型",
  "data": null,
  "timestamp": 1778578200000
}
{
  "code": 404,
  "message": "用户不存在",
  "data": null,
  "timestamp": 1778578200000
}
```

---

### 4.2 取消管理员角色（仅SUPER_ADMIN）

```
POST /admin/revoke-role?userId={userId}
```

**Header:** `X-User-Id: 6`

| 参数     | 类型   | 必填 | 说明                    |
|--------|------|----|-----------------------|
| userId | Long | ✅  | 要取消角色的目标用户ID（Query参数） |

> 不能取消自己的管理员角色。

---

### 4.3 设置 API 免费策略（仅SUPER_ADMIN）

```
POST /admin/set-free-strategy
```

**Header:** `X-User-Id: 6`

**请求体：**

```json
{
  "userId": 3,
  "freeApiStrategy": "UNLIMITED",
  "freeQuota": 100.0,
  "dailyCallLimit": null,
  "monthlyCallLimit": null,
  "freeStrategyStartTime": null,
  "freeStrategyEndTime": null,
  "allowedFreeModels": null
}
```

**freeApiStrategy 可选值：**

| 值              | 说明                                                                              |
|----------------|---------------------------------------------------------------------------------|
| UNLIMITED      | 完全免费，不限制调用次数和Token                                                              |
| QUOTA_BASED    | 额度免费，需设 freeQuota                                                               |
| COUNT_LIMITED  | 限次免费，需设 dailyCallLimit/monthlyCallLimit                                         |
| TIME_LIMITED   | 限时免费，需设 startTime/endTime                                                       |
| MODEL_SPECIFIC | 模型限定免费，需设 allowedFreeModels（JSON字符串如 `["deepseek-v4-pro","deepseek-v4-flash"]`） |

---

### 4.4 获取所有管理员列表（仅SUPER_ADMIN）

```
GET /admin/admins
```

**Header:** `X-User-Id: 6`

**成功响应 (code=200)：** data 为 AdminInfoVO 数组，字段见下方数据模型参考。

---

### 4.5 获取所有用户列表（ADMIN及以上）

```
GET /admin/users
```

**Header:** `X-User-Id: 6`（可以是 ADMIN 或 SUPER_ADMIN）

**成功响应 (code=200)：** data 为 AdminInfoVO 数组。

---

### 4.6 获取用户详情（ADMIN及以上）

```
GET /admin/user/{userId}
```

**Header:** `X-User-Id: 6`

| 参数     | 类型   | 必填 | 说明             |
|--------|------|----|----------------|
| userId | Long | ✅  | 目标用户ID（Path参数） |

---

### 4.7 禁用/启用用户（ADMIN及以上）

```
POST /admin/user/status?userId={userId}&status={status}
```

**Header:** `X-User-Id: 6`

| 参数     | 类型      | 必填 | 说明              |
|--------|---------|----|-----------------|
| userId | Long    | ✅  | 目标用户ID（Query参数） |
| status | Integer | ✅  | 0=禁用，1=启用       |

---

### 4.8 获取系统统计（ADMIN及以上）⭐

```
GET /admin/stats
```

**Header:** `X-User-Id: 6`

**成功响应 (code=200)：**

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "totalUsers": 4,
    // Long, 总用户数
    "activeUsers": 1,
    // Long, 活跃用户数（最近7天有API调用）
    "adminCount": 1,
    // Long, 管理员数量
    "superAdminCount": 2,
    // Long, 超级管理员数量
    "disabledUsers": 0,
    // Long, 被禁用用户数
    "totalApiCalls": 5,
    // Long, 总API调用次数
    "todayApiCalls": 3,
    // Long, 今日API调用次数
    "totalRevenue": 0.000024,
    // BigDecimal, 总消费金额（美元）
    "todayRevenue": 0.000008,
    // BigDecimal, 今日消费金额（美元）
    "avgCallCost": 0.000004,
    // BigDecimal, 平均每次调用费用（美元）
    "freeStrategyUsers": 1,
    // Long, 使用免费策略的用户数
    "modelCount": 3,
    // Long, 模型配置总数
    "activeModelCount": 3
    // Long, 启用的模型数量
  },
  "timestamp": 1778578200000
}
```

> 所有统计数据均为**真实实时计算**，不是假数据。

---

## 5. 聊天对话 ChatController

> 路径前缀: `/chat`  
> **必须带 Header**: `X-API-Key: sk-xxxxx`（API Key 由 `/api-key/generate` 接口获取）

### 拦截器行为链（前端了解即可）

Chat 请求经过 `ApiKeyAuthInterceptor` 进行以下校验：

1. 验证 API Key 是否存在且未被禁用/过期
2. 查询 Key 对应的用户是否未被禁用
3. 限流检查（Redis Lua 滑动窗口）
4. 检查用户是否有 UNLIMITED 免费策略
    - 有：跳过余额检查
    - 无：检查余额 > 0，预扣 $0.1
5. 通过后放行到 ChatController

---

### 5.1 非流式对话 ⭐

```
POST /chat/completions
```

**Header:**

```
X-API-Key: sk-3457a754a5534a668f87fb33c367e6f0
Content-Type: application/json
```

**请求体：**

```json
{
  "model": "deepseek-v4-pro",
  "messages": [
    {
      "role": "system",
      "content": "你是一个有用的助手"
    },
    {
      "role": "user",
      "content": "用一句话介绍Java"
    }
  ],
  "maxTokens": 200,
  "temperature": 0.7
}
```

**字段说明：**

| 字段                 | 类型      | 必填 | 说明                                                              |
|--------------------|---------|----|-----------------------------------------------------------------|
| model              | String  | ✅  | 模型名：`deepseek-v4-pro` / `deepseek-v4-flash` / `deepseek-chat` 等 |
| messages           | Array   | ✅  | 消息数组，每条含 role + content                                         |
| messages[].role    | String  | ✅  | `system` / `user` / `assistant`                                 |
| messages[].content | String  | ✅  | 消息内容                                                            |
| maxTokens          | Integer | 选填 | 最大生成 Token 数，范围 1-4096，默认不限制                                    |
| temperature        | Double  | 选填 | 温度参数，范围 0.0-2.0，默认模型值                                           |

**成功响应 (code=200)：**

```json
{
  "code": 200,
  "message": "操作成功",
  "data": "Java是一种具有跨平台特性的面向对象编程语言，凭借Java虚拟机实现"
  一次编写
  ，
  到处运行
  "。",
  "timestamp": 1778578200000
}
```

> data 直接是 AI 回复的纯文本字符串。

**失败示例：**

```json
{
  "code": 401,
  "message": "API Key无效或已过期",
  "data": null,
  "timestamp": 1778578200000
}
{
  "code": 429,
  "message": "请求过于频繁，请稍后重试",
  "data": null,
  "timestamp": 1778578200000
}
{
  "code": 402,
  "message": "余额不足",
  "data": null,
  "timestamp": 1778578200000
}
{
  "code": 404,
  "message": "模型不存在",
  "data": null,
  "timestamp": 1778578200000
}
```

---

### 5.2 流式对话（SSE）⭐

```
POST /chat/stream
```

**Header 和请求体**：与非流式相同。

**响应格式**：`text/event-stream` (SSE)

**SSE 事件类型：**

| 事件名       | 触发时机            | data 格式                                                                   |
|-----------|-----------------|---------------------------------------------------------------------------|
| `message` | 每次收到模型输出的 token | `{"content": "Java"}`                                                     |
| `done`    | 对话完成            | `{"finished":true, "inputTokens":15, "outputTokens":38, "cost":0.000008}` |
| `error`   | 发生错误            | `"错误描述字符串"`                                                               |

**done 事件的 data 字段：**

| 字段           | 类型         | 说明                 |
|--------------|------------|--------------------|
| finished     | Boolean    | 始终为 true           |
| inputTokens  | Integer    | 输入 Token 数（字符估算）   |
| outputTokens | Integer    | 输出 Token 数（字符估算）   |
| cost         | BigDecimal | 本次调用费用（美元），已从余额中扣除 |

**前端 SSE 连接示例（JavaScript）：**

```javascript
const eventSource = new EventSource('/api/chat/stream'); // 注意：实际应使用 fetch + ReadableStream，因为请求是 POST
// 推荐使用 fetch API 处理 SSE：
const response = await fetch('/api/chat/stream', {
    method: 'POST',
    headers: {
        'Content-Type': 'application/json',
        'X-API-Key': 'sk-xxxxx'
    },
    body: JSON.stringify({
        model: 'deepseek-v4-pro',
        messages: [{role: 'user', content: '你好'}]
    })
});

const reader = response.body.getReader();
const decoder = new TextDecoder();
let buffer = '';

while (true) {
    const {done, value} = await reader.read();
    if (done) break;

    buffer += decoder.decode(value, {stream: true});
    const lines = buffer.split('\n');
    buffer = lines.pop() || '';

    for (const line of lines) {
        if (line.startsWith('data: ')) {
            const data = JSON.parse(line.slice(6));
            // 处理 data
        }
    }
}
```

---

## 错误码全集

| code | HTTP | 枚举名                  | message          | 触发场景         |
|------|------|----------------------|------------------|--------------|
| 200  | 200  | SUCCESS              | 操作成功             | 请求成功         |
| 400  | 200  | PARAM_ERROR          | 参数错误             | 参数校验失败、参数缺失  |
| 401  | 200  | UNAUTHORIZED         | 未授权              | 通用未授权        |
| 401  | 200  | LOGIN_ERROR          | 用户名或密码错误         | 登录失败         |
| 401  | 200  | INVALID_API_KEY      | API Key无效或已过期    | Key不存在或过期    |
| 402  | 200  | INSUFFICIENT_BALANCE | 余额不足             | 余额 ≤ 0       |
| 403  | 200  | FORBIDDEN            | 禁止访问             | 无权操作         |
| 403  | 200  | API_KEY_DISABLED     | API Key已被禁用      | Key status=0 |
| 403  | 200  | USER_DISABLED        | 用户已被禁用           | 用户 status=0  |
| 403  | 200  | MODEL_DISABLED       | 模型已禁用            | 模型 status=0  |
| 404  | 200  | NOT_FOUND            | 资源不存在            | 通用404        |
| 404  | 200  | USER_NOT_FOUND       | 用户不存在            | 用户ID不存在      |
| 404  | 200  | MODEL_NOT_FOUND      | 模型不存在            | 模型名不存在       |
| 429  | 200  | RATE_LIMIT_EXCEEDED  | 请求过于频繁，请稍后重试     | 超过限流阈值       |
| 500  | 200  | ERROR                | 操作失败/系统异常，请联系管理员 | 服务器内部异常      |

> **记忆要点**：所有响应 HTTP Status = 200，通过 `code` 字段区分成功/失败。前端只需 `if (response.code === 200)` 判断。

---

## 数据模型参考

### UserInfoVO（用户基本信息）

| 字段              | 类型              | 说明                         |
|-----------------|-----------------|----------------------------|
| id              | Long            | 用户ID                       |
| username        | String          | 用户名                        |
| email           | String          | 邮箱                         |
| balance         | BigDecimal      | 余额（美元）                     |
| status          | Integer         | 1=启用，0=禁用                  |
| role            | String          | USER / ADMIN / SUPER_ADMIN |
| freeApiStrategy | String\|null    | 免费策略类型                     |
| createTime      | String(ISO8601) | 注册时间                       |
| updateTime      | String(ISO8601) | 更新时间                       |

### LoginResponseVO（登录响应）

| 字段       | 类型         | 说明                |
|----------|------------|-------------------|
| token    | String     | 访问令牌，UUID格式，24h有效 |
| userInfo | UserInfoVO | 用户信息              |

### ApiKeyInfoVO（API Key 信息）

| 字段         | 类型                    | 说明               |
|------------|-----------------------|------------------|
| id         | Long                  | Key记录ID          |
| apiKey     | String                | Key值 `sk-{uuid}` |
| name       | String\|null          | 名称备注             |
| status     | Integer               | 1=启用，0=禁用        |
| rateLimit  | Integer               | 每分钟限流次数          |
| createTime | String(ISO8601)       | 创建时间             |
| expireTime | String(ISO8601)\|null | 过期时间             |

### AdminInfoVO（管理员视图的用户信息）

继承 UserInfoVO 所有字段，额外包含：

| 字段                    | 类型                    | 说明                |
|-----------------------|-----------------------|-------------------|
| freeQuota             | BigDecimal            | 免费额度（美元）          |
| dailyCallLimit        | Integer\|null         | 每日调用限制            |
| monthlyCallLimit      | Integer\|null         | 每月调用限制            |
| freeStrategyStartTime | String(ISO8601)\|null | 策略开始时间            |
| freeStrategyEndTime   | String(ISO8601)\|null | 策略结束时间            |
| allowedFreeModels     | String\|null          | 免费模型列表（JSON数组字符串） |

### SystemStatsVO（系统统计）

| 字段                | 类型         | 说明            |
|-------------------|------------|---------------|
| totalUsers        | Long       | 总用户数          |
| activeUsers       | Long       | 活跃用户数（7天内有调用） |
| adminCount        | Long       | 管理员数          |
| superAdminCount   | Long       | 超级管理员数        |
| disabledUsers     | Long       | 已禁用用户数        |
| totalApiCalls     | Long       | 总API调用次数      |
| todayApiCalls     | Long       | 今日API调用次数     |
| totalRevenue      | BigDecimal | 总消费金额         |
| todayRevenue      | BigDecimal | 今日消费金额        |
| avgCallCost       | BigDecimal | 平均单次调用费用      |
| freeStrategyUsers | Long       | 使用免费策略的用户数    |
| modelCount        | Long       | 模型总数          |
| activeModelCount  | Long       | 启用模型数         |

---

## 可用模型列表

| model               | provider | 状态          |
|---------------------|----------|-------------|
| `deepseek-v4-pro`   | deepseek | ✅ 启用        |
| `deepseek-v4-flash` | deepseek | ✅ 启用（需自行添加） |
| `deepseek-chat`     | deepseek | ✅ 启用        |

> OpenAI 模型 (gpt-3.5-turbo, gpt-4o) 已在数据库注册但 API Key 为占位符，需替换真实 Key 后方可使用。

---

## 前端最佳实践建议

1. **统一请求封装**：封装 `api.get(url)` / `api.post(url, data)` 方法，自动附加 `X-User-Token` Header
2. **Token 存储**：登录后将 `token` 存入 localStorage，每次请求自动携带
3. **Token 过期处理**：收到 401 时清除 token，跳转登录页
4. **充值后刷新余额**：调用充值接口成功后立即调用查询余额接口更新显示
5. **SSE 处理**：流式对话使用 fetch + ReadableStream，逐步拼接 content 展示打字效果
6. **错误处理**：根据 code 值显示对应中文 message，不需要额外映射

---

## Docker 部署

### 本机一键启动

```bash
./deploy.sh
```

### 导出镜像到 CentOS9 虚拟机

```bash
./deploy.sh export                    # 生成 ai-gateway-platform.tar
scp ai-gateway-platform.tar root@VM_IP:/opt/ai-gateway/
scp centos9-deploy.sh root@VM_IP:/opt/ai-gateway/
scp docker-compose.yml root@VM_IP:/opt/ai-gateway/
scp -r src/main/resources/sql/ root@VM_IP:/opt/ai-gateway/src/main/resources/sql/
ssh root@VM_IP "cd /opt/ai-gateway && ./centos9-deploy.sh"
```

### Docker 容器说明

| 容器               | 端口   | 说明                      |
|------------------|------|-------------------------|
| ai-gateway-app   | 8080 | Spring Boot 应用          |
| ai-gateway-mysql | 3306 | MySQL 8.0（root/root123） |
| ai-gateway-redis | 6379 | Redis 7（密码 redis123）    |

### Docker 常用命令

```bash
docker compose up -d       # 启动
docker compose down        # 停止
docker compose logs -f app # 查看应用日志
docker compose restart app # 重启应用
```
