# 个性化音乐推荐系统 — 核心代码

---

## 一、用户认证与资料维护

```java
// UserService
public User login(String username, String password) {
    User user = userMapper.selectByUsername(username);
    if (user == null || !bCryptUtil.matches(password, user.getPassword())) return null;
    return user;
}

public boolean register(User user) {
    if (userMapper.selectByUsername(user.getUsername()) != null)
        throw new RuntimeException("用户名已存在");
    user.setPassword(bCryptUtil.encode(user.getPassword()));
    user.setRole("USER");
    user.setStatus(1);
    return userMapper.insert(user) > 0;
}
```

```java
// AuthController
@PostMapping("/login")
public Result<LoginResponse> login(@RequestBody LoginRequest req) {
    User user = userService.login(req.getUsername(), req.getPassword());
    if (user == null || user.getStatus() == 0) return Result.error("用户名或密码错误");
    String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());
    return Result.success(buildResponse(user, token));
}

@PostMapping("/register")
public Result<String> register(@RequestBody RegisterRequest req) {
    User user = new User(); BeanUtils.copyProperties(req, user);
    userService.register(user);
    return Result.success("注册成功", null);
}
```

---

## 二、音频AI推荐

```java
// UploadController
@PostMapping("/upload")
public Result<Map<String, Object>> upload(@RequestParam MultipartFile file,
                                          @RequestAttribute Long userId) {
    UploadRecord record = uploadService.saveUploadRecord(userId, file);
    return Result.success(Map.of("uploadId", record.getId(), "fileUrl", record.getFileUrl()));
}
```

```java
// RecommendService（核心）
@Transactional
public void analyzeAndRecommend(Long uploadId) {
    String features = audioAnalysisService.extractFeatures(fileUrl);
    List<Music> musicList = musicMapper.selectAll();
    List<RecommendRecord> records = new ArrayList<>();
    for (Music m : musicList) {
        double sim = audioAnalysisService.calculateSimilarity(features, m.getFeatures());
        if (sim >= 0.35) {
            RecommendRecord r = new RecommendRecord();
            r.setUploadId(uploadId); r.setMusicId(m.getId());
            r.setSimilarity(BigDecimal.valueOf(sim));
            records.add(r);
        }
    }
    records.sort((a, b) -> b.getSimilarity().compareTo(a.getSimilarity()));
    records = records.stream().limit(20).toList();
    recommendRecordMapper.deleteByUploadId(uploadId);
    recommendRecordMapper.insertBatch(records);
}
```

```java
// AudioAnalysisService（相似度计算）
public double calculateSimilarity(String f1, String f2) {
    ParsedFeatures p1 = parseFeatures(f1), p2 = parseFeatures(f2);
    return cosineSimilarity(p1.vector(), p2.vector()) * 0.60
         + textSimilarity(p1.searchText(), p2.searchText()) * 0.16
         + labelSimilarity(p1, p2) * 0.10
         + durationSimilarity(p1.duration(), p2.duration()) * 0.07
         + metadataSimilarity(p1, p2) * 0.07;
}
```

---

## 三、音乐浏览

```java
// MusicService
public Map<String, Object> getMusicList(String keyword, int pageNum, int pageSize) {
    int offset = (pageNum - 1) * pageSize;
    List<Music> list = musicMapper.selectList(keyword, null, null, 1, offset, pageSize);
    long total = musicMapper.count(keyword, null, null, 1);
    return Map.of("list", list, "total", total);
}
```

```java
// PlaylistService（Redis缓存）
public void savePlaylist(Long userId, List<Music> playlist) {
    redisTemplate.opsForValue().set("playlist:" + userId, playlist, 30, TimeUnit.DAYS);
}

public List<Music> getPlaylist(Long userId) {
    Object val = redisTemplate.opsForValue().get("playlist:" + userId);
    return val instanceof List<?> list ? list.stream()
        .map(i -> JSON.parseObject(JSON.toJSONString(i), Music.class)).toList() : List.of();
}

public void savePlayState(Long userId, Long musicId, Integer idx,
        String mode, Double time, Boolean playing) {
    redisTemplate.opsForValue().set("state:" + userId,
        new PlayState(musicId, idx, mode, time, playing), 30, TimeUnit.DAYS);
}
```

---

## 四、社区互动

```java
// FeedbackService
@Transactional
public boolean toggleCommentLike(Long commentId, Long userId) {
    Feedback comment = feedbackMapper.selectById(commentId);
    if (comment == null || comment.getUserId().equals(userId)) return false;
    return feedbackMapper.checkCommentLike(userId, commentId) > 0
        ? feedbackMapper.deleteCommentLike(commentId, userId) > 0
        : feedbackMapper.insertCommentLike(commentId, userId) > 0;
}

@Transactional
public boolean toggleFavorite(Long musicId, Long userId) {
    Feedback fav = feedbackMapper.selectByUserAndMusicAndType(userId, musicId, "FAVORITE");
    if (fav != null) { feedbackMapper.deleteById(fav.getId()); return false; }
    Feedback f = new Feedback(); f.setUserId(userId); f.setMusicId(musicId); f.setType("FAVORITE");
    return feedbackMapper.insert(f) > 0;
}
```

```java
// FeedbackController
@PostMapping("/comment/{commentId}/like")
public Result<Map<String, Object>> toggleLike(@PathVariable Long commentId,
                                              @RequestAttribute Long userId) {
    feedbackService.toggleCommentLike(commentId, userId);
    return Result.success(Map.of("isLiked", feedbackService.checkCommentLike(commentId, userId)));
}

@PostMapping("/music/{musicId}/favorite")
public Result<Map<String, Object>> toggleFavorite(@PathVariable Long musicId,
                                                  @RequestAttribute Long userId) {
    return Result.success(Map.of("isFavorite", feedbackService.toggleFavorite(musicId, userId)));
}
```

---

## 五、内容资源管理

```java
// AdminMusicController
@PostMapping
public Result<Map<String, Object>> addMusic(@RequestParam String title,
        @RequestParam String artist, @RequestParam MultipartFile audioFile,
        @RequestParam(required = false) MultipartFile coverFile) {
    Map<String, String> audio = fileService.saveLibraryFile(audioFile, null);
    Music m = new Music(); m.setTitle(title); m.setArtist(artist);
    m.setFileUrl(audio.get("fileUrl")); m.setStatus(1);
    if (coverFile != null) m.setCoverUrl(fileService.saveCoverImage(coverFile, null).get("fileUrl"));
    musicService.addMusic(m);
    musicService.refreshAudioFeatures(m.getId());
    return Result.success(Map.of("musicId", m.getId(), "fileUrl", m.getFileUrl()));
}

@DeleteMapping("/{id}")
public Result<String> deleteMusic(@PathVariable Long id) {
    return musicService.deleteMusic(id) ? Result.success("删除成功", null) : Result.error("删除失败");
}
```

```java
// AdminNoticeController
@PostMapping
public Result<Map<String, Object>> addNotice(@RequestBody Notice notice) {
    if (notice.getStatus() == 1) notice.setPublishTime(LocalDateTime.now());
    noticeMapper.insert(notice);
    return Result.success(Map.of("noticeId", notice.getId()));
}

@PutMapping("/{id}/publish")
public Result<String> publish(@PathVariable Long id) {
    Notice n = new Notice(); n.setId(id); n.setStatus(1); n.setPublishTime(LocalDateTime.now());
    return noticeMapper.update(n) > 0 ? Result.success("发布成功", null) : Result.error("失败");
}
```

```java
// FileService
public Map<String, String> saveLibraryFile(MultipartFile file, Long musicId) {
    String url = storageService.saveFile(file, "library", "music_" + musicId + "_" + UUID.randomUUID() + ext(file));
    return Map.of("fileUrl", url);
}

public boolean deleteFile(String fileUrl) {
    return storageService.deleteFile(fileUrl);
}
```

---

## 六、用户社区与AI管理

```java
// AdminAIController
@GetMapping("/status")
public Result<Map<String, Object>> getAIStatus() {
    AIConfig cfg = dynamicAIChatService.hasUsableRuntimeConfig()
        ? dynamicAIChatService.getRuntimeConfig() : null;
    return Result.success(Map.of(
        "online", cfg != null,
        "analyzingCount", uploadRecordMapper.countByStatus("ANALYZING"),
        "provider", cfg != null ? cfg.getProvider() : null,
        "model", cfg != null ? cfg.getModel() : null));
}

@GetMapping("/accuracy")
public Result<Map<String, Object>> getAccuracy() {
    long likes = feedbackMapper.count(null, null, "LIKE");
    long dislikes = feedbackMapper.count(null, null, "DISLIKE");
    double accuracy = (likes + dislikes) > 0 ? likes * 100.0 / (likes + dislikes) : 0;
    return Result.success(Map.of("accuracy", accuracy, "likes", likes, "dislikes", dislikes));
}
```

```java
// AdminDashboardController
@GetMapping("/stats")
public Result<Map<String, Object>> getStats() {
    return Result.success(Map.of(
        "totalUsers", userMapper.count(null, null, null),
        "totalMusic", musicMapper.count(null, null, null, null),
        "totalUploads", uploadRecordMapper.countAll(),
        "totalFeedback", feedbackMapper.count(null, null, null)));
}
```

```java
// AdminAIConfigController
@PostMapping("/test")
public Result<Map<String, Object>> testConnection(@RequestBody AIConfig config) {
    try {
        dynamicAIChatService.testConnection(config);
        return Result.success(Map.of("success", true));
    } catch (Exception e) {
        return Result.success(Map.of("success", false, "message", e.getMessage()));
    }
}

@PostMapping("/save")
public Result<Void> saveConfig(@RequestBody AIConfig config) {
    if (config.getId() != null) {
        if (config.getIsActive()) aiConfigMapper.deactivateOthers(config.getId());
        aiConfigMapper.update(config);
    } else {
        aiConfigMapper.insert(config);
        if (config.getIsActive()) aiConfigMapper.deactivateOthers(config.getId());
    }
    return Result.success(null);
}
```
