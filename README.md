# 个性化音乐推荐系统后端

基于SpringBoot + MyBatis + MySQL + MinIO的个性化音乐推荐系统后端服务。

## 技术栈

- **框架**: SpringBoot 4.0.1
- **数据库**: MySQL 8.0+
- **ORM**: MyBatis 3.0.3
- **文件存储**: MinIO
- **身份认证**: JWT Token
- **密码加密**: BCrypt
- **Java版本**: 17

## 项目结构

```
src/main/java/com/example/personalmusicsystem/
├── config/              # 配置类
│   ├── MinioConfig.java
│   └── WebConfig.java
├── annotation/          # 自定义注解
│   └── RequireAdmin.java
├── interceptor/         # 拦截器
│   └── AuthInterceptor.java
├── util/                # 工具类
│   ├── JwtUtil.java
│   └── BCryptUtil.java
├── entity/              # 实体类
├── dto/                 # 数据传输对象
├── mapper/              # MyBatis Mapper接口
├── service/             # 服务层
│   ├── storage/         # 存储服务
│   └── ai/              # AI服务
└── controller/          # 控制器
    └── admin/           # 管理员控制器

src/main/resources/
├── application.properties
└── mapper/              # MyBatis XML映射文件
```

## 环境要求

1. **JDK**: 17+
2. **Maven**: 3.6+
3. **MySQL**: 8.0+
4. **MinIO**: 最新版本

## 快速开始

### 1. 数据库配置

执行SQL脚本创建数据库和表：

```bash
mysql -u root -p < src/main/resources/db/schema.sql
```

或手动执行 `src/main/resources/db/schema.sql` 文件。

### 2. MinIO配置

使用Docker启动MinIO：

```bash
docker run -d \
  -p 9000:9000 \
  -p 9001:9001 \
  --name minio \
  -v D:/minio-data:/data \
  -e "MINIO_ROOT_USER=minioadmin" \
  -e "MINIO_ROOT_PASSWORD=minioadmin" \
  minio/minio server /data --console-address ":9001"
```

访问MinIO控制台：http://localhost:9001
- 用户名: minioadmin
- 密码: minioadmin

在MinIO控制台中创建名为 `music-storage` 的存储桶。

### 3. 配置文件

修改 `src/main/resources/application.properties`：

```properties
# 数据库配置
spring.datasource.url=jdbc:mysql://localhost:3306/music_system?...
spring.datasource.username=root
spring.datasource.password=your_password

# MinIO配置
minio.endpoint=http://localhost:9000
minio.access-key=minioadmin
minio.secret-key=minioadmin
minio.bucket-name=music-storage
```

### 4. 运行项目

```bash
mvn clean install
mvn spring-boot:run
```

服务启动在：http://localhost:8080

## API接口文档

### 认证接口

- `POST /api/auth/register` - 用户注册
- `POST /api/auth/login` - 用户登录
- `GET /api/auth/current` - 获取当前用户信息

### 用户端接口

#### 文件上传
- `POST /api/user/upload` - 上传音频文件
- `GET /api/user/upload/{id}` - 获取上传记录详情
- `GET /api/user/upload/history` - 获取上传历史

#### 音频分析
- `POST /api/user/analyze/{uploadId}` - 分析音频并生成推荐
- `GET /api/user/recommend/{uploadId}` - 获取推荐列表

#### 音乐相关
- `GET /api/user/music/{id}` - 获取音乐详情
- `GET /api/user/music/list` - 获取音乐列表
- `GET /api/user/music/preview/{id}` - 获取试听片段URL

#### 个人信息
- `GET /api/user/profile` - 获取个人信息
- `PUT /api/user/profile` - 更新个人信息
- `POST /api/user/avatar` - 上传头像
- `PUT /api/user/password` - 修改密码

#### 反馈
- `POST /api/user/feedback` - 提交反馈
- `GET /api/user/feedback/list` - 获取反馈列表

### 管理员端接口

#### 用户管理
- `GET /api/admin/users` - 获取用户列表
- `GET /api/admin/users/{id}` - 获取用户详情
- `PUT /api/admin/users/{id}` - 更新用户信息
- `DELETE /api/admin/users/{id}` - 删除用户
- `PUT /api/admin/users/{id}/status` - 启用/禁用用户

#### 音乐管理
- `GET /api/admin/music` - 获取音乐列表
- `POST /api/admin/music` - 添加音乐
- `POST /api/admin/music/{id}/upload` - 上传音乐文件
- `PUT /api/admin/music/{id}` - 更新音乐信息
- `DELETE /api/admin/music/{id}` - 删除音乐

#### 仪表盘
- `GET /api/admin/dashboard/stats` - 获取统计数据

### 文件访问

- `GET /api/files/**` - 获取文件（音频播放）

## 认证说明

### Token使用

1. 登录后获取Token：
```json
{
  "code": 200,
  "message": "登录成功",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "userId": 1,
    "username": "user123",
    "role": "USER"
  }
}
```

2. 在请求头中携带Token：
```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

### 角色说明

- **USER**: 普通用户，可访问 `/api/user/**` 接口
- **ADMIN**: 管理员，可访问 `/api/admin/**` 接口

## AI服务集成

系统通过HTTP调用Python AI服务进行音频特征提取和相似度计算。

AI服务需要提供以下接口：

1. **特征提取**: `POST /api/extract_features`
   ```json
   {
     "file_url": "http://..."
   }
   ```

2. **相似度计算**: `POST /api/calculate_similarity`
   ```json
   {
     "features1": "...",
     "features2": "..."
   }
   ```

AI服务URL配置在 `application.properties` 中的 `ai.service.url`。

## 开发说明

### 数据库表结构

- `user` - 用户表
- `music` - 音乐库表
- `upload_record` - 用户上传记录表
- `recommend_record` - 推荐记录表
- `feedback` - 反馈表
- `notice` - 通知公告表
- `site_content` - 站点内容表
- `user_behavior` - 用户行为统计表

详细表结构见 `src/main/resources/db/schema.sql`。

### 文件存储

所有音频文件存储在MinIO中，目录结构：
- `uploads/` - 用户上传的音频文件
- `library/` - 音乐库文件
- `previews/` - 试听片段

## 注意事项

1. **密码加密**: 使用BCrypt加密，注册时自动加密存储
2. **Token过期**: 默认24小时，可在配置文件中修改
3. **文件大小限制**: 默认50MB，可在配置文件中修改
4. **支持的文件类型**: mp3, wav, flac, m4a, aac

## 后续开发

- [ ] 完善头像上传功能
- [ ] 实现通知公告管理
- [ ] 实现站点内容管理
- [ ] 添加Redis缓存
- [ ] 实现异步音频分析
- [ ] 添加单元测试

## 许可证

MIT License

