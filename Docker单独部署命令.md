# Docker 单独部署命令参考

## 📦 Redis 部署

### 1. 拉取 Redis 镜像
```bash
docker pull redis:7-alpine
```

### 2. 创建并启动 Redis 容器
```bash
docker run -d \
  --name music-redis \
  -p 6379:6379 \
  -v redis_data:/data \
  --restart always \
  redis:7-alpine \
  redis-server --appendonly yes
```

**参数说明**:
- `-d`: 后台运行
- `--name music-redis`: 容器名称
- `-p 6379:6379`: 端口映射（主机:容器）
- `-v redis_data:/data`: 数据卷挂载（持久化数据）
- `--restart always`: 自动重启策略
- `redis-server --appendonly yes`: 启用 AOF 持久化

### 3. 验证 Redis 运行
```bash
# 检查容器状态
docker ps | grep redis

# 测试 Redis 连接
docker exec -it music-redis redis-cli ping
# 应返回: PONG

# 查看 Redis 信息
docker exec -it music-redis redis-cli info
```

### 4. Redis 常用命令
```bash
# 进入 Redis 容器
docker exec -it music-redis sh

# 连接 Redis CLI
docker exec -it music-redis redis-cli

# 查看所有键（验证码存储）
docker exec -it music-redis redis-cli keys "verification_code:*"

# 查看特定验证码
docker exec -it music-redis redis-cli get "verification_code:REGISTER:test@qq.com"

# 查看 Redis 日志
docker logs music-redis

# 查看实时日志
docker logs -f music-redis

# 停止 Redis 容器
docker stop music-redis

# 启动 Redis 容器
docker start music-redis

# 重启 Redis 容器
docker restart music-redis

# 删除 Redis 容器（⚠️ 会删除容器，但数据卷保留）
docker rm -f music-redis

# 删除 Redis 容器和数据卷（⚠️ 会删除所有数据）
docker rm -f music-redis
docker volume rm redis_data
```

---

## 📦 MinIO 部署

### 1. 拉取 MinIO 镜像
```bash
docker pull minio/minio:latest
```

### 2. 创建并启动 MinIO 容器
```bash
docker run -d \
  --name music-minio \
  -p 9000:9000 \
  -p 9001:9001 \
  -v minio_data:/data \
  -e "MINIO_ROOT_USER=admin" \
  -e "MINIO_ROOT_PASSWORD=12345678" \
  --restart always \
  minio/minio server /data --console-address ":9001"
```

**参数说明**:
- `-d`: 后台运行
- `--name music-minio`: 容器名称
- `-p 9000:9000`: API 端口映射
- `-p 9001:9001`: 控制台端口映射
- `-v minio_data:/data`: 数据卷挂载（持久化数据）
- `-e "MINIO_ROOT_USER=admin"`: 设置管理员用户名
- `-e "MINIO_ROOT_PASSWORD=12345678"`: 设置管理员密码
- `--restart always`: 自动重启策略
- `server /data --console-address ":9001"`: 启动服务器并指定控制台地址

### 3. 验证 MinIO 运行
```bash
# 检查容器状态
docker ps | grep minio

# 访问 MinIO 控制台
# 浏览器打开: http://localhost:9001
# 用户名: admin
# 密码: 12345678

# 测试 API 连接
curl http://localhost:9000/minio/health/live
```

### 4. 初始化 MinIO 存储桶

1. 访问 MinIO 控制台：http://localhost:9001
2. 使用账号密码登录（admin / 12345678）
3. 点击左侧菜单 "Buckets"
4. 点击 "Create Bucket"
5. 输入存储桶名称：`music-storage`
6. 点击 "Create Bucket" 完成创建

### 5. MinIO 常用命令
```bash
# 查看 MinIO 日志
docker logs music-minio

# 查看实时日志
docker logs -f music-minio

# 进入 MinIO 容器
docker exec -it music-minio sh

# 停止 MinIO 容器
docker stop music-minio

# 启动 MinIO 容器
docker start music-minio

# 重启 MinIO 容器
docker restart music-minio

# 删除 MinIO 容器（⚠️ 会删除容器，但数据卷保留）
docker rm -f music-minio

# 删除 MinIO 容器和数据卷（⚠️ 会删除所有数据）
docker rm -f music-minio
docker volume rm minio_data
```

---

## 📦 MySQL 部署（可选）

如果需要单独部署 MySQL：

### 1. 拉取 MySQL 镜像
```bash
docker pull mysql:8.0
```

### 2. 创建并启动 MySQL 容器
```bash
docker run -d \
  --name music-mysql \
  -p 3306:3306 \
  -e MYSQL_ROOT_PASSWORD=root \
  -e MYSQL_DATABASE=music_system \
  -e TZ=Asia/Shanghai \
  -v mysql_data:/var/lib/mysql \
  -v ./src/main/resources/db/schema.sql:/docker-entrypoint-initdb.d/schema.sql \
  --restart always \
  mysql:8.0 \
  --character-set-server=utf8mb4 \
  --collation-server=utf8mb4_unicode_ci
```

**参数说明**:
- `-e MYSQL_ROOT_PASSWORD=root`: 设置 root 用户密码
- `-e MYSQL_DATABASE=music_system`: 创建数据库
- `-v mysql_data:/var/lib/mysql`: 数据卷挂载
- `-v ./src/main/resources/db/schema.sql:/docker-entrypoint-initdb.d/schema.sql`: 自动执行 SQL 脚本

### 3. 验证 MySQL 运行
```bash
# 检查容器状态
docker ps | grep mysql

# 连接 MySQL
docker exec -it music-mysql mysql -uroot -proot music_system

# 查看数据库
docker exec -it music-mysql mysql -uroot -proot -e "SHOW DATABASES;"
```

---

## 📊 数据卷管理

### 查看所有数据卷
```bash
docker volume ls
```

### 查看数据卷详情
```bash
docker volume inspect redis_data
docker volume inspect minio_data
docker volume inspect mysql_data
```

### 删除数据卷（⚠️ 会删除所有数据）
```bash
docker volume rm redis_data
docker volume rm minio_data
docker volume rm mysql_data
```

### 备份数据卷
```bash
# 备份 Redis 数据
docker run --rm -v redis_data:/data -v $(pwd):/backup alpine tar czf /backup/redis_backup.tar.gz /data

# 备份 MinIO 数据
docker run --rm -v minio_data:/data -v $(pwd):/backup alpine tar czf /backup/minio_backup.tar.gz /data

# 备份 MySQL 数据
docker exec music-mysql mysqldump -uroot -proot music_system > mysql_backup.sql
```

---

## 🔍 故障排查

### 查看容器日志
```bash
# Redis
docker logs music-redis
docker logs -f music-redis  # 实时日志

# MinIO
docker logs music-minio
docker logs -f music-minio  # 实时日志

# MySQL
docker logs music-mysql
docker logs -f music-mysql  # 实时日志
```

### 检查端口占用
```bash
# Windows
netstat -ano | findstr :6379
netstat -ano | findstr :9000
netstat -ano | findstr :3306

# Linux/Mac
lsof -i :6379
lsof -i :9000
lsof -i :3306
```

### 重启所有服务
```bash
docker restart music-redis
docker restart music-minio
docker restart music-mysql
```

### 清理未使用的资源
```bash
# 删除所有停止的容器
docker container prune

# 删除所有未使用的镜像
docker image prune

# 删除所有未使用的数据卷
docker volume prune

# 清理所有未使用的资源
docker system prune -a
```

---

## 📝 完整部署示例

### 一次性部署所有服务

```bash
# 1. 拉取所有镜像
docker pull redis:7-alpine
docker pull minio/minio:latest
docker pull mysql:8.0

# 2. 启动 Redis
docker run -d \
  --name music-redis \
  -p 6379:6379 \
  -v redis_data:/data \
  --restart always \
  redis:7-alpine \
  redis-server --appendonly yes

# 3. 启动 MinIO
docker run -d \
  --name music-minio \
  -p 9000:9000 \
  -p 9001:9001 \
  -v minio_data:/data \
  -e "MINIO_ROOT_USER=admin" \
  -e "MINIO_ROOT_PASSWORD=12345678" \
  --restart always \
  minio/minio server /data --console-address ":9001"

# 4. 启动 MySQL
docker run -d \
  --name music-mysql \
  -p 3306:3306 \
  -e MYSQL_ROOT_PASSWORD=root \
  -e MYSQL_DATABASE=music_system \
  -e TZ=Asia/Shanghai \
  -v mysql_data:/var/lib/mysql \
  -v ./src/main/resources/db/schema.sql:/docker-entrypoint-initdb.d/schema.sql \
  --restart always \
  mysql:8.0 \
  --character-set-server=utf8mb4 \
  --collation-server=utf8mb4_unicode_ci

# 5. 验证所有服务
docker ps

# 6. 初始化 MinIO 存储桶
# 访问 http://localhost:9001，创建 music-storage 存储桶
```

---

**提示**: 推荐使用 `docker-compose.yml` 一键部署，更简单方便！

