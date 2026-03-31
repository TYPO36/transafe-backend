# 文档解析服务 - 需求文档

**版本**：v1.1
**日期**：2026-03-31
**作者**：TYPO

---

## 1. 功能概述

### 1.1 背景

提供文件/邮件的文本内容解析能力，将非结构化文档转为结构化数据存入Elasticsearch，为后续全文搜索和翻译提供基础。

### 1.2 核心功能

- 支持解析：PDF、DOC、DOCX、PPT、PPTX、XLS、XLSX、TXT、EML
- 提取纯文本内容
- 递归解析邮件附件及压缩包（无深度限制）
- 解析失败状态管理与密码重试机制
- 会员优先队列解析

---

## 2. 系统架构

```
┌─────────────────────────────────────────────────────────────────────────┐
│                              Kafka                                       │
│  ┌──────────────────────────┐    ┌──────────────────────────┐            │
│  │  document_parse_queue   │    │ document_parse_queue_    │            │
│  │     (普通队列)           │    │ priority                 │            │
│  │                          │    │     (优先队列)           │            │
│  └───────────┬──────────────┘    └───────────┬──────────────┘            │
│              │                              │                           │
└──────────────┼──────────────────────────────┼───────────────────────────┘
               │                              │
               ▼                              ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                         解析服务 (单机消费)                               │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐     │
│  │  普通消费者  │  │  优先消费者  │  │  解析器     │  │  ES写入     │     │
│  │              │─▶│              │─▶│  策略模式   │─▶│             │     │
│  └─────────────┘  └─────────────┘  └─────────────┘  └──────┬──────┘     │
└─────────────────────────────────────────────────────────────┼─────────────┘
                                                              │
                    ┌─────────────────┐                       │
                    │   ES索引         │◀─────────────────────┘
                    │ transafe_       │
                    │ documents       │
                    └─────────────────┘
```

---

## 3. ES索引设计

**索引名**：`transafe_documents`

```json
{
  "settings": {
    "analysis": {
      "analyzer": {
        "ik_analyzer": {
          "type": "custom",
          "tokenizer": "ik_max_word"
        }
      }
    }
  },
  "mappings": {
    "properties": {
      "file_id": { "type": "keyword" },
      "parent_id": { "type": "keyword", "description": "父文档file_id，顶层文档为null" },
      "root_id": { "type": "keyword", "description": "根文档file_id，用于全文检索时关联所有相关内容" },
      "file_name": {
        "type": "text",
        "analyzer": "ik_max_word",
        "fields": { "keyword": { "type": "keyword" } }
      },
      "file_size": { "type": "long" },
      "file_storage_path": { "type": "keyword" },
      "file_type": { "type": "keyword" },
      "content": {
        "type": "text",
        "analyzer": "ik_max_word",
        "store": true
      },
      "translated_content": {
        "type": "text",
        "analyzer": "ik_max_word",
        "doc_values": false
      },
      "parse_status": { "type": "keyword" },
      "parse_error_code": { "type": "integer" },
      "parse_error_message": { "type": "text" },
      "has_password": { "type": "boolean" },
      "is_attachment": { "type": "boolean", "description": "是否为附件" },
      "priority": { "type": "integer", "description": "优先级: 0普通, 1优先" },
      "metadata": {
        "properties": {
          "title": { "type": "text" },
          "author": { "type": "keyword" },
          "created_date": { "type": "date" },
          "email_from": { "type": "keyword" },
          "email_to": { "type": "keyword" },
          "email_cc": { "type": "keyword" },
          "email_subject": { "type": "text" },
          "email_date": { "type": "date" },
          "attachments": {
            "type": "nested",
            "properties": {
              "name": { "type": "text" },
              "size": { "type": "long" },
              "file_id": { "type": "keyword" }
            }
          }
        }
      },
      "created_at": { "type": "date" },
      "updated_at": { "type": "date" }
    }
  }
}
```

---

## 4. 数据库设计

### 4.1 document表

```sql
CREATE TABLE document (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    file_id VARCHAR(64) UNIQUE NOT NULL COMMENT '文件唯一标识(UUID)',
    parent_id VARCHAR(64) COMMENT '父文档file_id，顶层为null',
    root_id VARCHAR(64) COMMENT '根文档file_id，所有关联文档指向顶层',
    file_name VARCHAR(512) NOT NULL COMMENT '原始文件名',
    file_size BIGINT COMMENT '文件大小(字节)',
    file_storage_path VARCHAR(1024) NOT NULL COMMENT '文件存储路径',
    file_type VARCHAR(32) NOT NULL COMMENT '文件类型: pdf,doc,docx,ppt,pptx,xls,xlsx,txt,eml',
    parse_status VARCHAR(32) DEFAULT 'pending' COMMENT '状态: pending/parsing/parsed/failed',
    parse_error_code INT DEFAULT 0 COMMENT '错误码: 0成功, 3001密码保护, 3002不支持格式, 3003文件损坏, 3004解析超时',
    parse_error_message VARCHAR(1024) COMMENT '错误信息',
    password_provided VARCHAR(256) COMMENT '用户提供的密码(仅密码保护文件)',
    is_attachment TINYINT(1) DEFAULT 0 COMMENT '是否为附件',
    priority INT DEFAULT 0 COMMENT '优先级: 0普通, 1优先',
    retry_count INT DEFAULT 0 COMMENT '重试次数',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_root_id (root_id),
    INDEX idx_parent_id (parent_id),
    INDEX idx_parse_status (parse_status),
    INDEX idx_priority (priority),
    INDEX idx_created_at (created_at)
) COMMENT '文档解析表';
```

### 4.2 状态流转

```
┌─────────────────────────────────────────────────────────────────┐
│                                                                 │
│  pending ──▶ parsing ──┬──▶ parsed                              │
│       ▲                │                                         │
│       │                └──▶ failed (可重试)                       │
│       │                     │                                    │
│       │            ┌─────────┴─────────┐                          │
│       │            │                   │                          │
│       │     密码保护(3001)         其他错误                         │
│       │            │                   │                          │
│       │            ▼                   ▼                          │
│       │     可密码重试解析        需人工处理                         │
│       │            │                                           │
│       └────────────┘                                           │
│                                                                 │
│  注: 重试不走优先队列                                           │
└─────────────────────────────────────────────────────────────────┘
```

---

## 5. MQ队列设计

### 5.1 队列规划

| 队列名 | 用途 | 消费者优先级 |
|--------|------|-------------|
| `document_parse_queue` | 普通用户解析任务 | 低 |
| `document_parse_queue_priority` | 会员优先解析任务 | 高 |

### 5.2 消息体

```json
{
  "file_id": "uuid-string",
  "parent_id": "父文档file_id或null",
  "root_id": "根文档file_id或null",
  "file_storage_path": "/storage/path/to/file.pdf",
  "file_type": "pdf",
  "file_name": "原始文件名.pdf",
  "is_attachment": false,
  "password": "用户提供的密码(可选)",
  "priority": 0,
  "retry_count": 0,
  "timestamp": "2026-03-31T10:00:00Z"
}
```

### 5.3 消息发送规则

| 场景 | 目标队列 | priority字段 |
|------|----------|--------------|
| 普通用户上传 | `document_parse_queue` | 0 |
| 会员上传 | `document_parse_queue_priority` | 1 |
| 密码重试 | `document_parse_queue`（不走优先） | 0 |
| 解析过程中发现附件 | 继承父文档的priority | 继承 |

### 5.4 消费策略

- **优先队列消费者**：同时监听两个队列，优先消费 `document_parse_queue_priority`
- **普通队列消费者**：仅监听 `document_parse_queue`
- **消费者数量**：各1个（单机部署）

---

## 6. 解析流程

### 6.1 主流程

```
┌─────────────────────────────────────────────────────────────────┐
│                        解析服务消费者                            │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
                    ┌─────────────────┐
                    │  更新状态:parsing │
                    └────────┬────────┘
                              │
                              ▼
                    ┌─────────────────┐
                    │  获取存储路径    │
                    │  下载文件到本地   │
                    └────────┬────────┘
                              │
                              ▼
                    ┌─────────────────┐
                    │  根据file_type  │
                    │  选择解析器      │
                    └────────┬────────┘
                              │
          ┌───────────────────┼───────────────────┐
          │                   │                   │
          ▼                   ▼                   ▼
    ┌──────────┐       ┌──────────┐       ┌──────────┐
    │ PDF解析器 │       │ Office解析 │       │ 邮件解析器 │
    │          │       │          │       │          │
    │ • 文本   │       │ • 文本    │       │ • 主题   │
    │ • 密码   │       │ • 密码    │       │ • 发件人 │
    │   检测   │       │   检测    │       │ • 收件人 │
    │ • 元数据 │       │ • 元数据  │       │ • 正文   │
    └────┬─────┘       └────┬─────┘       │ • 附件   │
         │                  │            └────┬─────┘
         └──────────────────┼─────────────────┘
                            │
                            ▼
                    ┌─────────────────┐
                    │  提取纯文本内容   │
                    │  提取元数据      │
                    └────────┬────────┘
                             │
                             ▼
                    ┌─────────────────┐
                    │  检测附件/压缩包 │
                    │  (递归解析)      │
                    └────────┬────────┘
                             │
              ┌──────────────┴──────────────┐
              │                               │
              ▼                               ▼
      ┌──────────────┐               ┌──────────────┐
      │ 有附件/压缩包  │               │   无附件     │
      │              │               │             │
      │ • 上传原文件  │               └──────┬──────┘
      │   到存储服务  │                      │
      │              │                      ▼
      │ • 发送MQ     │              ┌─────────────────┐
      │   递归解析   │              │  存入ES         │
      │   (继承priority)│           │  更新DB状态     │
      │              │              └────────┬────────┘
      └──────────────┘                       │
                                              ▼
                                     ┌─────────────────┐
                                     │  递归等待子文档  │
                                     │  全部解析完成    │
                                     └────────┬────────┘
                                              │
                                              ▼
                                     ┌─────────────────┐
                                     │  完成           │
                                     └─────────────────┘
```

### 6.2 附件树构建

每个文件解析后，构建如下树结构：

```
root (邮件.eml)
├── file_id: uuid_root
├── file_name: "客户合同邮件.eml"
├── is_attachment: false
├── metadata.email_subject: "关于XX合同"
├── metadata.attachments: [
│     { name: "合同.pdf", file_id: "uuid_attach_1" },
│     { name: "附件.zip", file_id: "uuid_attach_2" }
│   ]
│
├── child[0] (附件1: 合同.pdf)
│   ├── file_id: uuid_attach_1
│   ├── parent_id: uuid_root
│   ├── root_id: uuid_root
│   ├── file_name: "合同.pdf"
│   ├── is_attachment: true
│   └── parse_status: "parsed"
│
└── child[1] (附件2: 附件.zip)
    ├── file_id: uuid_attach_2
    ├── parent_id: uuid_root
    ├── root_id: uuid_root
    ├── file_name: "附件.zip"
    ├── is_attachment: true
    │
    ├── child[0] (zip内文件)
    │   ├── file_id: uuid_zip_1
    │   ├── parent_id: uuid_attach_2
    │   ├── root_id: uuid_root
    │   ├── file_name: "报价单.xlsx"
    │   └── is_attachment: true
    │
    └── child[1] (zip内文件)
        ├── file_id: uuid_zip_2
        ├── parent_id: uuid_attach_2
        ├── root_id: uuid_root
        ├── file_name: "技术方案.docx"
        └── is_attachment: true
```

**前端展示**：递归渲染树，支持无限层级展开。

### 6.3 解析器职责

| 解析器 | 支持格式 | 提取内容 |
|--------|----------|----------|
| PdfParser | PDF | 文本内容、页数、作者(若有)、创建日期(若有)、密码保护检测 |
| OfficeParser | DOC/DOCX/PPT/PPTX/XLS/XLSX | 文本内容、标题、作者、创建日期、最后修改日期、密码保护检测 |
| TextParser | TXT | 纯文本内容 |
| EmailParser | EML | 邮件主题、发件人、收件人、CC、正文、附件列表、附件原始文件名 |

### 6.4 密码保护处理

```
解析器检测到密码保护
        │
        ▼
┌───────────────────────┐
│ parse_error_code = 3001 │
│ has_password = true    │
│ 记录password_provided  │
└───────────┬────────────┘
            │
            ▼
    更新DB状态为failed
            │
            ▼
    用户调用 /retry 接口
    提交password参数
            │
            ▼
    发送MQ消息(priority=0, 不优先)
    重新解析
```

---

## 7. 接口设计

### 7.1 文件上传

```
POST /api/document/upload
Content-Type: multipart/form-data

Request:
  - file: File (单个文件)
  - user_id: Long (用户ID，用于判断会员身份)
  - is_attachment: boolean (可选，默认false，批量时不需要传)

Response:
{
  "success": true,
  "code": 200,
  "message": "上传成功",
  "data": {
    "file_id": "uuid",
    "file_name": "文件名.pdf",
    "file_size": 1024000,
    "file_type": "pdf",
    "parse_status": "pending"
  }
}
```

### 7.2 批量上传

```
POST /api/document/upload/batch
Content-Type: multipart/form-data

Request:
  - files: File[] (多个文件，建议≤100)
  - user_id: Long (用户ID)

Response:
{
  "success": true,
  "code": 200,
  "message": "批量上传成功",
  "data": {
    "total": 10,
    "success": 8,
    "failed": 2,
    "items": [
      { "file_id": "uuid1", "file_name": "a.pdf", "status": "pending" },
      { "file_id": "uuid2", "file_name": "b.pdf", "status": "pending" }
    ]
  }
}
```

### 7.3 查询解析状态

```
GET /api/document/{fileId}/status

Response:
{
  "success": true,
  "code": 200,
  "data": {
    "file_id": "uuid",
    "file_name": "文件名.pdf",
    "parse_status": "parsed",
    "parse_error_code": 0,
    "parse_error_message": null,
    "has_password": false,
    "is_attachment": false,
    "root_id": "uuid_root",
    "parent_id": null
  }
}
```

### 7.4 获取文档树(附件树)

```
GET /api/document/{fileId}/tree

Response:
{
  "success": true,
  "code": 200,
  "data": {
    "file_id": "uuid_root",
    "file_name": "客户合同邮件.eml",
    "file_type": "eml",
    "parse_status": "parsed",
    "is_attachment": false,
    "children": [
      {
        "file_id": "uuid_attach_1",
        "file_name": "合同.pdf",
        "file_type": "pdf",
        "parse_status": "parsed",
        "is_attachment": true,
        "children": []
      },
      {
        "file_id": "uuid_attach_2",
        "file_name": "附件.zip",
        "file_type": "zip",
        "parse_status": "parsed",
        "is_attachment": true,
        "children": [
          {
            "file_id": "uuid_zip_1",
            "file_name": "报价单.xlsx",
            "file_type": "xlsx",
            "parse_status": "parsed",
            "is_attachment": true,
            "children": []
          }
        ]
      }
    ]
  }
}
```

### 7.5 密码重试解析

```
POST /api/document/{fileId}/retry
Content-Type: application/json

Request:
{
  "password": "用户输入的密码"
}

Response:
{
  "success": true,
  "code": 200,
  "message": "解析任务已提交"
}
```

### 7.6 全文搜索

```
GET /api/document/search?keyword=关键词&page=1&size=20

Response:
{
  "success": true,
  "code": 200,
  "data": {
    "total": 100,
    "page": 1,
    "size": 20,
    "items": [
      {
        "file_id": "uuid",
        "file_name": "邮件主题.eml",
        "file_type": "eml",
        "highlight": "...匹配到的<em>关键词</em>...",
        "score": 1.5,
        "is_attachment": false,
        "children": [
          {
            "file_id": "uuid-att",
            "file_name": "附件.pdf",
            "highlight": "...<em>关键词</em>...",
            "score": 2.0
          }
        ]
      }
    ]
  }
}
```

### 7.7 获取文档详情

```
GET /api/document/{fileId}

Response:
{
  "success": true,
  "code": 200,
  "data": {
    "file_id": "uuid",
    "file_name": "文件名.pdf",
    "file_size": 1024000,
    "file_type": "pdf",
    "file_storage_path": "/storage/path",
    "content": "解析后的文本内容...",
    "translated_content": null,
    "parse_status": "parsed",
    "is_attachment": false,
    "root_id": "uuid",
    "parent_id": null,
    "metadata": {
      "title": "文档标题",
      "author": "作者",
      "created_date": "2026-01-01"
    },
    "children": [...],
    "created_at": "2026-03-31T10:00:00Z"
  }
}
```

---

## 8. 错误码

| 错误码 | 说明 | 是否可重试 | 处理方式 |
|--------|------|-----------|----------|
| 0 | 解析成功 | - | - |
| 3001 | 文件被密码保护 | ✅ | 用户输入密码后重试 |
| 3002 | 文件格式不支持 | ❌ | 标记失败，需人工处理 |
| 3003 | 文件已损坏 | ❌ | 标记失败，需人工处理 |
| 3004 | 解析超时(>5分钟) | ✅ | 自动重试，最多3次 |
| 3005 | 存储服务异常 | ✅ | 自动重试 |
| 3099 | 未知错误 | ✅ | 记录日志，人工处理 |

---

## 9. 会员识别与优先队列

### 9.1 会员判断

基于 `user_id` 查询用户会员状态（具体会员表结构不在本文档范围内）。

### 9.2 队列选择逻辑

```java
if (isVipUser(userId)) {
    sendToQueue("document_parse_queue_priority", message);
} else {
    sendToQueue("document_parse_queue", message);
}
```

### 9.3 消费者配置

```yaml
spring:
  kafka:
    consumer:
      group-id: document-parser
      auto-offset-reset: earliest
    listener:
      concurrency: 1
```

**注意**：优先队列和普通队列各配置一个消费者，优先消费者同时订阅两个队列。

---

## 10. 性能与限制

| 项目 | 限制 |
|------|------|
| 单文件大小 | 不限制 |
| 单次批量上传 | 建议≤100个文件 |
| 解析超时 | 5分钟/文件 |
| 重试次数 | 最多3次 |
| 支持文件编码 | UTF-8、GBK、GB2312 |
| ES分词器 | IK Max Word（中文分词） |
| 附件递归深度 | 无限制 |

---

## 11. 项目目录结构

```
src/main/java/com/benmake/transafe/
├── document/
│   ├── config/
│   │   ├── KafkaConfig.java
│   │   └── ElasticsearchConfig.java
│   ├── controller/
│   │   └── DocumentController.java
│   ├── service/
│   │   ├── DocumentService.java
│   │   ├── DocumentUploadService.java
│   │   ├── ParseService.java
│   │   └── SearchService.java
│   ├── parser/
│   │   ├── DocumentParser.java           # 解析器接口
│   │   ├── PdfParser.java
│   │   ├── OfficeParser.java
│   │   ├── TextParser.java
│   │   ├── EmailParser.java
│   │   └── ParserFactory.java
│   ├── mq/
│   │   ├── DocumentParseProducer.java
│   │   ├── NormalQueueConsumer.java      # 普通队列消费者
│   │   └── PriorityQueueConsumer.java    # 优先队列消费者
│   ├── entity/
│   │   └── DocumentEntity.java
│   ├── repository/
│   │   └── DocumentRepository.java
│   ├── dto/
│   │   ├── DocumentDTO.java
│   │   ├── DocumentTreeDTO.java
│   │   ├── ParseMessageDTO.java
│   │   └── SearchResultDTO.java
│   └── common/
│       ├── constant/
│       │   ├── DocumentType.java
│       │   └── ParseStatus.java
│       └── enums/
│           └── ParseErrorCode.java
```

---

## 12. 依赖建议

```xml
<!-- PDF解析 -->
<dependency>
    <groupId>org.apache.pdfbox</groupId>
    <artifactId>pdfbox</artifactId>
    <version>2.0.30</version>
</dependency>

<!-- Office文档解析 -->
<dependency>
    <groupId>org.apache.poi</groupId>
    <artifactId>poi</artifactId>
    <version>5.2.5</version>
</dependency>
<dependency>
    <groupId>org.apache.poi</groupId>
    <artifactId>poi-ooxml</artifactId>
    <version>5.2.5</version>
</dependency>

<!-- 邮件解析 -->
<dependency>
    <groupId> jakarta.mail</groupId>
    <artifactId>jakarta.mail-api</artifactId>
    <version>2.1.3</version>
</dependency>

<!-- ES -->
<dependency>
    <groupId>co.elastic.clients</groupId>
    <artifactId>elasticsearch-java</artifactId>
    <version>8.12.0</version>
</dependency>

<!-- 压缩包处理 -->
<dependency>
    <groupId>org.apache.commons</groupId>
    <artifactId>commons-compress</artifactId>
    <version>1.25.0</version>
</dependency>

<!-- Kafka -->
<dependency>
    <groupId>org.springframework.kafka</groupId>
    <artifactId>spring-kafka</artifactId>
</dependency>
```
