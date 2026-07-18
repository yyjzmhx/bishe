# 用户认证与资料维护 — 核心代码

---

## 1. Entity

```java
@Data
public class User {
    private Long id;
    private String username;
    private String phone;
    private String email;
    private String password;
    private String avatar;
    private String nickname;
    private String role;       // USER / ADMIN
    private Integer status;    // 1-正常 0-禁用
}
```

## 2. DTO

```java
@Data
public class LoginRequest {
    private String username;
    private String password;
}

@Data
public class RegisterRequest {
    private String username;
    private String password;
    private String phone;
    private String email;
    private String nickname;
}

@Data
public class LoginResponse {
    private String token;
    private Long userId;
    private String username;
    private String role;
    private String nickname;
    private String avatar;
}
```

## 3. Controller

```java
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @PostMapping("/login")
    public Result<LoginResponse> login(@RequestBody LoginRequest req) {
        User user = userService.login(req.getUsername(), req.getPassword());
        if (user == null || user.getStatus() == 0) {
            return Result.error("用户名或密码错误");
        }
        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());
        return Result.success(buildResponse(user, token));
    }

    @PostMapping("/register")
    public Result<String> register(@RequestBody RegisterRequest req) {
        User user = new User();
        BeanUtils.copyProperties(req, user);
        userService.register(user);
        return Result.success("注册成功", null);
    }
}

@RestController
@RequestMapping("/api/user")
public class UserController {

    @GetMapping("/profile")
    public Result<User> getProfile(@RequestAttribute Long userId) {
        User user = userService.getById(userId);
        user.setPassword(null);
        return Result.success(user);
    }

    @PutMapping("/profile")
    public Result<String> updateProfile(@RequestBody User user,
                                        @RequestAttribute Long userId) {
        user.setId(userId);
        user.setPassword(null);
        user.setRole(null);
        userService.update(user);
        return Result.success("更新成功", null);
    }
}
```

## 4. Service

```java
@Service
public class UserService {

    public User login(String username, String password) {
        User user = userMapper.selectByUsername(username);
        if (user == null || !bCryptUtil.matches(password, user.getPassword())) {
            return null;
        }
        return user;
    }

    public boolean register(User user) {
        if (userMapper.selectByUsername(user.getUsername()) != null) {
            throw new RuntimeException("用户名已存在");
        }
        user.setPassword(bCryptUtil.encode(user.getPassword()));
        user.setRole("USER");
        user.setStatus(1);
        return userMapper.insert(user) > 0;
    }

    public User getById(Long id) {
        return userMapper.selectById(id);
    }

    public boolean update(User user) {
        return userMapper.update(user) > 0;
    }
}
```

## 5. Mapper

```java
@Mapper
public interface UserMapper {
    User selectById(Long id);
    User selectByUsername(String username);
    int insert(User user);
    int update(User user);
}
```

## 6. Util

```java
@Component
public class JwtUtil {
    public String generateToken(Long userId, String username, String role) {
        Map<String, Object> claims = Map.of("userId", userId, "username", username, "role", role);
        return Jwts.builder().claims(claims)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 86400000))
                .signWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)))
                .compact();
    }
}

@Component
public class BCryptUtil {
    private static final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    public String encode(String raw) { return encoder.encode(raw); }
    public boolean matches(String raw, String encoded) { return encoder.matches(raw, encoded); }
}
```

## 7. Interceptor（精简）

```java
@Component
public class AuthInterceptor implements HandlerInterceptor {
    public boolean preHandle(HttpServletRequest req, HttpServletResponse res, Object h) {
        if (req.getRequestURI().startsWith("/api/auth/")) return true;
        String token = req.getHeader("Authorization").replace("Bearer ", "");
        if (!jwtUtil.validateToken(token)) return false;
        req.setAttribute("userId", jwtUtil.getUserIdFromToken(token));
        return true;
    }
}
```

---

## 调用链

```
登录: AuthController.login() → UserService.login()
      → UserMapper.selectByUsername() → BCryptUtil.matches()
      → JwtUtil.generateToken()

注册: AuthController.register() → UserService.register()
      → UserMapper.selectByUsername() (查重)
      → BCryptUtil.encode() → UserMapper.insert()

获取: UserController.getProfile() → UserService.getById()
      → UserMapper.selectById()

更新: UserController.updateProfile() → UserService.update()
      → UserMapper.update()

拦截: AuthInterceptor → 从Header取Token → JwtUtil验证 → 注入userId到Request
```
