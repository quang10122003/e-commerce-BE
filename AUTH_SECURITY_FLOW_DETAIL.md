# Login va xac thuc API trong project

Tai lieu nay mo ta luong login, JWT filter, va cac case loi 401/403 trong project.

## 1. Cac class chinh

| Class | Vai tro |
| --- | --- |
| `AuthController` | Nhan request login/signup/refresh/validate/me tu client. |
| `AuthService` | Xu ly logic login, signup, tao token, refresh token, lay current user. |
| `AuthenticationManager` | Cong xac thuc cua Spring Security. `AuthService.login()` goi vao day. |
| `DaoAuthenticationProvider` | Provider dung DB user + password encoder de xac thuc email/password. |
| `UserDetailsServiceCustom` | Tim user trong DB theo email va chuyen thanh `UserDetails`. |
| `PasswordEncoder` | Dung BCrypt de so sanh password nguoi dung nhap voi password hash trong DB. |
| `AuthUtil` | Tao JWT, doc JWT, verify chu ky JWT, kiem tra token het han va token type. |
| `JwtAuthFilter` | Chay truoc controller cho cac API can token. Neu token hop le thi set Authentication vao `SecurityContextHolder`. |
| `SecurityConfig` | Cau hinh endpoint nao public, endpoint nao can login, filter nao duoc gan vao chain. |
| `RestAuthenticationEntryPoint` | Tra response JSON khi request chua dang nhap/token sai/token thieu. |
| `GlobalExceptionHandler` | Bat cac exception nhu `BadCredentialsException`, `LockedException`, `AccessDeniedException`. |
| `ApiErrorException` | Bat `ApiError` do code service tu nem ra. |
## so sanh mk khi login 
Dòng này:

java



provider.setPasswordEncoder(passwordEncoder);



nói cho DaoAuthenticationProvider biết: khi login, hãy dùng passwordEncoder này để kiểm tra mật khẩu.

Luồng login sẽ đi như này:

text



Client gửi email/password
        ↓
AuthController.login()
        ↓
AuthService.login()
        ↓
authenticationManager.authenticate(...)
        ↓
DaoAuthenticationProvider
        ↓
UserDetailsServiceCustom.loadUserByUsername(email)
        ↓
Lấy user trong DB
        ↓
So sánh password nhập vào với password_hash trong DB
        ↓
Nếu đúng → login thành công
Nếu sai → BadCredentialsException



Cụ thể trong AuthService.login() có đoạn kiểu:

java



authenticationManager.authenticate(
    new UsernamePasswordAuthenticationToken(email, password)
);



Lúc này Spring Security nhận được:

text



username/email = email người dùng nhập
raw password = password người dùng nhập



Sau đó DaoAuthenticationProvider gọi UserDetailsServiceCustom.java để lấy user:

java



loadUserByUsername(email)



Trong user trả về có password đã hash từ DB:

java



.password(user.getPassword())



Rồi provider dùng encoder bạn set:

java



provider.setPasswordEncoder(passwordEncoder);



để kiểm tra:

java



passwordEncoder.matches(rawPassword, encodedPasswordFromDatabase)



Ví dụ:

text



rawPassword: 123456
encodedPasswordFromDatabase: $2a$10$....



Nếu matches(...) trả về true, Spring coi là đăng nhập hợp lệ. Sau đó code của bạn tiếp tục tạo JWT token trong AuthService.

Tóm lại, dòng đó không tự encode password mới. Nó chỉ cấu hình cho Spring Security biết dùng BCrypt để so sánh mật khẩu khi login.

## 2. Luong login thanh cong

Endpoint:

```http
POST /api/auth/login
```

Flow:

```text
Client
  |
  | POST /api/auth/login { email, password }
  v
AuthController.login()
  |
  v
AuthService.login()
  |
  | normalize email/password
  v
authenticationManager.authenticate(
    new UsernamePasswordAuthenticationToken(email, password)
)
  |
  v
DaoAuthenticationProvider
  |
  | goi loadUserByUsername(email)
  v
UserDetailsServiceCustom
  |
  | userRepo.findByEmailIgnoreCase(email)
  | tao UserDetails gom:
  | - username = email
  | - password = password_hash trong DB
  | - authorities = role/quyen
  | - accountLocked = user.isLocked()
  v
DaoAuthenticationProvider
  |
  | passwordEncoder.matches(rawPassword, passwordHashFromDb)
  v
Neu dung password va account khong bi khoa
  |
  v
Tra ve Authentication
  |
  v
AuthService.login()
  |
  | UserDetails userDetails = authentication.getPrincipal()
  | userRepo.findByEmailIgnoreCase(userDetails.getUsername())
  v
Lay User entity day du tu DB
  |
  | authUtil.generateAccessToken(user)
  | authUtil.generateRefreshToken(user)
  v
Tra ApiResponse<AuthResponse> ve client
```

Ly do lay lai `User entity` sau khi login thanh cong:

`UserDetails` cua Spring chu yeu phuc vu bao mat. App can `User entity` de lay `id`, `role`, `fullName` dua vao JWT va response.

## 3. Login sai email hoac password

Flow:

```text
AuthService.login()
  |
  v
authenticationManager.authenticate(...)
  |
  v
DaoAuthenticationProvider
  |
  | khong tim thay user hoac passwordEncoder.matches(...) = false
  v
Nem BadCredentialsException
  |
  v
Ham AuthService.login() dung ngay tai dong authenticate(...)
  |
  v
GlobalExceptionHandler.handleBadCredentials()
  |
  v
Tra ErrorCode.INVALID_CREDENTIALS
```

Response:

```http
HTTP 401 Unauthorized
```

Error code:

```java
INVALID_CREDENTIALS("Email hoac mat khau khong dung", HttpStatus.UNAUTHORIZED)
```

Quan trong: khi `authenticate(...)` nem exception, cac dong tao JWT phia sau khong chay.

## 4. Login khi tai khoan bi khoa

Flow:

```text
AuthService.login()
  |
  v
authenticationManager.authenticate(...)
  |
  v
UserDetailsServiceCustom tao UserDetails voi accountLocked(user.isLocked())
  |
  v
DaoAuthenticationProvider thay account locked
  |
  v
Nem LockedException
  |
  v
GlobalExceptionHandler.handleLockedException()
  |
  v
Tra ErrorCode.ACCOUNT_LOCKED
```

Response:

```http
HTTP 401 Unauthorized
```

## 5. Luong goi API can dang nhap bang JWT

Vi du:

```http
GET /api/orders
Authorization: Bearer <access-token>
```

Flow:

```text
Client
  |
  | Goi API kem Authorization: Bearer <token>
  v
JwtAuthFilter.doFilterInternal()
  |
  | doc header Authorization
  | cat chuoi "Bearer "
  v
AuthUtil.extractEmail(token)
  |
  | verify chu ky token bang app.jwt.secret
  | doc subject = email
  v
UserDetailsServiceCustom.loadUserByUsername(email)
  |
  | lay user tu DB
  | tao UserDetails
  v
JwtAuthFilter
  |
  | neu user bi khoa -> goi RestAuthenticationEntryPoint va dung request
  | neu token hop le:
  |   authUtil.isAccessTokenValid(token, user)
  v
SecurityContextHolder.getContext().setAuthentication(auth)
  |
  v
Spring Security authorize request
  |
  v
Controller cua API duoc goi
```

Sau khi filter set `Authentication`, cac service co the lay user hien tai qua:

```java
SecurityContextHolder.getContext().getAuthentication()
```

## 6. API khong co token

Vi du goi API can dang nhap nhung khong gui header:

```http
GET /api/orders
```

Flow:

```text
JwtAuthFilter
  |
  | header Authorization null
  v
filterChain.doFilter(...)
  |
  v
Spring Security thay endpoint nay can authenticated
  |
  v
RestAuthenticationEntryPoint.commence()
  |
  v
Tra ErrorCode.UNAUTHORIZED
```

Response:

```http
HTTP 401 Unauthorized
```

## 7. API co token sai, het han, hoac refresh token thay vi access token

Flow:

```text
JwtAuthFilter
  |
  | co Authorization: Bearer <token>
  v
AuthUtil.extractEmail(token) hoac isAccessTokenValid(token, user)
  |
  | token sai chu ky / het han / tokenType khong phai "access"
  v
catch Exception
  |
  | SecurityContextHolder.clearContext()
  v
filterChain.doFilter(...)
  |
  v
Spring Security thay request van chua authenticated
  |
  v
RestAuthenticationEntryPoint.commence()
  |
  v
Tra ErrorCode.UNAUTHORIZED
```

Response:

```http
HTTP 401 Unauthorized
```

## 8. API co token hop le nhung khong du quyen

Flow:

```text
JwtAuthFilter
  |
  | token hop le
  v
Set Authentication vao SecurityContextHolder
  |
  v
Spring Security / @PreAuthorize / rule phan quyen
  |
  | user da dang nhap nhung khong co role/quyen can thiet
  v
AccessDeniedException
  |
  v
AccessDeniedHandler trong SecurityConfig hoac GlobalExceptionHandler
  |
  v
Tra ErrorCode.ACCESS_DENIED
```

Response:

```http
HTTP 403 Forbidden
```

Khac nhau giua 401 va 403:

```text
401 = chua xac thuc duoc
    - khong co token
    - token sai
    - token het han
    - login sai email/password

403 = da xac thuc roi nhung khong co quyen
    - token hop le
    - user da dang nhap
    - nhung role/quyen khong du de vao API
```

## 9. Public endpoints

Trong `SecurityConfig`, cac endpoint public hien tai nam trong `PUBLIC_ENDPOINTS`.

Nhung endpoint nay khong yeu cau authenticated:

```text
/api/auth/login
/api/auth/signup
/api/auth/refresh-token
/api/auth/validate-token
/api/cloudinary/upload-images
/api/products/**
/api/categories
```

Ngoai ra method `OPTIONS /**` cung duoc permit de ho tro CORS preflight.

Luu y: `JwtAuthFilter.shouldNotFilter()` hien dang skip rieng cac auth endpoint:

```text
/api/auth/login
/api/auth/signup
/api/auth/refresh-token
/api/auth/validate-token
```

Nhung endpoint public khac van co the di qua filter. Neu khong gui token thi filter cho di tiep binh thuong.

## 10. Refresh token

Endpoint:

```http
GET /api/auth/refresh-token
Authorization: Bearer <refresh-token>
```

Flow:

```text
AuthController.refreshToken()
  |
  v
AuthService.refreshAccessToken()
  |
  | extractBearerToken(authorizationHeader)
  | authUtil.extractEmail(refreshToken)
  | userRepo.findByEmailIgnoreCase(email)
  | assertUserNotLocked(user)
  | authUtil.isRefreshTokenValid(refreshToken, user)
  v
Neu refresh token hop le
  |
  | authUtil.generateAccessToken(user)
  v
Tra access token moi
```

Neu refresh token sai/het han/khong phai refresh token:

```text
Tra ErrorCode.REFRESH_TOKEN_INVALID
HTTP 401 Unauthorized
```

## 11. Token duoc tao nhu the nao

Trong `AuthUtil.generateToken()`:

```text
subject = user.email
claims:
  userId
  role
  fullName
  tokenType = access hoac refresh
issuedAt = thoi diem tao
expiration = thoi diem het han
signWith(app.jwt.secret)
```

`app.jwt.secret` duoc lay tu `application.yaml`, va trong YAML gia tri nay tro toi env:

```yaml
app:
  jwt:
    secret: ${JWT_SECRET}
```

## 12. Tom tat nhanh cac case

| Case | Noi nem/bat loi | ErrorCode | HTTP |
| --- | --- | --- | --- |
| Login sai email/password | `DaoAuthenticationProvider` nem `BadCredentialsException`, `GlobalExceptionHandler` bat | `INVALID_CREDENTIALS` | 401 |
| Login user bi khoa | Spring Security nem `LockedException`, `GlobalExceptionHandler` bat | `ACCOUNT_LOCKED` | 401 |
| Goi API can login khong co token | `RestAuthenticationEntryPoint` | `UNAUTHORIZED` | 401 |
| Goi API can login token sai/het han | `JwtAuthFilter` clear context, sau do `RestAuthenticationEntryPoint` | `UNAUTHORIZED` | 401 |
| Token hop le nhung user bi khoa | `JwtAuthFilter` goi `RestAuthenticationEntryPoint` voi `LockedException` | `ACCOUNT_LOCKED` | 401 |
| Da login nhung khong du quyen | Access denied handler trong `SecurityConfig` | `ACCESS_DENIED` | 403 |
| Service tu nem loi nghiep vu | `ApiErrorException` bat `ApiError` | tuy `ErrorCode` | tuy `ErrorCode` |

