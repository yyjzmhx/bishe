# MinIO 配置说明

## 问题说明

如果遇到错误：`The Access Key Id you provided does not exist in our records`

这表示 MinIO 的 Access Key 和 Secret Key 配置不正确，或者 MinIO 服务未正确启动。

## 解决方案

### 1. 安装和启动 MinIO

#### Windows 系统

1. 下载 MinIO：
   ```powershell
   # 使用 PowerShell
   Invoke-WebRequest -Uri "https://dl.min.io/server/minio/release/windows-amd64/minio.exe" -OutFile "minio.exe"
   ```

2. 启动 MinIO（使用默认凭据）：
   ```powershell
   .\minio.exe server D:\minio-data --console-address ":9001"
   ```

3. 访问 MinIO 控制台：
   - 浏览器打开：http://localhost:9001
   - 默认用户名：`minioadmin`
   - 默认密码：`minioadmin`

#### Docker 方式（推荐）

```bash
docker run -d \
  -p 9000:9000 \
  -p 9001:9001 \
  --name minio \
  -e "MINIO_ROOT_USER=minioadmin" \
  -e "MINIO_ROOT_PASSWORD=minioadmin" \
  -v D:/minio-data:/data \
  minio/minio server /data --console-address ":9001"
```

### 2. 创建存储桶

1. 登录 MinIO 控制台（http://localhost:9001）
2. 点击左侧菜单的 "Buckets"
3. 点击 "Create Bucket"
4. 输入存储桶名称：`music-storage`
5. 点击 "Create Bucket" 完成创建

### 3. 配置应用

确保 `application.yml` 中的配置正确：

```yaml
minio:
  endpoint: http://localhost:9000
  access-key: minioadmin      # 与 MinIO 启动时的 ROOT_USER 一致
  secret-key: minioadmin      # 与 MinIO 启动时的 ROOT_PASSWORD 一致
  bucket-name: music-storage
  base-url: http://localhost:9000/music-storage
```

### 4. 自定义凭据

如果你修改了 MinIO 的默认凭据：

1. 启动 MinIO 时设置环境变量：
   ```bash
   set MINIO_ROOT_USER=your_access_key
   set MINIO_ROOT_PASSWORD=your_secret_key
   minio.exe server D:\minio-data --console-address ":9001"
   ```

2. 更新 `application.yml`：
   ```yaml
   minio:
     access-key: your_access_key
     secret-key: your_secret_key
   ```

### 5. 验证配置

启动应用后，检查日志中是否有以下信息：
```
MinIO存储桶创建成功: music-storage
```

如果没有，检查：
- MinIO 服务是否运行
- Access Key 和 Secret Key 是否正确
- 网络连接是否正常

## 常见问题

### Q: 如何检查 MinIO 是否运行？

A: 访问 http://localhost:9000 或 http://localhost:9001，如果能看到 MinIO 界面，说明服务正在运行。

### Q: 如何修改 MinIO 的 Access Key？

A: 
1. 停止 MinIO 服务
2. 设置环境变量 `MINIO_ROOT_USER` 和 `MINIO_ROOT_PASSWORD`
3. 重新启动 MinIO
4. 更新 `application.yml` 中的配置

### Q: Docker 方式如何持久化数据？

A: 使用 `-v` 参数挂载数据目录，例如：`-v D:/minio-data:/data`

### Q: 生产环境配置建议

A: 
- 使用强密码
- 配置 HTTPS
- 设置访问策略
- 定期备份数据

