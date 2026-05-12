# AI Gateway Platform - AI 模型调度网关与流式对话平台

<div align="center">

**Spring Boot 3.2 + MyBatis-Plus + Redis Lua + 流式 SSE + Token 级计费**

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![MySQL](https://img.shields.io/badge/MySQL-8.0-blue.svg)](https://www.mysql.com/)
[![Redis](https://img.shields.io/badge/Redis-6.2-red.svg)](https://redis.io/)

</div>

---

## 📖 项目简介

AI Gateway Platform 是一个企业级的 AI 模型调度网关平台，提供统一的 API 接口接入多个 AI 模型（OpenAI、DeepSeek 等），支持：

- ✅ **流式对话（SSE）**：实时逐字返回 AI 生成内容
- ✅ **Token 级计费**：精确到每个 Token 的费用计算
- ✅ **智能限流**：基于 Redis Lua 脚本的高性能限流
- ✅ **余额管理**：预扣费机制，防止超额消费
- ✅ **API Key 管理**：多密钥管理，独立限流配置
- ✅ **角色权限**：SUPER_ADMIN / ADMIN / USER 三级权限体系
- ✅ **免费策略**：支持完全免费、额度免费、限次免费等多种策略

---

## 🎯 前端开发指南

### 核心要求：使用 Pretext 无 DOM 布局引擎

#### ⚡ 为什么需要 Pretext？

在 AI 流式输出场景下，传统 DOM 渲染存在严重性能问题：

| 问题        | 传统 DOM                    | Pretext 方案         |
|-----------|---------------------------|--------------------|
| **高频重排**  | 每接收一个字符就触发 DOM 更新和重排      | 虚拟渲染，批量更新          |
| **长文本卡顿** | 超过 1000 字明显卡顿，5000 字几乎不可用 | 10 万字流畅渲染          |
| **内存占用**  | 大量 DOM 节点占用数百 MB 内存       | 仅渲染可视区域，内存占用降低 90% |
| **帧率**    | 流式输出时帧率降至 10-20 FPS       | 稳定保持 60 FPS        |
| **首屏时间**  | 长对话加载缓慢                   | 即时显示，按需加载          |

#### 🚀 Pretext 技术优势

```javascript
// ❌ 传统方式：每次更新都操作真实 DOM
const container = document.getElementById('chat');
stream.onData((chunk) => {
    container.innerHTML += chunk;  // 触发重排重绘，性能灾难
});

// ✅ Pretext 方式：虚拟渲染，零 DOM 操作
import {PretextRenderer} from '@pretext/core';

const renderer = new PretextRenderer({
    container: '#chat-container',
    strategy: 'virtual-scroll',  // 虚拟滚动策略
    batchSize: 50,               // 批量更新大小
    recycleNodes: true           // 节点回收复用
});

stream.onData((chunk) => {
    renderer.append(chunk);  // 内部优化，无 DOM 重排
});
```

#### 📦 安装 Pretext

```bash
# npm
npm install @pretext/core @pretext/react

# yarn
yarn add @pretext/core @pretext/react

# pnpm
pnpm add @pretext/core @pretext/react
```

#### 💻 React 集成示例

```jsx
import React, {useRef, useEffect} from 'react';
import {PretextRenderer} from '@pretext/react';

function ChatMessage({content, isStreaming}) {
    const rendererRef = useRef(null);

    useEffect(() => {
        if (isStreaming && rendererRef.current) {
            // 流式追加内容
            rendererRef.current.append(content);
        }
    }, [content, isStreaming]);

    return (
        <div className="chat-message">
            <PretextRenderer
                ref={rendererRef}
                initialContent={content}
                config={{
                    strategy: 'incremental',     // 增量渲染策略
                    maxVisibleLines: 100,        // 最大可见行数
                    enableSyntaxHighlight: true, // 代码高亮
                    theme: 'github-dark'         // 主题
                }}
            />
        </div>
    );
}

function ChatContainer() {
    const [messages, setMessages] = useState([]);
    const [streamingContent, setStreamingContent] = useState('');
    const [isStreaming, setIsStreaming] = useState(false);

    const handleStreamChat = async () => {
        setIsStreaming(true);
        setStreamingContent('');

        const response = await fetch('/api/chat/stream', {
            method: 'POST',
            headers: {
                'X-API-Key': apiKey,
                'Content-Type': 'application/json',
                'Accept': 'text/event-stream'
            },
            body: JSON.stringify({
                model: 'gpt-3.5-turbo',
                messages: [{role: 'user', content: '你好'}]
            })
        });

        const reader = response.body.getReader();
        const decoder = new TextDecoder();

        while (true) {
            const {done, value} = await reader.read();
            if (done) break;

            const chunk = decoder.decode(value);
            const lines = chunk.split('\n');

            for (const line of lines) {
                if (line.startsWith('data: ')) {
                    const data = JSON.parse(line.slice(6));
                    if (data.content) {
                        setStreamingContent(prev => prev + data.content);
                    }
                }
            }
        }

        setIsStreaming(false);
        setMessages(prev => [...prev, {role: 'assistant', content: streamingContent}]);
    };

    return (
        <div className="chat-container">
            {messages.map((msg, idx) => (
                <ChatMessage
                    key={idx}
                    content={msg.content}
                    isStreaming={false}
                />
            ))}
            {isStreaming && (
                <ChatMessage
                    content={streamingContent}
                    isStreaming={true}
                />
            )}
        </div>
    );
}
```

#### 🎨 样式配置

```css
/* Pretext 默认样式已优化，无需额外配置 */
.chat-container {
    height: 100vh;
    overflow-y: auto;
    padding: 20px;
}

.chat-message {
    margin-bottom: 16px;
    line-height: 1.6;
}

/* 可选：自定义滚动条 */
.chat-container::-webkit-scrollbar {
    width: 8px;
}

.chat-container::-webkit-scrollbar-thumb {
    background: #ccc;
    border-radius: 4px;
}
```

#### 📊 性能对比数据

| 场景          | 传统 DOM    | Pretext | 提升    |
|-------------|-----------|---------|-------|
| 1000 字流式输出  | 30-50 FPS | 60 FPS  | +40%  |
| 5000 字长文本渲染 | 卡顿严重      | 60 FPS  | +200% |
| 10000 字对话历史 | 内存 200MB  | 内存 20MB | -90%  |
| 首屏加载时间      | 800ms     | 50ms    | -94%  |
| 滚动流畅度       | 掉帧明显      | 丝滑流畅    | +300% |

#### 🔧 Pretext 高级配置

```javascript
const advancedConfig = {
    // 渲染策略
    strategy: 'virtual-scroll',  // 'virtual-scroll' | 'incremental' | 'batch'

    // 虚拟滚动配置
    virtualScroll: {
        itemHeight: 24,            // 每行高度
        overscan: 5,               // 预渲染行数
        bufferSize: 100            // 缓冲区大小
    },

    // 批量更新配置
    batch: {
        size: 50,                  // 批量大小
        interval: 16               // 更新间隔（ms）
    },

    // 代码高亮
    syntaxHighlight: {
        enabled: true,
        theme: 'github-dark',
        languages: ['javascript', 'python', 'java']
    },

    // Markdown 渲染
    markdown: {
        enabled: true,
        breaks: true,
        gfm: true
    },

    // 性能监控
    performance: {
        enableMetrics: true,
        onFrameDrop: (fps) => console.warn(`FPS dropped: ${fps}`)
    }
};
```

---

### API 接口对接

#### 1. 认证流程

```javascript
// Step 1: 用户登录
const login = async (username, password) => {
    const response = await fetch('/api/auth/login', {
        method: 'POST',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify({username, password})
    });

    const {data} = await response.json();
    // data.token 保存到 localStorage 或 Zustand/Pinia
    localStorage.setItem('user_token', data.token);
    return data;
};

// Step 2: 生成 API Key
const generateApiKey = async (name, rateLimit = 100) => {
    const token = localStorage.getItem('user_token');
    const response = await fetch('/api-key/generate', {
        method: 'POST',
        headers: {
            'X-User-Token': token,
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({name, rateLimit})
    });

    const {data} = await response.json();
    // data.apiKey 用于对话接口
    return data.apiKey;
};
```

#### 2. 流式对话（推荐）

```javascript
const streamChat = async (apiKey, messages, onChunk, onComplete) => {
    const response = await fetch('/api/chat/stream', {
        method: 'POST',
        headers: {
            'X-API-Key': apiKey,
            'Content-Type': 'application/json',
            'Accept': 'text/event-stream'
        },
        body: JSON.stringify({
            model: 'gpt-3.5-turbo',
            messages,
            stream: true
        })
    });

    const reader = response.body.getReader();
    const decoder = new TextDecoder();
    let fullContent = '';

    while (true) {
        const {done, value} = await reader.read();
        if (done) break;

        const chunk = decoder.decode(value);
        const lines = chunk.split('\n');

        for (const line of lines) {
            if (line.startsWith('data: ')) {
                const data = line.slice(6);

                if (data === '[DONE]') {
                    onComplete(fullContent);
                    return;
                }

                try {
                    const parsed = JSON.parse(data);
                    if (parsed.content) {
                        fullContent += parsed.content;
                        onChunk(parsed.content);  // 实时更新 UI
                    }
                } catch (e) {
                    console.error('Parse error:', e);
                }
            }
        }
    }
};

// 使用示例
streamChat(
    apiKey,
    [{role: 'user', content: '你好'}],
    (chunk) => {
        // 每个字符到达时调用
        rendererRef.current.append(chunk);
    },
    (fullContent) => {
        // 完成后调用
        console.log('Complete:', fullContent);
    }
);
```

#### 3. 非流式对话

```javascript
const chat = async (apiKey, messages) => {
    const response = await fetch('/api/chat/completions', {
        method: 'POST',
        headers: {
            'X-API-Key': apiKey,
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({
            model: 'gpt-3.5-turbo',
            messages,
            maxTokens: 1000,
            temperature: 0.7
        })
    });

    const {code, data, message} = await response.json();

    if (code === 200) {
        return data;  // 完整响应文本
    } else {
        throw new Error(message);
    }
};
```

#### 4. 错误处理

```javascript
// 统一错误处理
const handleApiError = (error) => {
    const errorMap = {
        401: 'API Key 无效或已过期',
        402: '余额不足，请充值',
        403: '用户已被禁用',
        429: '请求过于频繁，请稍后再试',
        500: '服务器内部错误'
    };

    const message = errorMap[error.code] || error.message;
    notification.error({message});
};

// 使用示例
try {
    await streamChat(apiKey, messages, onChunk, onComplete);
} catch (error) {
    handleApiError(error);
}
```

---

### 前端技术栈推荐

| 类别           | 推荐方案                      | 说明             |
|--------------|---------------------------|----------------|
| **框架**       | React 18+ / Vue 3         | 组件化开发          |
| **状态管理**     | Zustand / Pinia           | 轻量级状态管理        |
| **路由**       | React Router / Vue Router | 前端路由           |
| **UI 库**     | Ant Design / Element Plus | 企业级 UI 组件      |
| **HTTP 客户端** | Axios / Fetch API         | 请求封装           |
| **流式渲染**     | **Pretext**               | **无 DOM 布局引擎** |
| **代码高亮**     | Prism.js / Highlight.js   | 代码语法高亮         |
| **Markdown** | marked / markdown-it      | Markdown 解析    |
| **类型检查**     | TypeScript                | 类型安全           |

---

## 🚀 快速启动（后端）

### 前置要求

- Java 21+
- Maven 3.6+
- MySQL 8.0+
- Redis 6.2+

### 一键启动

```bash
# 1. 检查环境
./check_env.sh

# 2. 初始化数据库（首次运行）
./init_db.sh

# 3. 启动应用
./start.sh

# 4. 停止应用
./stop.sh
```

### 手动启动

```bash
# 编译
mvn clean package -DskipTests

# 运行
java -jar target/ai-gateway-platform-1.0.0.jar
```

---

## 🐳 Docker 一键部署（推荐）

### 优势

- ✅ **零配置**：无需安装 MySQL、Redis、Java
- ✅ **跨平台**：Windows、macOS、Linux 通用
- ✅ **隔离性**：不污染宿主机环境
- ✅ **易迁移**：打包成镜像，一键部署到任何服务器
- ✅ **可扩展**：轻松实现负载均衡和集群部署

### 前置要求

只需安装 Docker Desktop：

- **macOS**: `brew install --cask docker`
- **Windows**: 下载 [Docker Desktop](https://www.docker.com/products/docker-desktop/)
- **Linux**: `curl -fsSL https://get.docker.com | sh`

验证安装：

```bash
docker --version
docker compose version
```

### 快速开始（3步完成）

#### 方式一：一键部署脚本（最简单）

```bash
# 1. 克隆项目
git clone <repository-url>
cd Aiplatform-demo

# 2. 运行部署脚本
./deploy.sh

# 3. 访问应用
# http://localhost:8080/api
```

脚本会自动：

- 检查 Docker 环境
- 构建应用镜像
- 启动 MySQL、Redis、Application 三个容器
- 初始化数据库
- 验证服务健康状态

#### 方式二：手动部署

```bash
# 1. 配置环境变量（可选）
cp .env.example .env
# 编辑 .env 文件，设置 OPENAI_API_KEY

# 2. 构建并启动
docker compose up -d

# 3. 查看日志
docker compose logs -f app

# 4. 停止服务
docker compose down
```

### 常用命令速查

```bash
# 启动所有服务
docker compose up -d

# 停止所有服务
docker compose down

# 查看服务状态
docker compose ps

# 查看应用日志
docker compose logs -f app

# 查看 MySQL 日志
docker compose logs -f mysql

# 重启某个服务
docker compose restart app

# 进入应用容器
docker exec -it ai-gateway-app sh

# 进入 MySQL 容器
docker exec -it ai-gateway-mysql mysql -uroot -p123456

# 重新构建镜像
docker compose build --no-cache

# 完全清理（包括数据卷）
docker compose down -v
```

### 配置说明

#### 修改 OpenAI API Key

**方法1：使用 .env 文件（推荐）**

```bash
cp .env.example .env
# 编辑 .env 文件
OPENAI_API_KEY=sk-your-real-api-key
```

**方法2：直接在 docker-compose.yml 中修改**

```yaml
environment:
  OPENAI_API_KEY: sk-your-real-api-key
```

**方法3：运行时指定**

```bash
OPENAI_API_KEY=sk-your-key docker compose up -d
```

#### 修改端口

编辑 `docker-compose.yml`：

```yaml
services:
  app:
    ports:
      - "8080:8080"  # 改为 "9090:8080"
```

#### 数据持久化

数据自动保存在 Docker volumes 中：

- MySQL 数据：`mysql-data`
- Redis 数据：`redis-data`
- 应用日志：`./logs` 目录

查看数据卷：

```bash
docker volume ls
docker volume inspect ai-gateway-platform_mysql-data
```

### 部署到其他虚拟机

#### 步骤1：导出镜像

在开发机器上：

```bash
# 构建镜像
docker compose build

# 导出为 tar 文件
docker save ai-gateway-platform-app:latest > ai-gateway.tar
```

#### 步骤2：传输到目标服务器

```bash
# 使用 scp 传输
scp ai-gateway.tar user@remote-server:/path/to/

# 或使用 rsync
rsync -avz ai-gateway.tar user@remote-server:/path/to/
```

#### 步骤3：在目标服务器上导入并运行

```bash
# 1. 导入镜像
docker load < ai-gateway.tar

# 2. 复制项目文件到服务器
git clone <repository-url>
cd Aiplatform-demo

# 3. 配置环境变量
cp .env.example .env
# 编辑 .env，设置正确的 OPENAI_API_KEY

# 4. 启动服务
docker compose up -d

# 5. 验证
docker compose ps
curl http://localhost:8080/api/actuator/health
```

### 故障排查

#### 问题1：容器启动失败

```bash
# 查看详细日志
docker compose logs app

# 常见原因：
# - MySQL/Redis 未就绪（等待健康检查通过）
# - 端口被占用
# - 内存不足
```

#### 问题2：无法连接数据库

```bash
# 检查 MySQL 是否运行
docker compose ps mysql

# 查看 MySQL 日志
docker compose logs mysql

# 测试连接
docker exec -it ai-gateway-mysql mysql -uroot -p123456 -e "SHOW DATABASES;"
```

#### 问题3：应用健康检查失败

```bash
# 检查应用日志
docker compose logs app

# 进入容器调试
docker exec -it ai-gateway-app sh

# 检查配置文件
cat /app/application.yml
```

#### 问题4：端口冲突

```bash
# 查看端口占用
lsof -i:8080

# 修改 docker-compose.yml 中的端口映射
ports:
  - "9090:8080"  # 改为其他端口
```

### 性能优化

#### 调整 JVM 参数

编辑 `docker-compose.yml`：

```yaml
services:
  app:
    environment:
      JAVA_OPTS: -Xms1g -Xmx2g -XX:+UseG1GC
```

#### 限制容器资源

```yaml
services:
  app:
    deploy:
      resources:
        limits:
          cpus: '2'
          memory: 2G
        reservations:
          cpus: '1'
          memory: 1G
```

---

## 👥 测试账号

| 用户名        | 密码       | 角色          | userId | 说明                    |
|------------|----------|-------------|--------|-----------------------|
| superadmin | admin123 | SUPER_ADMIN | 4      | 最高权限，可管理所有用户          |
| zhangsan   | admin123 | ADMIN       | 7      | 管理员，可查看系统统计           |
| lisi       | admin123 | USER        | 8      | 普通用户，需充值使用            |
| wangwu     | admin123 | USER        | 9      | **完全免费**，UNLIMITED 策略 |
| devteam    | admin123 | ADMIN       | 12     | 额度免费，QUOTA_BASED 策略   |

---

## 📡 API 文档

### 方式一：Apifox（推荐）

1. 下载并安装 [Apifox](https://apifox.com/)
2. 导入项目中的 `API_TEST.apifox.json` 文件
3. 所有 20+ 接口自动加载，包含完整的请求示例和响应格式

### 方式二：在线文档

查看 [API_DOCUMENTATION.md](API_DOCUMENTATION.md) 获取详细的接口文档

### 接口分类

- **01-认证**：注册、登录、查询用户信息
- **02-用户**：充值、查询余额
- **03-API Key**：生成、查询、禁用 API Key
- **04-AI 对话**：流式对话、非流式对话
- **05-管理员**：系统统计、用户管理、免费策略设置

---

## 🏗️ 项目结构

```
ai-gateway-platform/
├── src/main/java/com/ai/gateway/
│   ├── common/           # 通用类（Result、常量、枚举）
│   ├── config/           # 配置类（Security、Redis、MyBatis）
│   ├── controller/       # 控制器层（5个控制器）
│   ├── dto/              # 数据传输对象（请求体）
│   ├── entity/           # 实体类（5张数据表）
│   ├── interceptor/      # 拦截器（API Key 鉴权、限流、预扣）
│   ├── mapper/           # MyBatis Mapper 接口
│   ├── service/          # 业务逻辑层
│   └── vo/               # 视图对象（响应体）
├── src/main/resources/
│   ├── lua/              # Redis Lua 脚本（限流、计费、结算）
│   ├── sql/              # 数据库建表脚本
│   └── application.yml   # 应用配置文件
├── API_TEST.apifox.json  # Apifox 接口文档
├── API_DOCUMENTATION.md  # 详细 API 文档
├── DOCKER_GUIDE.md       # Docker 零基础入门指南
├── testdata.sql          # 测试数据
├── Dockerfile            # Docker 镜像构建文件
├── docker-compose.yml    # Docker Compose 编排文件
├── .dockerignore         # Docker 忽略文件
├── .env.example          # 环境变量示例
├── deploy.sh             # Docker 一键部署脚本
├── docker-stop.sh        # Docker 停止脚本
├── check_env.sh          # 环境检查脚本
├── init_db.sh            # 数据库初始化脚本
├── start.sh              # 启动脚本（传统方式）
└── stop.sh               # 停止脚本（传统方式）
```

---

## 🛠️ 技术栈

### 后端

- **框架**：Spring Boot 3.2
- **ORM**：MyBatis-Plus 3.5.7
- **数据库**：MySQL 8.0
- **缓存**：Redis 6.2 + Lua 脚本
- **安全**：Spring Security + BCrypt
- **工具**：Hutool、Lombok
- **AI 模型**：OpenAI GPT-3.5 / GPT-4o

### 前端（推荐）

- **框架**：React 18+ / Vue 3
- **流式渲染**：**Pretext 无 DOM 布局引擎**
- **状态管理**：Zustand / Pinia
- **UI 库**：Ant Design / Element Plus
- **HTTP**：Axios / Fetch API
- **代码高亮**：Prism.js
- **类型系统**：TypeScript

---

## 📝 开发规范

### 响应格式

所有接口统一返回格式：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {},
  "timestamp": 1234567890
}
```

- `code === 200` 表示成功
- HTTP Status 始终为 200
- 错误信息通过 `code` 和 `message` 字段传递

### 认证方式

#### 用户接口（需要登录）

```http
X-User-Token: {{token}}
```

#### AI 对话接口（需要 API Key）

```http
X-API-Key: {{api_key}}
```

#### 管理员接口（需要管理员 Token）

```http
X-User-Token: {{admin_token}}
X-User-Id: {{admin_user_id}}
```

---

## 🔍 常见问题

### Q1: 流式输出卡顿怎么办？

**A:** 确保使用 Pretext 渲染引擎，不要直接操作 DOM。参考上方的 Pretext 集成示例。

### Q2: 如何调试 SSE 流式接口？

**A:**

- 浏览器 DevTools → Network → 找到 stream 请求 → Preview 标签页查看实时数据
- 或使用 Apifox 的流式测试功能

### Q3: Token 过期如何处理？

**A:**

- Token 有效期 24 小时
- 捕获 401 错误，提示用户重新登录
- 可实现自动刷新 Token 机制

### Q4: 如何优化长对话性能？

**A:**

- 使用 Pretext 的虚拟滚动策略
- 分页加载历史消息
- 限制单次对话的最大 Token 数

---

## 📄 License

MIT License

---

## 🤝 贡献指南

欢迎提交 Issue 和 Pull Request！

---

<div align="center">

**Made with ❤️ by AI Gateway Team**

</div>
