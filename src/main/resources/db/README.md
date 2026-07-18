# 数据库脚本说明

## 文件说明

1. **schema.sql** - 数据库表结构脚本
   - 创建数据库和所有表
   - 包含索引和外键约束

2. **test-data.sql** - 测试数据脚本
   - 测试用户账号
   - 测试音乐数据
   - 测试上传记录和推荐记录
   - 测试反馈和通知数据

## 执行顺序

1. 先执行 `schema.sql` 创建表结构
2. 再执行 `test-data.sql` 插入测试数据

## 执行方法

### 方法一：命令行执行

```bash
# 执行表结构脚本
mysql -u root -p < src/main/resources/db/schema.sql

# 执行测试数据脚本
mysql -u root -p < src/main/resources/db/test-data.sql
```

### 方法二：MySQL客户端执行

1. 打开MySQL客户端
2. 连接到MySQL服务器
3. 依次执行两个SQL文件的内容

## 测试账号

### 管理员账号
- **用户名**: admin
- **密码**: admin123
- **角色**: ADMIN

### 普通用户账号
- **用户名**: user001 / user002 / user003
- **密码**: user123
- **角色**: USER

## 测试数据说明

### 用户数据
- 1个管理员账号
- 3个普通用户账号

### 音乐数据
- 12首测试音乐
- 包含流行、民谣、摇滚等风格
- 每首音乐都有特征向量（用于推荐算法）

### 上传记录
- 5条上传记录
- 包含不同状态（已完成、分析中等）

### 推荐记录
- 为多个上传记录生成了推荐结果
- 包含相似度分数和排名

### 反馈数据
- 多条用户反馈记录
- 包含点赞、评论等类型

## 注意事项

1. **密码加密**: 测试数据中的密码已使用BCrypt加密
2. **文件URL**: 音乐文件的URL指向MinIO，需要确保MinIO服务已启动
3. **特征向量**: 音乐特征向量是模拟数据，实际使用时需要AI服务提取
4. **重复执行**: 脚本使用了 `ON DUPLICATE KEY UPDATE`，可以安全地重复执行

## 生成新密码

如果需要生成新的BCrypt密码哈希值，可以运行：

```java
// 运行 PasswordGenerator.java
// 或使用以下代码：
BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
String hash = encoder.encode("your_password");
System.out.println(hash);
```

## 清空测试数据

如果需要清空所有测试数据（保留表结构），可以执行：

```sql
DELETE FROM `user_behavior`;
DELETE FROM `site_content`;
DELETE FROM `notice`;
DELETE FROM `feedback`;
DELETE FROM `recommend_record`;
DELETE FROM `upload_record`;
DELETE FROM `music`;
DELETE FROM `user` WHERE `username` IN ('admin', 'user001', 'user002', 'user003');
```

