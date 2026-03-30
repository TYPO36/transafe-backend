# Transafe 业务模块设计文档

> **作者**: TYPO | **日期**: 2026-03-30 | **版本**: v1.0

---

## 1. 系统概述

### 1.1 系统定位

Transafe 业务模块是文件处理平台的**用户门户系统**，负责用户交互、业务逻辑编排、任务调度和结果交付。

### 1.2 技术栈

| 组件 | 技术选型 | 版本 |
|-----|---------|-----|
| 后端框架 | Spring Boot | 4.0.5 |
| Java | OpenJDK | 21 |
| 数据库 | MySQL | 8.0 |
| 缓存 | Redis | 7.x |
| 搜索引擎 | Elasticsearch | 8.x + IK 分词器 |
| 消息队列 | RabbitMQ | 3.12 |
| 安全框架 | Spring Security + JWT | - |

### 1.3 外部服务依赖

| 服务 | 说明 | 当前状态 |
|-----|------|---------|
| 文件上传服务 | 文件存储、下载、管理 | Mock 实现 |
| 文件解析服务 | 文件解析、内容提取 | Mock 实现 |

---

## 2. 系统架构

### 2.1 整体架构

```
┌─────────────────────────────────────────────────────────────────┐
│                        transafe (业务模块)                       │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐  │
│  │  auth   │ │  user   │ │ quota   │ │  task   │ │  file   │  │
│  │ 认证模块 │ │ 用户模块 │ │ 配额模块 │ │ 任务模块 │ │文件代理 │  │
│  └────┬────┘ └────┬────┘ └────┬────┘ └────┬────┘ └────┬────┘  │
│       │           │           │           │           │        │
│       └───────────┴───────────┴───────────┴───────────┘        │
│                              │                                  │
│  ┌───────────────────────────┴───────────────────────────────┐ │
│  │                        infra (基础设施层)                   │ │
│  │  ┌─────────────┐ ┌─────────────┐ ┌─────────────┐         │ │
│  │  │   MQ配置    │ │ 外部服务客户端│ │  工具类     │         │ │
│  │  │ (RabbitMQ)  │ │ (Feign/Rest)│ │             │         │ │
│  │  └─────────────┘ └─────────────┘ └─────────────┘         │ │
│  └───────────────────────────────────────────────────────────┘ │
│                                                                 │
│  ┌───────────────────────────────────────────────────────────┐ │
│  │                      common (公共层)                       │ │
│  │  异常定义 │ 响应封装 │ 常量 │ 工具类                       │ │
│  └───────────────────────────────────────────────────────────┘ │
│                                                                 │
│  ┌───────────────────────────────────────────────────────────┐ │
│  │                      config (配置层)                       │ │
│  │  Security │ Redis │ RabbitMQ │ Elasticsearch              │ │
│  └───────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────┘
```

### 2.2 模块职责

| 模块 | 职责 |
|-----|------|
| `auth` | JWT 认证、登录注册、Token 管理 |
| `user` | 用户信息管理、会员等级 |
| `quota` | 翻译配额、存储配额、消耗追踪 |
| `task` | 任务创建、状态管理、结果接收、选中翻译 |
| `file` | 文件上传/下载代理（调用文件上传服务） |
| `infra` | RabbitMQ 消息收发、外部服务 Mock 客户端、ES 操作 |

---

## 3. 数据流设计

### 3.1 文件上传 + 创建任务流程

```
用户                 业务模块                    文件上传服务           RabbitMQ           解析模块
 │                      │                           │                    │                    │
 │──(1) 上传文件───────►│                           │                    │                    │
 │                      │──(2) 校验存储配额          │                    │                    │
 │                      │──(3) 调用上传 API─────────►│                    │                    │
 │                      │                           │──存储文件到 MinIO──►│                    │
 │                      │◄──(4) 返回 fileId─────────│                    │                    │
 │                      │──(5) 创建任务记录          │                    │                    │
 │                      │──(6) 发送任务消息────────────────────────────────►                   │
 │                      │                           │                    │───────────────────►│
 │◄──(7) 返回 taskId───│                           │                    │                    │
```

### 3.2 接收解析结果流程

```
解析模块              RabbitMQ                   业务模块
    │                     │                         │
    │──解析完成──────────►│                         │
    │──发送结果消息───────►│                         │
    │                     │──(消费消息)────────────►│
    │                     │                         │──更新任务状态
    │                     │                         │──存储内容到 ES
    │                     │                         │──更新配额消耗
```

### 3.3 选中翻译流程

```
用户选中文本
     │
     ▼
前端发送: { taskId, paragraphId, selectedText, startIndex, endIndex }
     │
     ▼
┌─────────────────┐
│  Redis 缓存查询  │──── 命中 ────► 直接返回翻译结果
│  key: MD5(text) │
└────────┬────────┘
         │ 未命中
         ▼
┌─────────────────┐
│  调用翻译服务    │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│  存入 Redis 缓存 │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│  更新 ES 文档    │──── 将翻译追加到 translations 数组
└────────┬────────┘
         │
         ▼
返回翻译结果给前端
```

---

## 4. API 设计

### 4.1 认证模块

| 功能 | 方法 | API | 说明 |
|-----|------|-----|-----|
| 用户注册 | POST | `/api/auth/register` | 邮箱或手机号注册 |
| 用户登录 | POST | `/api/auth/login` | 返回 JWT Token |
| Token 刷新 | POST | `/api/auth/refresh` | 刷新 Token |

### 4.2 用户模块

| 功能 | 方法 | API | 说明 |
|-----|------|-----|-----|
| 获取当前用户 | GET | `/api/users/me` | 返回用户信息 |

### 4.3 配额模块

| 功能 | 方法 | API | 说明 |
|-----|------|-----|-----|
| 查询配额 | GET | `/api/quota/status` | 返回翻译和存储配额 |

### 4.4 文件模块

| 功能 | 方法 | API | 说明 |
|-----|------|-----|-----|
| 上传文件 | POST | `/api/files/upload` | multipart/form-data |
| 下载文件 | GET | `/api/files/{id}/download` | 返回文件流 |
| 文件列表 | GET | `/api/files` | 分页查询 |
| 删除文件 | DELETE | `/api/files/{id}` | 删除文件 |

### 4.5 任务模块

| 功能 | 方法 | API | 说明 |
|-----|------|-----|-----|
| 创建任务 | POST | `/api/tasks` | 创建解析任务 |
| 任务详情 | GET | `/api/tasks/{id}` | 查询任务状态和结果 |
| 任务列表 | GET | `/api/tasks` | 分页查询 |
| 选中翻译 | POST | `/api/tasks/{id}/translate` | 提交选中文本翻译 |
| 获取翻译 | GET | `/api/tasks/{id}/translations` | 获取所有翻译记录 |

---

## 5. 数据库设计

### 5.1 MySQL 表结构

#### 用户表 (user)

```sql
CREATE TABLE user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    email VARCHAR(100) UNIQUE,
    phone VARCHAR(20) UNIQUE,
    password VARCHAR(100) NOT NULL,
    nickname VARCHAR(50),
    avatar VARCHAR(200),
    membership_level INT DEFAULT 0,
    balance DECIMAL(10,2) DEFAULT 0,
    status VARCHAR(20) DEFAULT 'ACTIVE',
    created_at DATETIME,
    updated_at DATETIME,
    INDEX idx_email (email),
    INDEX idx_phone (phone)
);
```

#### 配额表 (quota)

```sql
CREATE TABLE quota (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT UNIQUE NOT NULL,
    daily_translation_total INT DEFAULT 5000,
    daily_translation_used INT DEFAULT 0,
    storage_total BIGINT DEFAULT 5368709120,
    storage_used BIGINT DEFAULT 0,
    last_reset_date DATE,
    created_at DATETIME,
    updated_at DATETIME,
    FOREIGN KEY (user_id) REFERENCES user(id)
);
```

#### 任务表 (task)

```sql
CREATE TABLE task (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    task_id VARCHAR(50) UNIQUE NOT NULL,
    user_id BIGINT NOT NULL,
    file_id VARCHAR(50) NOT NULL,
    file_name VARCHAR(255),
    file_type VARCHAR(50),
    status VARCHAR(20) DEFAULT 'PENDING',
    char_count INT DEFAULT 0,
    error_message TEXT,
    created_at DATETIME,
    completed_at DATETIME,
    INDEX idx_task_id (task_id),
    INDEX idx_user (user_id),
    INDEX idx_status (status),
    FOREIGN KEY (user_id) REFERENCES user(id)
);
```

### 5.2 Elasticsearch 索引结构

#### 段落索引 (paragraph)

```json
{
  "mappings": {
    "properties": {
      "taskId": { "type": "keyword" },
      "userId": { "type": "long" },
      "fileId": { "type": "keyword" },
      "paragraphId": { "type": "keyword" },
      "paragraphIndex": { "type": "integer" },
      "originalContent": {
        "type": "text",
        "analyzer": "ik_max_word"
      },
      "translations": {
        "type": "nested",
        "properties": {
          "id": { "type": "keyword" },
          "selectedText": { "type": "text" },
          "translatedText": { "type": "text" },
          "startIndex": { "type": "integer" },
          "endIndex": { "type": "integer" },
          "translatedAt": { "type": "date" }
        }
      },
      "createdAt": { "type": "date" }
    }
  }
}
```

### 5.3 Redis 缓存设计

| Key 格式 | 用途 | 过期时间 |
|---------|------|---------|
| `trans:cache:{md5}` | 翻译结果缓存 | 30 天 |
| `quota:daily:{userId}` | 每日配额使用量 | 当天有效 |
| `session:{userId}:{deviceId}` | 用户登录会话 | 7 天 |

### 5.4 存储分工

| 存储系统 | 存储内容 | 查询场景 |
|---------|---------|---------|
| **MySQL** | 用户、配额、任务元数据 | 业务查询、事务操作 |
| **Elasticsearch** | 解析内容、翻译记录 | 全文检索、段落查询 |
| **Redis** | 翻译缓存、配额缓存、会话 | 高频读取、缓存命中 |

---

## 6. 项目结构

```
com.benmake.transafe
├── TransafeApplication.java
│
├── config/                          # 配置层
│   ├── SecurityConfig.java
│   ├── JwtConfig.java
│   ├── RedisConfig.java
│   ├── ElasticsearchConfig.java
│   ├── RabbitMQConfig.java
│   └── WebConfig.java
│
├── common/                          # 公共层
│   ├── exception/
│   │   ├── BusinessException.java
│   │   ├── AuthException.java
│   │   ├── QuotaException.java
│   │   └── GlobalExceptionHandler.java
│   ├── response/
│   │   └── ApiResponse.java
│   ├── constants/
│   │   └── Constants.java
│   └── util/
│       └── Md5Util.java
│
├── auth/                            # 认证模块
│   ├── controller/
│   │   └── AuthController.java
│   ├── service/
│   │   ├── AuthService.java
│   │   └── JwtService.java
│   ├── filter/
│   │   └── JwtAuthenticationFilter.java
│   └── dto/
│       ├── LoginRequest.java
│       ├── RegisterRequest.java
│       └── TokenResponse.java
│
├── user/                            # 用户模块
│   ├── controller/
│   │   └── UserController.java
│   ├── service/
│   │   └── UserService.java
│   ├── repository/
│   │   └── UserRepository.java
│   ├── entity/
│   │   └── UserEntity.java
│   └── dto/
│       └── UserInfoResponse.java
│
├── quota/                           # 配额模块
│   ├── controller/
│   │   └── QuotaController.java
│   ├── service/
│   │   └── QuotaService.java
│   ├── repository/
│   │   └── QuotaRepository.java
│   ├── entity/
│   │   └── QuotaEntity.java
│   └── dto/
│       └── QuotaStatusResponse.java
│
├── file/                            # 文件代理模块
│   ├── controller/
│   │   └── FileController.java
│   ├── service/
│   │   └── FileProxyService.java
│   ├── client/
│   │   └── FileStorageClient.java
│   └── dto/
│       ├── FileUploadResponse.java
│       └── FileInfoResponse.java
│
├── task/                            # 任务模块
│   ├── controller/
│   │   └── TaskController.java
│   ├── service/
│   │   ├── TaskService.java
│   │   ├── TaskProducer.java
│   │   └── TranslationService.java
│   ├── repository/
│   │   └── TaskRepository.java
│   ├── entity/
│   │   └── TaskEntity.java
│   ├── consumer/
│   │   └── TaskResultConsumer.java
│   └── dto/
│       ├── TaskCreateRequest.java
│       ├── TaskResponse.java
│       ├── TranslateRequest.java
│       └── TranslateResponse.java
│
└── infra/                           # 基础设施层
    ├── elasticsearch/
    │   ├── ParagraphDocument.java
    │   └── ParagraphRepository.java
    ├── cache/
    │   └── TranslationCache.java
    └── mq/
        └── TaskMessage.java
```

---

## 7. 外部服务 Mock 策略

### 7.1 文件上传服务 Mock

| API | Mock 行为 |
|-----|----------|
| POST /api/internal/files/upload | 返回模拟 fileId，文件暂存本地临时目录 |
| GET /api/internal/files/{id}/download | 从本地临时目录读取文件返回 |
| GET /api/internal/files | 返回模拟文件列表 |
| DELETE /api/internal/files/{id} | 删除本地临时文件 |

### 7.2 文件解析服务 Mock

- 接收 RabbitMQ 任务消息
- 延迟 2-3 秒后返回模拟解析结果
- 结果包含多个段落的模拟文本内容

---

## 8. Docker Compose 配置

```yaml
version: '3.8'

services:
  mysql:
    image: mysql:8.0
    environment:
      MYSQL_ROOT_PASSWORD: root123
      MYSQL_DATABASE: transafe
    ports:
      - "3306:3306"
    volumes:
      - mysql_data:/var/lib/mysql

  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"
    volumes:
      - redis_data:/data

  elasticsearch:
    image: elasticsearch:8.11.0
    environment:
      - discovery.type=single-node
      - xpack.security.enabled=false
      - ES_JAVA_OPTS=-Xms512m -Xmx512m
    ports:
      - "9200:9200"
    volumes:
      - es_data:/usr/share/elasticsearch/data

  rabbitmq:
    image: rabbitmq:3.12-management
    ports:
      - "5672:5672"
      - "15672:15672"
    volumes:
      - rabbitmq_data:/var/lib/rabbitmq

volumes:
  mysql_data:
  redis_data:
  es_data:
  rabbitmq_data:
```

---

## 9. 实现阶段

### 第一阶段（本次实现）

- 用户认证（注册、登录、JWT）
- 用户信息查询
- 配额查询
- 文件代理（上传、下载、列表、删除）
- 任务管理（创建、查询、列表）
- 选中翻译功能
- 外部服务 Mock

### 第二阶段（后续扩展）

- 支付模块（充值、订单）
- 通知模块（站内消息）
- 审计模块（操作日志）
- WebSocket 实时推送

### 第三阶段（后续扩展）

- 会员等级升级逻辑
- 配额预警
- 管理员功能