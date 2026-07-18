# 个性化音乐推荐系统 — 完整类模型图

以下 5 张 Mermaid 图覆盖系统架构、数据库、实体类、DTO/VO、Python 音频分析服务。
在 VS Code 安装 "Markdown Preview Mermaid Support" 插件即可预览。

---

## 一、系统整体架构图

```mermaid
graph TB
    subgraph "前端 - Vue 3 SPA"
        V[Vue 3 + TypeScript]
        VP[Pinia 状态管理]
        VR[Vue Router]
        V3[Three.js 3D可视化]
        VE[ECharts 图表]
    end

    subgraph "后端 - Spring Boot"
        subgraph "Controller 层"
            C1[AuthController]
            C2[MusicController]
            C3[UploadController]
            C4[RecommendController]
            C5[PlaylistController]
            C6[FeedbackController]
            C7[UserController]
            C8[UserPersonalTagController]
            C9[Admin*Controller]
        end
        subgraph "Service 层"
            S1[UserService]
            S2[MusicService]
            S3[UploadService]
            S4[RecommendService]
            S5[PlaylistService]
            S6[FeedbackService]
            S7[FileService]
            S8[EmailService]
            S9[VerificationCodeService]
        end
        subgraph "AI 子模块"
            AI1[DynamicAIChatService]
            AI2[AudioAnalysisService]
            AI3[PythonAudioAnalysisClient]
            AI4[AudioMetadataExtractor]
            AI5[LocalAudioFeatureExtractor]
        end
        subgraph "基础设施"
            M1[MyBatis Mapper]
            M2[JWT Util]
            M3[BCrypt Util]
            M4[StorageService / MinIO]
            M5[AuthInterceptor]
        end
    end

    subgraph "外部存储"
        DB[(MySQL :3307<br/>music_system)]
        RD[(Redis :6379)]
        MN[(MinIO :9000<br/>music-storage)]
    end

    subgraph "Python 音频分析"
        PY[FastAPI :9008<br/>librosa 特征提取]
    end

    subgraph "AI 云服务"
        DS[DashScope<br/>qwen-turbo]
    end

    V --> C1 & C2 & C3 & C4 & C5 & C6 & C7 & C8 & C9
    C1 --> S1 & S9
    C2 --> S2
    C3 --> S3
    C4 --> S4
    C5 --> S5
    C6 --> S6
    C7 --> S1
    C8 --> S1
    S3 --> AI2 --> AI3 --> PY
    S3 --> AI5
    AI2 --> DS
    S1 & S2 & S3 & S4 & S5 & S6 --> M1
    M1 --> DB
    S1 --> M2 & M3
    S3 --> M4 --> MN
    C1 --> M5
    S3 --> RD
```

---

## 二、数据库 ER 图（13 张表）

```mermaid
erDiagram
    USER {
        bigint id PK "用户ID"
        varchar username UK "用户名"
        varchar phone UK "手机号"
        varchar email UK "邮箱"
        varchar password "密码(BCrypt)"
        varchar avatar "头像URL"
        varchar nickname "昵称"
        json preferences "偏好标签JSON"
        varchar role "USER/ADMIN"
        tinyint status "1正常 0禁用"
        datetime create_time
        datetime update_time
    }

    MUSIC {
        bigint id PK "音乐ID"
        varchar title "歌曲名"
        varchar artist "歌手"
        varchar album "专辑"
        varchar file_url "音频文件URL(MinIO)"
        varchar preview_url "试听片段URL"
        int duration "时长(秒)"
        varchar genre "风格/流派"
        text lyrics "歌词内容(LRC)"
        json features "MFCC等特征向量"
        varchar cover_url "封面图URL"
        int play_count "播放次数"
        int like_count "点赞数"
        tinyint status "1正常 0下架"
        datetime create_time
        datetime update_time
    }

    UPLOAD_RECORD {
        bigint id PK "上传ID"
        bigint user_id FK "用户ID"
        varchar file_url "文件URL(MinIO)"
        varchar file_name "原始文件名"
        bigint file_size "文件大小(字节)"
        varchar file_type "文件类型"
        varchar status "UPLOADING/ANALYZING/COMPLETED/FAILED"
        json features "提取的音频特征"
        text analysis_result "分析结果描述"
        varchar error_message "错误信息"
        datetime create_time
        datetime update_time
    }

    RECOMMEND_RECORD {
        bigint id PK "推荐ID"
        bigint upload_id FK "上传记录ID"
        bigint music_id FK "音乐ID"
        decimal similarity "相似度分数(0-1)"
        int rank "推荐排名"
        datetime create_time
    }

    FEEDBACK {
        bigint id PK "反馈ID"
        bigint user_id FK "用户ID"
        bigint recommend_id FK "推荐记录ID"
        bigint music_id FK "音乐ID"
        bigint parent_id FK "父评论ID(回复用)"
        varchar type "LIKE/DISLIKE/COMMENT"
        text content "评论内容"
        datetime create_time
    }

    COMMENT_LIKE {
        bigint id PK "ID"
        bigint comment_id FK "评论ID"
        bigint user_id FK "用户ID"
        datetime create_time
    }

    NOTICE {
        bigint id PK "公告ID"
        varchar title "标题"
        text content "内容"
        varchar type "SYSTEM/UPDATE/NEW_MUSIC"
        tinyint status "1发布 0草稿"
        datetime publish_time "发布时间"
        datetime create_time
        datetime update_time
    }

    SITE_CONTENT {
        bigint id PK "内容ID"
        varchar type "CAROUSEL/HOT_PLAYLIST/BANNER"
        varchar title "标题"
        text content "内容JSON"
        varchar image_url "图片URL"
        varchar link_url "链接URL"
        int sort_order "排序"
        tinyint status "1启用 0禁用"
        datetime create_time
        datetime update_time
    }

    VERIFICATION_CODE {
        bigint id PK "ID"
        varchar email "邮箱地址"
        varchar code "验证码(6位数字)"
        varchar type "RESET_PASSWORD"
        datetime expire_time "过期时间"
        tinyint is_used "0未使用 1已使用"
        datetime create_time
    }

    AI_CONFIG {
        bigint id PK "配置ID"
        varchar provider "dashscope/openai/baidu/tencent"
        varchar api_key "API密钥(加密)"
        varchar base_url "Base URL"
        varchar model "模型名称"
        decimal temperature "温度参数(0-2)"
        int timeout "超时时间(ms)"
        varchar analysis_strategy "description/transcribe"
        tinyint is_active "1激活 0禁用"
        datetime create_time
        datetime update_time
    }

    REPORT_TEMPLATE {
        bigint id PK "模板ID"
        varchar name "模板名称"
        text description "模板描述"
        json config "模板配置(fields/layout)"
        tinyint is_default "0否 1是"
        tinyint status "1启用 0禁用"
        datetime create_time
        datetime update_time
    }

    USER_PERSONAL_TAG {
        bigint id PK "标签ID"
        bigint user_id FK "用户ID"
        varchar name "标签名称"
        varchar color "标签颜色"
        int sort_order "排序顺序"
        datetime create_time
        datetime update_time
    }

    USER_BEHAVIOR {
        bigint id PK "行为ID"
        bigint user_id "用户ID"
        varchar action "VIEW/UPLOAD/PLAY/LIKE"
        varchar target_type "MUSIC/RECOMMEND"
        bigint target_id "目标ID"
        datetime create_time
    }

    %% ===== 关系 =====
    USER ||--o{ UPLOAD_RECORD : "上传"
    USER ||--o{ FEEDBACK : "反馈"
    USER ||--o{ USER_PERSONAL_TAG : "个性化标签"
    USER ||--o{ COMMENT_LIKE : "点赞评论"
    UPLOAD_RECORD ||--o{ RECOMMEND_RECORD : "推荐结果"
    MUSIC ||--o{ RECOMMEND_RECORD : "被推荐"
    MUSIC ||--o{ FEEDBACK : "被评价"
    RECOMMEND_RECORD ||--o{ FEEDBACK : "推荐反馈"
    FEEDBACK ||--o{ COMMENT_LIKE : "评论点赞"
    FEEDBACK ||--o{ FEEDBACK : "评论回复(parent_id)"
```

---

## 三、Java 实体类图

```mermaid
classDiagram
    class User {
        +Long id
        +String username
        +String phone
        +String email
        +String password
        +String avatar
        +String nickname
        +String preferences
        +String role
        +Integer status
        +LocalDateTime createTime
        +LocalDateTime updateTime
    }

    class Music {
        +Long id
        +String title
        +String artist
        +String album
        +String fileUrl
        +String previewUrl
        +Integer duration
        +String genre
        +String lyrics
        +String features
        +String coverUrl
        +Integer playCount
        +Integer likeCount
        +Integer status
        +LocalDateTime createTime
        +LocalDateTime updateTime
    }

    class UploadRecord {
        +Long id
        +Long userId
        +String fileUrl
        +String fileName
        +Long fileSize
        +String fileType
        +String status
        +String features
        +String analysisResult
        +String errorMessage
        +LocalDateTime createTime
        +LocalDateTime updateTime
    }

    class RecommendRecord {
        +Long id
        +Long uploadId
        +Long musicId
        +BigDecimal similarity
        +Integer rank
        +LocalDateTime createTime
        +Music music
    }

    class Feedback {
        +Long id
        +Long userId
        +Long recommendId
        +Long musicId
        +Long parentId
        +String type
        +String content
        +LocalDateTime createTime
    }

    class CommentLike {
        +Long id
        +Long commentId
        +Long userId
        +LocalDateTime createTime
    }

    class Notice {
        +Long id
        +String title
        +String content
        +String type
        +Integer status
        +LocalDateTime publishTime
        +LocalDateTime createTime
        +LocalDateTime updateTime
    }

    class SiteContent {
        +Long id
        +String type
        +String title
        +String content
        +String imageUrl
        +String linkUrl
        +Integer sortOrder
        +Integer status
        +LocalDateTime createTime
        +LocalDateTime updateTime
    }

    class AIConfig {
        +Long id
        +String provider
        +String apiKey
        +String baseUrl
        +String model
        +BigDecimal temperature
        +Integer timeout
        +String analysisStrategy
        +Boolean isActive
        +LocalDateTime createTime
        +LocalDateTime updateTime
    }

    class ReportTemplate {
        +Long id
        +String name
        +String description
        +String config
        +Boolean isDefault
        +Integer status
        +LocalDateTime createTime
        +LocalDateTime updateTime
    }

    class UserPersonalTag {
        +Long id
        +Long userId
        +String name
        +String color
        +Integer sortOrder
        +LocalDateTime createTime
        +LocalDateTime updateTime
    }

    %% ===== 关联 =====
    User "1" --> "0..*" UploadRecord : userId
    User "1" --> "0..*" Feedback : userId
    User "1" --> "0..*" UserPersonalTag : userId
    User "1" --> "0..*" CommentLike : userId
    UploadRecord "1" --> "0..*" RecommendRecord : uploadId
    Music "1" --> "0..*" RecommendRecord : musicId
    Music "1" --> "0..*" Feedback : musicId
    RecommendRecord "1" --> "0..*" Feedback : recommendId
    Feedback "1" --> "0..*" CommentLike : commentId
    Feedback "1" --> "0..*" Feedback : parentId
```

---

## 四、DTO/VO 请求-响应类图

```mermaid
classDiagram
    class Result~T~ {
        +Integer code
        +String message
        +T data
        +success(T) Result
        +error(String) Result
    }

    class LoginRequest {
        +String username
        +String password
    }

    class RegisterRequest {
        +String username
        +String password
        +String phone
        +String email
        +String emailCode
        +String nickname
    }

    class LoginResponse {
        +String token
        +Long userId
        +String username
        +String role
        +String nickname
        +String avatar
    }

    class FeedbackVO {
        +String username
        +String nickname
        +String musicTitle
        +String musicArtist
    }

    class CommentVO {
        +Long id
        +Long userId
        +Long musicId
        +Long parentId
        +String content
        +LocalDateTime createTime
        +String username
        +String nickname
        +String avatar
        +Long likeCount
        +Boolean isLiked
        +Long replyCount
        +List~CommentVO~ replies
    }

    class CommunityPostVO {
        +String musicTitle
        +String musicArtist
        +String musicCoverUrl
    }

    class UploadRecordVO {
        +String username
        +String nickname
    }

    class FavoriteMusicVO {
        +Long favoriteId
        +Long id
        +String title
        +String artist
        +String album
        +String fileUrl
        +String previewUrl
        +Integer duration
        +String genre
        +String lyrics
        +String coverUrl
        +Integer playCount
        +Integer likeCount
        +Integer status
        +LocalDateTime favoriteTime
    }

    Feedback <|-- FeedbackVO : extends
    CommentVO <|-- CommunityPostVO : extends
    UploadRecord <|-- UploadRecordVO : extends
    Result --> LoginResponse : data
    Result --> CommentVO : data
    Result --> FavoriteMusicVO : data
```

---

## 五、Python 音频分析服务模型

```mermaid
classDiagram
    class AnalyzeRequest {
        +String file_path
        +String file_name?
        +String title?
        +String artist?
        +String album?
        +String genre?
        +String lyrics?
        +String source_kind?
    }

    class AnalysisResponse {
        +String style
        +String emotion
        +String rhythm
        +List~String~ instruments
        +String atmosphere
        +String description
        +List~String~ keywords
        +List~float~ vector
        +List~String~ vectorLabels
        +String technicalSummary
        +String extractionMode
        +int durationSeconds
        +int sampleRate
        +String tagTitle
        +String tagArtist
        +String tagAlbum
        +String tagGenre
        +Extra extra
    }

    class Extra {
        +float tempoBpm
        +String detectedKey
        +float spectralFlatness
        +float percussiveRatio
        +List~float~ mfccMean
    }

    class FeatureVector {
        +float energy
        +float rhythmActivity
        +float brightness
        +float dynamicRange
        +float textureComplexity
    }

    AnalyzeRequest --> AnalysisResponse : /analyze
    AnalysisResponse --> Extra : extra
    AnalysisResponse --> FeatureVector : vector
```
