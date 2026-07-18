# 个性化音乐推荐系统 — 时序图

> 每张图覆盖完整调用链：**Controller → Service → Mapper → Database / 外部服务**

---

## 一、用户认证与资料维护

### 1.1 用户注册

```mermaid
sequenceDiagram
    actor U as 用户
    participant AC as AuthController
    participant VCS as VerificationCodeService
    participant ES as EmailService
    participant US as UserService
    participant UM as UserMapper
    participant DB as MySQL
    participant RD as Redis

    Note over U,RD: 第一步：发送邮箱验证码
    U->>AC: POST /api/auth/register/send-code
    activate AC
    AC->>US: getByEmail(email)
    activate US
    US->>UM: selectByEmail(email)
    activate UM
    UM->>DB: SELECT FROM user WHERE email=?
    DB-->>UM: User / null
    deactivate UM
    US-->>AC: null (未注册)
    deactivate US
    AC->>VCS: generateAndSendCode(email, "REGISTER")
    activate VCS
    VCS->>VCS: generateCode() → 6位数字
    VCS->>RD: SET verification_code:REGISTER:email → code (TTL 600s)
    VCS->>ES: sendVerificationCode(email, code, "REGISTER")
    activate ES
    ES-->>VCS: void
    deactivate ES
    VCS-->>AC: void
    deactivate VCS
    AC-->>U: Result{code:200, message:"验证码已发送"}
    deactivate AC

    Note over U,RD: 第二步：提交注册
    U->>AC: POST /api/auth/register
    activate AC
    AC->>VCS: verifyAndDeleteCode(email, code, "REGISTER")
    activate VCS
    VCS->>RD: GET verification_code → storedCode
    RD-->>VCS: storedCode
    VCS->>VCS: code == storedCode ?
    VCS->>RD: DEL verification_code
    VCS-->>AC: true
    deactivate VCS
    AC->>US: register(user)
    activate US
    US->>UM: selectByUsername(username)
    UM->>DB: SELECT FROM user WHERE username=?
    DB-->>UM: null
    US->>UM: selectByPhone(phone)
    UM->>DB: SELECT FROM user WHERE phone=?
    DB-->>UM: null
    US->>UM: selectByEmail(email)
    UM->>DB: SELECT FROM user WHERE email=?
    DB-->>UM: null
    US->>US: BCryptUtil.encode(password)
    US->>US: setRole("USER"), setStatus(1)
    US->>UM: insert(user)
    UM->>DB: INSERT INTO user VALUES
    DB-->>UM: 1
    UM-->>US: 1
    US-->>AC: true
    deactivate US
    AC-->>U: Result{code:200, message:"注册成功"}
    deactivate AC
```

### 1.2 用户登录

```mermaid
sequenceDiagram
    actor U as 用户
    participant AC as AuthController
    participant US as UserService
    participant UM as UserMapper
    participant DB as MySQL
    participant JU as JwtUtil
    participant RD as Redis

    U->>AC: POST /api/auth/login
    activate AC
    AC->>US: login(username, password)
    activate US
    US->>UM: selectByUsername(username)
    activate UM
    UM->>DB: SELECT * FROM user WHERE username=?
    DB-->>UM: User
    deactivate UM
    US->>US: BCryptUtil.matches(password, user.password)
    US-->>AC: User
    deactivate US
    AC->>AC: user.status == 0 → 拒绝
    AC->>JU: generateToken(userId, username, role)
    activate JU
    JU-->>AC: token (JWT)
    deactivate JU
    AC->>RD: DEL verification_code
    AC-->>U: Result{token, userId, username, role, nickname, avatar}
    deactivate AC
```

### 1.3 忘记密码 → 重置密码

```mermaid
sequenceDiagram
    actor U as 用户
    participant AC as AuthController
    participant VCS as VerificationCodeService
    participant ES as EmailService
    participant US as UserService
    participant UM as UserMapper
    participant DB as MySQL
    participant RD as Redis

    Note over U,RD: 第一步：发送重置验证码
    U->>AC: POST /api/auth/forgot-password
    activate AC
    AC->>US: getByEmail(email)
    activate US
    US->>UM: selectByEmail(email)
    UM->>DB: SELECT FROM user WHERE email=?
    DB-->>UM: User
    UM-->>US: User
    US-->>AC: User
    deactivate US
    AC->>VCS: generateAndSendCode(email, "RESET_PASSWORD")
    activate VCS
    VCS->>RD: SET (TTL 60s)
    VCS->>ES: sendVerificationCode
    ES-->>VCS: void
    VCS-->>AC: void
    deactivate VCS
    AC-->>U: Result{message:"验证码已发送"}
    deactivate AC

    Note over U,RD: 第二步：重置密码
    U->>AC: POST /api/auth/reset-password
    activate AC
    AC->>VCS: verifyAndDeleteCode(email, code, "RESET_PASSWORD")
    activate VCS
    VCS->>RD: GET → verify → DEL
    VCS-->>AC: true
    deactivate VCS
    AC->>US: getByEmail(email)
    US->>UM: selectByEmail(email)
    UM->>DB: SELECT FROM user WHERE email=?
    DB-->>UM: User
    AC->>US: updatePassword(userId, newPassword)
    activate US
    US->>UM: update(user)
    UM->>DB: UPDATE user SET password=? WHERE id=?
    DB-->>UM: 1
    US-->>AC: true
    deactivate US
    AC-->>U: Result{message:"密码重置成功"}
    deactivate AC
```

### 1.4 获取/更新个人资料 & 上传头像 & 修改密码

```mermaid
sequenceDiagram
    actor U as 用户
    participant UC as UserController
    participant US as UserService
    participant UM as UserMapper
    participant DB as MySQL
    participant FS as FileService
    participant MN as MinIO

    Note over U,MN: getProfile → GET /api/user/profile
    U->>UC: getProfile()
    activate UC
    UC->>US: getById(userId)
    US->>UM: selectById(id)
    UM->>DB: SELECT FROM user WHERE id=?
    DB-->>UM: User
    UC->>UC: user.setPassword(null)
    UC-->>U: Result{User}
    deactivate UC

    Note over U,MN: uploadAvatar → POST /api/user/avatar
    U->>UC: uploadAvatar(file)
    activate UC
    UC->>UC: 校验图片类型 & ≤2MB
    UC->>FS: saveFile(file, "avatars/…", fileName)
    activate FS
    FS->>MN: PUT object
    MN-->>FS: fileUrl
    FS-->>UC: fileUrl
    deactivate FS
    UC->>US: getById(userId)
    US->>UM: selectById
    UM->>DB: SELECT
    DB-->>UM: User
    UC->>US: update(user)
    US->>UM: update
    UM->>DB: UPDATE user SET avatar=?
    DB-->>UM: 1
    UC-->>U: Result{avatarUrl}
    deactivate UC

    Note over U,MN: changePassword → PUT /api/user/password
    U->>UC: changePassword(oldPwd, newPwd)
    activate UC
    UC->>US: getById(userId)
    US->>UM: selectById
    UM->>DB: SELECT
    UC->>US: login(username, oldPwd)
    activate US
    US->>UM: selectByUsername
    UM->>DB: SELECT
    US->>US: BCryptUtil.matches(oldPwd, …)
    US-->>UC: User (验证通过)
    deactivate US
    UC->>US: update(user)
    US->>UM: update
    UM->>DB: UPDATE user SET password=?
    DB-->>UM: 1
    UC-->>U: Result{message:"密码修改成功"}
    deactivate UC
```

### 1.5 退出登录

```mermaid
sequenceDiagram
    actor U as 用户
    participant AC as AuthController
    participant PS as PlaylistService
    participant RD as Redis

    U->>AC: POST /api/auth/logout
    activate AC
    AC->>PS: clearUserData(userId)
    activate PS
    PS->>RD: DEL playlist:user:{userId}
    PS->>RD: DEL playstate:user:{userId}
    PS-->>AC: void
    deactivate PS
    AC-->>U: Result{message:"退出登录成功"}
    deactivate AC
```

---

## 二、音频 AI 推荐

### 2.1 用户上传音频 → AI 分析 → 生成推荐

```mermaid
sequenceDiagram
    actor U as 用户
    participant UC as UploadController
    participant US as UploadService
    participant FS as FileService
    participant MN as MinIO
    participant URM as UploadRecordMapper
    participant DB as MySQL
    participant RC as RecommendController
    participant RS as RecommendService
    participant AAS as AudioAnalysisService
    participant ABS as AudioAnalysisBridgeService
    participant PAC as PythonAudioAnalysisClient
    participant PY as FastAPI(librosa)
    participant LFE as LocalAudioFeatureExtractor
    participant AME as AudioMetadataExtractor
    participant DAI as DynamicAIChatService
    participant DS as DashScope
    participant MM as MusicMapper
    participant RRM as RecommendRecordMapper

    Note over U,RRM: 步骤1：上传音频
    U->>UC: POST /api/user/upload (MultipartFile)
    activate UC
    UC->>US: saveUploadRecord(userId, file)
    activate US
    US->>FS: saveUserUploadFile(file, userId)
    activate FS
    FS->>FS: validateFile(类型/大小)
    FS->>MN: PUT object → uploads
    MN-->>FS: fileUrl
    FS-->>US: Map{fileUrl, fileName, fileSize}
    deactivate FS
    US->>URM: insert(record)
    URM->>DB: INSERT INTO upload_record
    DB-->>URM: 1
    US-->>UC: UploadRecord
    deactivate US
    UC-->>U: Result{uploadId, fileUrl, status}
    deactivate UC

    Note over U,RRM: 步骤2：触发 AI 分析
    U->>RC: POST /api/user/analyze/{uploadId}
    activate RC
    RC->>US: getById(uploadId)
    US->>URM: selectById
    URM->>DB: SELECT
    DB-->>URM: UploadRecord
    RC->>RS: analyzeAndRecommend(uploadId)
    activate RS
    RS->>URM: update(status="ANALYZING")
    URM->>DB: UPDATE upload_record

    Note over RS,PY: 2a. 提取音频特征 (Python → 本地 fallback)
    RS->>AAS: extractFeatures(AudioAnalysisContext)
    activate AAS
    AAS->>ABS: analyze(context)
    activate ABS
    ABS->>PAC: analyzeFile(tempFilePath, context)
    activate PAC
    PAC->>PY: POST /analyze {file_path}
    activate PY
    PY->>PY: librosa.load() → rms/mfcc/tempo/…
    PY-->>PAC: Map{style, emotion, vector, extra}
    deactivate PY
    PAC-->>ABS: Map
    deactivate PAC
    ABS-->>AAS: Map (Python result)
    deactivate ABS

    alt Python 不可用
        AAS->>LFE: extract(context)
        activate LFE
        LFE->>AME: extract(context)
        activate AME
        AME-->>LFE: AudioMetadataSnapshot
        deactivate AME
        LFE->>LFE: tryExtractFromPcm / tryExtractFromBytes
        LFE-->>AAS: AudioFeatureProfile
        deactivate LFE
    end

    Note over AAS,DS: 2b. AI 语义增强
    AAS->>DAI: prompt(buildAiPrompt)
    activate DAI
    DAI->>DS: ChatClient.call(prompt)
    DS-->>DAI: JSON{style, emotion, …}
    DAI-->>AAS: String (JSON)
    deactivate DAI
    AAS->>AAS: mergeStructuredDescription → JSON
    AAS-->>RS: featuresJson
    deactivate AAS

    Note over RS,RRM: 2c. 相似度计算 & 推荐入库
    RS->>URM: update(status="COMPLETED", features, analysisResult)
    URM->>DB: UPDATE upload_record
    RS->>MM: selectByFeatures(features, 2000)
    MM->>DB: SELECT FROM music
    DB-->>MM: List<Music>
    loop 遍历每首曲库音乐
        RS->>AAS: calculateSimilarity(uploadFeatures, musicFeatures)
        AAS-->>RS: double (0~1)
    end
    RS->>RS: 排序 → Top20
    RS->>RRM: deleteByUploadId(uploadId)
    RRM->>DB: DELETE FROM recommend_record
    RS->>RRM: insertBatch(topRecords)
    RRM->>DB: INSERT INTO recommend_record (x20)
    RS-->>RC: void
    deactivate RS
    RC-->>U: Result{message:"分析完成"}
    deactivate RC
```

### 2.2 获取推荐结果

```mermaid
sequenceDiagram
    actor U as 用户
    participant RC as RecommendController
    participant US as UploadService
    participant URM as UploadRecordMapper
    participant DB as MySQL
    participant RS as RecommendService
    participant RRM as RecommendRecordMapper

    U->>RC: GET /api/user/recommend/{uploadId}
    activate RC
    RC->>US: getById(uploadId)
    US->>URM: selectById
    URM->>DB: SELECT
    RC->>RC: 校验 userId
    RC->>RS: getRecommendations(uploadId)
    activate RS
    RS->>RRM: selectByUploadId(uploadId)
    RRM->>DB: SELECT * FROM recommend_record ORDER BY rank
    DB-->>RRM: List<RecommendRecord>
    RS-->>RC: List<RecommendRecord>
    deactivate RS
    RC-->>U: Result{uploadId, recommendations[], count}
    deactivate RC
```

---

## 三、音乐浏览

```mermaid
sequenceDiagram
    actor U as 用户
    participant MC as MusicController
    participant MS as MusicService
    participant MM as MusicMapper
    participant DB as MySQL
    participant PC as PlaylistController
    participant PS as PlaylistService
    participant RD as Redis

    Note over U,RD: getMusicList → GET /api/user/music/list
    U->>MC: getMusicList(keyword, artist, genre, pageNum, pageSize)
    activate MC
    MC->>MS: getMusicList(keyword, artist, genre, 1, pageNum, pageSize)
    activate MS
    MS->>MM: selectList(keyword, artist, genre, status=1, offset, pageSize)
    MM->>DB: SELECT * FROM music WHERE status=1 LIMIT ?,?
    DB-->>MM: List<Music>
    MS->>MM: count(keyword, artist, genre, 1)
    MM->>DB: SELECT COUNT(*) FROM music
    DB-->>MM: total
    MS-->>MC: Map{list, total, pageNum, pageSize}
    deactivate MS
    MC-->>U: Result{list, total}
    deactivate MC

    Note over U,RD: getMusic → GET /api/user/music/{id}
    U->>MC: getMusic(id)
    activate MC
    MC->>MS: getById(id)
    MS->>MM: selectById(id)
    MM->>DB: SELECT * FROM music WHERE id=?
    DB-->>MM: Music
    MC-->>U: Result{Music}
    deactivate MC

    Note over U,RD: getPreviewUrl → GET /api/user/music/preview/{id}
    U->>MC: getPreviewUrl(id)
    activate MC
    MC->>MS: getById(id)
    MS->>MM: selectById
    MM->>DB: SELECT
    MC-->>U: Result{previewUrl}
    deactivate MC

    Note over U,RD: 保存/获取播放列表 (Redis)
    U->>PC: POST /api/user/playlist/save
    activate PC
    PC->>MS: getById(id) × N
    MS->>MM: selectById × N
    MM->>DB: SELECT × N
    PC->>PS: savePlaylist(userId, playlist)
    activate PS
    PS->>RD: SET playlist:user:{userId} (TTL 30d)
    PS-->>PC: void
    deactivate PS
    PC-->>U: Result{message:"保存成功"}
    deactivate PC

    U->>PC: GET /api/user/playlist/get
    activate PC
    PC->>PS: getPlaylist(userId)
    activate PS
    PS->>RD: GET playlist:user:{userId}
    RD-->>PS: JSON / null
    PS->>PS: parse JSON → List<Music>
    PS-->>PC: List<Music>
    deactivate PS
    PC-->>U: Result{playlist[]}
    deactivate PC

    Note over U,RD: 保存/获取播放状态 (Redis)
    U->>PC: POST /api/user/playlist/state/save
    activate PC
    PC->>PS: savePlayState(userId, musicId, index, mode, time, isPlaying)
    activate PS
    PS->>RD: SET playstate:user:{userId} (TTL 30d)
    PS-->>PC: void
    deactivate PS
    PC-->>U: Result{message:"保存成功"}
    deactivate PC

    U->>PC: GET /api/user/playlist/state/get
    activate PC
    PC->>PS: getPlayState(userId)
    activate PS
    PS->>RD: GET playstate:user:{userId}
    RD-->>PS: PlayState
    PS-->>PC: PlayState
    deactivate PS
    PC->>MS: getById(currentMusicId)
    MS->>MM: selectById
    MM->>DB: SELECT
    PC-->>U: Result{currentMusicId, currentIndex, playMode, currentTime, isPlaying, currentMusic}
    deactivate PC
```

---

## 四、社区互动

### 4.1 歌曲评论 & 回复 & 删除

```mermaid
sequenceDiagram
    actor U as 用户
    participant FC as FeedbackController
    participant FS as FeedbackService
    participant FM as FeedbackMapper
    participant DB as MySQL

    Note over U,DB: 发表评论
    U->>FC: POST /api/user/music/{musicId}/comment
    activate FC
    FC->>FS: submitFeedback(feedback)
    activate FS
    FS->>FM: insert(feedback)
    FM->>DB: INSERT INTO feedback (type="COMMENT")
    DB-->>FM: 1
    FS-->>FC: true
    deactivate FS
    FC-->>U: Result{message:"评论成功"}
    deactivate FC

    Note over U,DB: 回复评论
    U->>FC: POST /api/user/comment/{commentId}/reply
    activate FC
    FC->>FS: getFeedbackById(commentId)
    FS->>FM: selectById
    FM->>DB: SELECT
    FC->>FS: submitFeedback(reply) [parentId]
    FS->>FM: insert
    FM->>DB: INSERT INTO feedback (parent_id=?)
    FC-->>U: Result{message:"回复成功"}
    deactivate FC

    Note over U,DB: 删除评论
    U->>FC: DELETE /api/user/comment/{commentId}
    activate FC
    FC->>FS: getFeedbackById(commentId)
    FS->>FM: selectById
    FM->>DB: SELECT
    FC->>FC: 校验本人评论
    FC->>FS: deleteFeedback(commentId)
    FS->>FM: deleteById
    FM->>DB: DELETE FROM feedback
    FC-->>U: Result{message:"删除成功"}
    deactivate FC
```

### 4.2 评论点赞 & 社区动态 & 歌曲点赞 & 收藏

```mermaid
sequenceDiagram
    actor U as 用户
    participant FC as FeedbackController
    participant FS as FeedbackService
    participant FM as FeedbackMapper
    participant DB as MySQL

    Note over U,DB: 点赞/取消评论
    U->>FC: POST /api/user/comment/{commentId}/like
    activate FC
    FC->>FS: toggleCommentLike(commentId, userId)
    activate FS
    FS->>FM: selectById(commentId)
    FM->>DB: SELECT
    FS->>FM: checkCommentLike(userId, commentId)
    FM->>DB: SELECT COUNT(*)
    alt 已点赞 → 取消
        FS->>FM: deleteCommentLike(commentId, userId)
        FM->>DB: DELETE FROM comment_like
    else 未点赞 → 点赞
        FS->>FM: insertCommentLike(commentId, userId)
        FM->>DB: INSERT INTO comment_like
    end
    FS-->>FC: boolean
    deactivate FS
    FC->>FS: checkCommentLike(commentId, userId)
    FS->>FM: checkCommentLike
    FM->>DB: SELECT COUNT(*)
    FC-->>U: Result{isLiked}
    deactivate FC

    Note over U,DB: 获取评论列表
    U->>FC: GET /api/user/music/{musicId}/comments
    activate FC
    FC->>FS: getCommentsByMusicId(musicId, userId, pageNum, pageSize, sortType)
    FS->>FM: selectCommentsByMusicId
    FM->>DB: SELECT f.*, u.*, COUNT(cl.id) … JOIN … GROUP BY
    DB-->>FM: List<CommentVO>
    FS-->>FC: List<CommentVO>
    FC-->>U: Result{list, total}
    deactivate FC

    Note over U,DB: 获取社区动态
    U->>FC: GET /api/user/community
    activate FC
    FC->>FS: getCommunityFeed(userId, keyword, pageNum, pageSize, sortType)
    FS->>FM: selectCommunityFeedByLatest / selectCommunityFeedByHot
    FM->>DB: SELECT f.*, u.*, m.title, m.artist, m.cover_url FROM feedback f JOIN user u JOIN music m
    DB-->>FM: List<CommunityPostVO>
    FC-->>U: Result{list, total, sortType}
    deactivate FC

    Note over U,DB: 点赞/取消歌曲
    U->>FC: POST /api/user/music/{musicId}/like
    activate FC
    FC->>FS: checkUserLike(userId, musicId)
    FS->>FM: checkUserLike
    FM->>DB: SELECT COUNT(*)
    alt 已点赞 → 取消
        FC->>FS: deleteFeedback(id)
        FS->>FM: deleteById
        FM->>DB: DELETE
    else 未点赞 → 点赞
        FC->>FS: submitFeedback(type="LIKE")
        FS->>FM: insert
        FM->>DB: INSERT
    end
    FC-->>U: Result{isLiked}
    deactivate FC

    Note over U,DB: 收藏/取消歌曲
    U->>FC: POST /api/user/music/{musicId}/favorite
    activate FC
    FC->>FS: toggleFavorite(musicId, userId)
    activate FS
    FS->>FM: selectByUserAndMusicAndType(userId, musicId, "FAVORITE")
    FM->>DB: SELECT
    alt 已收藏 → 取消
        FS->>FM: deleteById(id)
        FM->>DB: DELETE
        FS-->>FC: false
    else 未收藏 → 收藏
        FS->>FM: insert(type="FAVORITE")
        FM->>DB: INSERT
        FS-->>FC: true
    end
    deactivate FS
    FC-->>U: Result{isFavorite, message}
    deactivate FC

    Note over U,DB: 获取收藏列表
    U->>FC: GET /api/user/favorites
    activate FC
    FC->>FS: getFavoriteMusicList(userId, keyword, pageNum, pageSize)
    FS->>FM: selectFavoriteMusicList
    FM->>DB: SELECT f.id as favoriteId, m.*, f.create_time as favoriteTime FROM feedback f JOIN music m
    DB-->>FM: List<FavoriteMusicVO>
    FC-->>U: Result{list, total}
    deactivate FC
```

---

## 五、内容资源管理（Admin）

### 5.1 音乐库 CRUD

```mermaid
sequenceDiagram
    actor AD as 管理员
    participant AMC as AdminMusicController
    participant MS as MusicService
    participant FS as FileService
    participant MN as MinIO
    participant MM as MusicMapper
    participant DB as MySQL
    participant AAS as AudioAnalysisService

    Note over AD,AAS: 添加音乐
    AD->>AMC: POST /api/admin/music (audioFile + coverFile)
    activate AMC
    AMC->>AMC: 校验文件类型/大小
    AMC->>FS: saveLibraryFile(audioFile, null)
    activate FS
    FS->>MN: PUT object → library/
    MN-->>FS: audioFileUrl
    FS-->>AMC: Map{fileUrl}
    deactivate FS
    opt 有封面图
        AMC->>FS: saveCoverImage(coverFile, null)
        FS->>MN: PUT object → covers/
        MN-->>FS: coverFileUrl
    end
    AMC->>MS: addMusic(music)
    activate MS
    MS->>MM: insert(music)
    MM->>DB: INSERT INTO music
    DB-->>MM: id
    MS->>AAS: refreshAudioFeatures
    AAS-->>MS: featuresJson
    MS->>MM: update(features, duration, genre)
    MM->>DB: UPDATE music
    MS-->>AMC: true
    deactivate MS
    AMC-->>AD: Result{musicId, fileUrl, coverUrl}
    deactivate AMC

    Note over AD,AAS: 更新音乐
    AD->>AMC: PUT /api/admin/music/{id}
    activate AMC
    AMC->>MS: updateMusic(music)
    MS->>MM: update(music)
    MM->>DB: UPDATE music
    MS->>AAS: refreshAudioFeatures(id)
    AMC-->>AD: Result{message:"更新成功"}
    deactivate AMC

    Note over AD,AAS: 删除音乐
    AD->>AMC: DELETE /api/admin/music/{id}
    activate AMC
    AMC->>MS: deleteMusic(id)
    MS->>MM: deleteById(id)
    MM->>DB: DELETE FROM music
    AMC-->>AD: Result{message:"删除成功"}
    deactivate AMC
```

### 5.2 上传记录 & 通知公告 & 站点内容

```mermaid
sequenceDiagram
    actor AD as 管理员
    participant AUC as AdminUploadController
    participant ANC as AdminNoticeController
    participant ASC as AdminSiteContentController
    participant FS as FileService
    participant MN as MinIO
    participant URM as UploadRecordMapper
    participant NM as NoticeMapper
    participant SCM as SiteContentMapper
    participant DB as MySQL

    Note over AD,DB: 上传记录管理
    AD->>AUC: GET /api/admin/uploads
    activate AUC
    AUC->>URM: selectList(keyword, status, startDate, endDate, offset, pageSize)
    URM->>DB: SELECT ur.*, u.username, u.nickname FROM upload_record ur JOIN user u
    DB-->>URM: List<UploadRecordVO>
    AUC-->>AD: Result{list, total}
    deactivate AUC

    AD->>AUC: DELETE /api/admin/uploads/{id}
    activate AUC
    AUC->>URM: selectById(id)
    URM->>DB: SELECT
    AUC->>FS: deleteFile(fileUrl)
    FS->>MN: DELETE object
    AUC->>URM: deleteById(id)
    URM->>DB: DELETE FROM upload_record
    AUC-->>AD: Result{message:"删除成功"}
    deactivate AUC

    Note over AD,DB: 通知公告 CRUD
    AD->>ANC: POST /api/admin/notices
    activate ANC
    ANC->>ANC: setStatus/publishTime
    ANC->>NM: insert(notice)
    NM->>DB: INSERT INTO notice
    DB-->>NM: id
    ANC-->>AD: Result{noticeId}
    deactivate ANC

    AD->>ANC: PUT /api/admin/notices/{id}/publish
    activate ANC
    ANC->>ANC: setStatus(1), setPublishTime(now)
    ANC->>NM: update
    NM->>DB: UPDATE notice SET status=1, publish_time=?
    ANC-->>AD: Result{message:"发布成功"}
    deactivate ANC

    Note over AD,DB: 站点内容 CRUD
    AD->>ASC: POST /api/admin/site-content
    activate ASC
    opt 有图片
        ASC->>FS: saveCoverImage(file, null)
        FS->>MN: PUT object
        MN-->>FS: imageUrl
    end
    ASC->>SCM: insert(siteContent)
    SCM->>DB: INSERT INTO site_content
    DB-->>SCM: id
    ASC-->>AD: Result{contentId}
    deactivate ASC

    AD->>ASC: PUT /api/admin/site-content/{id}/status
    activate ASC
    ASC->>SCM: update(status)
    SCM->>DB: UPDATE site_content SET status=?
    ASC-->>AD: Result
    deactivate ASC
```

---

## 六、用户社区与 AI 管理（Admin）

### 6.1 用户管理 & 反馈管理

```mermaid
sequenceDiagram
    actor AD as 管理员
    participant AUC as AdminUserController
    participant AFC as AdminFeedbackController
    participant US as UserService
    participant UM as UserMapper
    participant FM as FeedbackMapper
    participant DB as MySQL

    Note over AD,DB: 用户管理
    AD->>AUC: GET /api/admin/users
    activate AUC
    AUC->>UM: selectList(keyword, role, status, offset, pageSize)
    UM->>DB: SELECT * FROM user WHERE … LIMIT ?,?
    DB-->>UM: List<User>
    AUC->>AUC: forEach: setPassword(null)
    AUC-->>AD: Result{list, total}
    deactivate AUC

    AD->>AUC: PUT /api/admin/users/{id}/status
    activate AUC
    AUC->>US: update(user)
    US->>UM: update
    UM->>DB: UPDATE user SET status=? WHERE id=?
    AUC-->>AD: Result{message:"状态更新成功"}
    deactivate AUC

    Note over AD,DB: 反馈管理
    AD->>AFC: GET /api/admin/feedback
    activate AFC
    AFC->>FM: selectAdminList(keyword, type, startDate, endDate, offset, pageSize)
    FM->>DB: SELECT f.*, u.username, m.title FROM feedback f JOIN user u JOIN music m
    DB-->>FM: List<FeedbackVO>
    AFC-->>AD: Result{list, total}
    deactivate AFC

    AD->>AFC: DELETE /api/admin/feedback/{id}
    activate AFC
    AFC->>FM: deleteById(id)
    FM->>DB: DELETE FROM feedback WHERE id=?
    AFC-->>AD: Result{message:"删除成功"}
    deactivate AFC
```

### 6.2 AI 配置管理 & AI 监控 & 仪表盘

```mermaid
sequenceDiagram
    actor AD as 管理员
    participant AACC as AdminAIConfigController
    participant AAC as AdminAIController
    participant ADC as AdminDashboardController
    participant DAI as DynamicAIChatService
    participant DS as DashScope
    participant ACM as AIConfigMapper
    participant MM as MusicMapper
    participant RM as RecommendRecordMapper
    participant FM as FeedbackMapper
    participant URM as UploadRecordMapper
    participant DB as MySQL

    Note over AD,DB: AI 配置管理
    AD->>AACC: GET /api/admin/ai/config/current
    activate AACC
    AACC->>ACM: selectActive()
    ACM->>DB: SELECT * FROM ai_config WHERE is_active=1
    DB-->>ACM: AIConfig
    AACC-->>AD: Result{AIConfig}
    deactivate AACC

    AD->>AACC: POST /api/admin/ai/config/test
    activate AACC
    AACC->>DAI: testConnection(config)
    activate DAI
    DAI->>DS: ChatClient.call("Reply with OK only.")
    DS-->>DAI: "OK"
    DAI-->>AACC: void
    deactivate DAI
    AACC-->>AD: Result{success: true}
    deactivate AACC

    AD->>AACC: POST /api/admin/ai/config/save
    activate AACC
    alt 更新
        AACC->>ACM: deactivateOthers(id)
        ACM->>DB: UPDATE ai_config SET is_active=0 WHERE id!=?
        AACC->>ACM: update(config)
        ACM->>DB: UPDATE ai_config
    else 新增
        AACC->>ACM: insert(config)
        ACM->>DB: INSERT INTO ai_config
        AACC->>ACM: deactivateOthers(newId)
        ACM->>DB: UPDATE ai_config SET is_active=0 WHERE id!=?
    end
    AACC-->>AD: Result{null}
    deactivate AACC

    Note over AD,DB: AI 监控面板
    AD->>AAC: GET /api/admin/ai/status
    activate AAC
    AAC->>ACM: selectActive()
    ACM->>DB: SELECT
    AAC->>DAI: hasUsableRuntimeConfig() / getRuntimeConfig()
    AAC->>URM: countByStatus("ANALYZING")
    URM->>DB: SELECT COUNT(*)
    AAC-->>AD: Result{online, analyzingCount, provider, model, strategy}
    deactivate AAC

    AD->>AAC: GET /api/admin/ai/accuracy
    activate AAC
    AAC->>RM: selectAverageSimilarity()
    RM->>DB: SELECT AVG(similarity)
    AAC->>RM: countAll()
    RM->>DB: SELECT COUNT(*)
    AAC->>FM: count(null, null, "LIKE")
    FM->>DB: SELECT COUNT(*)
    AAC->>FM: count(null, null, "DISLIKE")
    FM->>DB: SELECT COUNT(*)
    AAC->>AAC: accuracy = LIKE/(LIKE+DISLIKE) × 100
    AAC-->>AD: Result{accuracy, avgSimilarity, totalRecommends, positiveFeedback, negativeFeedback}
    deactivate AAC

    AD->>AAC: GET /api/admin/ai/features/distribution
    activate AAC
    AAC->>MM: selectGenreDistribution(8)
    MM->>DB: SELECT genre as name, COUNT(*) as value FROM music GROUP BY genre ORDER BY value DESC LIMIT 8
    DB-->>MM: distribution
    AAC-->>AD: Result{distribution}
    deactivate AAC

    Note over AD,DB: 仪表盘统计
    AD->>ADC: GET /api/admin/dashboard/stats
    activate ADC
    ADC->>UM: count → totalUsers
    UM->>DB: SELECT COUNT(*) FROM user
    ADC->>MM: count → totalMusic
    MM->>DB: SELECT COUNT(*) FROM music
    ADC->>URM: countAll → totalUploads
    URM->>DB: SELECT COUNT(*) FROM upload_record
    ADC->>FM: count → totalFeedback
    FM->>DB: SELECT COUNT(*) FROM feedback
    ADC-->>AD: Result{totalUsers, activeUsers, totalMusic, totalUploads, totalFeedback}
    deactivate ADC
```

---

## 文件清单

| 文件 | 内容 |
|------|------|
| `E:\MUSIC\docs\class-diagrams.md` | 类图（5张 Mermaid） |
| `E:\MUSIC\docs\sequence-diagrams.md` | 时序图（6张 Mermaid） |
| `E:\MUSIC\docs\01-architecture.puml` | 架构图 PlantUML |
| `E:\MUSIC\docs\02-database-er.puml` | ER 图 PlantUML |
| `E:\MUSIC\docs\03-java-entities.puml` | 实体 PlantUML |
| `E:\MUSIC\docs\04-dto-vo.puml` | DTO/VO PlantUML |
| `E:\MUSIC\docs\05-python-audio.puml` | Python 模型 PlantUML |
