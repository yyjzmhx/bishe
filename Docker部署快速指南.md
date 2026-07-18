# Docker 部署快速指南

## 🚀 一键启动所有服务

### 前置要求

- 已安装 Docker Desktop（Windows/Mac）或 Docker Engine（Linux）
- 已安装 Docker Compose

### 快速启动步骤

```bash
# 1. 进入项目根目录
cd PersonalMusicSystem

# 2. 启动所有 Docker 服务
docker-compose up -d

# 3. 查看服务状态
docker-compose ps

# 4. 查看服务日志
docker-compose logs -f
```

### 服务说明

启动后会自动创建以下服务：

| 服务 | 容器名 | 端口 | 说明 |
|------|--------|------|------|
| MySQL | music-mysql | 3306 | 数据库服务 |
| Redis | music-redis | 6379 | 缓存服务（验证码存储） |
| MinIO | music-minio | 9000/9001 | 对象存储服务 |

### 初始化 MinIO

1. 访问 MinIO 控制台：http://localhost:9001
2. 登录信息：
   - 用户名：`admin`
   - 密码：`12345678`
3. 创建存储桶：
   - 点击左侧 "Buckets"
   - 点击 "Create Bucket"
   - 输入名称：`music-storage`
   - 点击 "Create Bucket"

### 常用命令

```bash
# 启动服务
docker-compose up -d

# 停止服务
docker-compose stop

# 停止并删除容器
docker-compose down

# 停止并删除容器和数据卷（⚠️ 会删除所有数据）
docker-compose down -v

# 重启服务
docker-compose restart

# 查看日志
docker-compose logs -f [service-name]

# 进入容器（调试用）
docker exec -it music-mysql bash
docker exec -it music-redis sh
docker exec -it music-minio sh
```

### 验证服务

#### MySQL
```bash
# 检查容器状态
docker ps | grep mysql

# 连接数据库（可选）
docker exec -it music-mysql mysql -uroot -proot music_system
```

#### Redis
```bash
# 检查容器状态
docker ps | grep redis

# 连接 Redis（可选）
docker exec -it music-redis redis-cli

# 测试 Redis
redis-cli ping  # 应返回 PONG
```

#### MinIO
```bash
# 检查容器状态
docker ps | grep minio

# 访问控制台
# 浏览器打开: http://localhost:9001
```

### 数据持久化

所有数据存储在 Docker 数据卷中：

- `mysql_data`: MySQL 数据文件
- `redis_data`: Redis 数据文件
- `minio_data`: MinIO 数据文件

即使删除容器，数据也不会丢失（除非使用 `docker-compose down -v`）。

### 故障排查

#### 端口冲突

如果端口被占用，修改 `docker-compose.yml` 中的端口映射：

```yaml
ports:
  - "3307:3306"  # 改为其他端口
```

#### 服务启动失败

```bash
# 查看详细日志
docker-compose logs [service-name]

# 重启服务
docker-compose restart [service-name]

# 重新创建容器
docker-compose up -d --force-recreate [service-name]
```

#### 数据丢失

如果数据丢失，检查数据卷：

```bash
# 查看数据卷
docker volume ls

# 检查数据卷详情
docker volume inspect personalmusicsystem_mysql_data
```

---

**提示**: 首次启动可能需要几分钟时间下载镜像和初始化数据库。

