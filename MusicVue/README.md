# 个性化音乐推荐系统 - 前端

基于 Vue 3 + Element Plus + TypeScript 的现代化前端应用。

## 技术栈

- **框架**: Vue 3 (Composition API)
- **路由**: Vue Router 4
- **状态管理**: Pinia
- **UI组件库**: Element Plus
- **图表**: ECharts 5
- **构建工具**: Vite
- **样式**: SCSS + Tailwind CSS
- **类型检查**: TypeScript

## 项目结构

```
MusicVue/
├── src/
│   ├── assets/          # 静态资源
│   │   └── styles/      # 样式文件
│   ├── components/       # 组件
│   │   ├── common/      # 公共组件
│   │   └── charts/      # 图表组件
│   ├── views/           # 页面
│   │   ├── auth/        # 认证页面
│   │   ├── user/        # 用户页面
│   │   └── admin/       # 管理员页面
│   ├── router/          # 路由配置
│   ├── store/           # 状态管理
│   ├── api/             # API接口
│   ├── utils/           # 工具函数
│   ├── types/           # 类型定义
│   ├── App.vue          # 根组件
│   └── main.ts          # 入口文件
├── public/              # 公共资源
├── index.html           # HTML模板
└── package.json         # 依赖配置
```

## 快速开始

### 1. 安装依赖

```bash
cd MusicVue
npm install
```

### 2. 启动开发服务器

```bash
npm run dev
```

访问 http://localhost:3000

### 3. 构建生产版本

```bash
npm run build
```

## 功能特性

### 用户端
- ✅ 美观的登录/注册页面（渐变背景、粒子动画）
- ✅ 首页轮播图和推荐列表
- ✅ 音频文件上传（拖拽支持）
- ✅ 音频分析页面（MFCC雷达图）
- ✅ 推荐历史查看
- ✅ 个人信息管理
- ✅ 音频播放器组件

### 管理员端
- ✅ 数据仪表盘（统计卡片、图表）
- ✅ 用户管理（CRUD操作）
- ✅ 音乐库管理（CRUD、文件上传）

## 设计亮点

1. **渐变背景**: 使用线性渐变营造沉浸式体验
2. **动画效果**: GSAP动画、淡入淡出、悬浮效果
3. **响应式设计**: PC端优化，最小宽度1280px
4. **暗色模式**: 支持主题切换
5. **玻璃态效果**: 毛玻璃卡片设计
6. **图表可视化**: ECharts雷达图、折线图、饼图

## API配置

确保后端服务运行在 `http://localhost:8080`，或修改 `vite.config.ts` 中的代理配置。

## 注意事项

1. 首次运行需要安装依赖：`npm install`
2. 确保后端API服务已启动
3. 登录后Token会自动存储在localStorage
4. 管理员账号需要role为'ADMIN'才能访问管理后台

## 开发规范

- 使用TypeScript进行类型检查
- 组件使用Composition API
- 样式使用SCSS，遵循BEM命名规范
- 遵循ESLint和Prettier代码规范

