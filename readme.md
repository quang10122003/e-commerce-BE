# Shop

Shop là dự án backend Spring Boot cho hệ thống ecommerce.

## Yêu cầu dự án

- Java 21
- Maven 3.9+
- MySQL 8
- RabbitMQ
- Redis
- Docker và Docker Compose nếu muốn chạy bằng container
- File `.env` để chứa biến môi trường

## Cấu hình cần có

Sao chép file mẫu và cập nhật giá trị thật:

```bash
# Windows
copy .env.example .env

# macOS/Linux
cp .env.example .env
```

Các biến quan trọng cần khai báo trong `.env`:

- `JWT_SECRET`
- `CLOUDINARY_NAME`, `CLOUDINARY_API_KEY`, `CLOUDINARY_API_SECRET`
- `RESEND_KEY`
- `SECRET_KEY_CLOUDFLARE_TURNSTILE`
- `SECRET_KEY_WEBHOOK_SEPAY`
- `USERNAME_DATABASE`, `PASSWORD_DATABASE`
- `USER_MQ`, `PASSWORD_MQ`
- `CLIENT_ID`, `CLIENT_SECRET`

Ngoài ra dự án dùng database `ecommerce_db`, vì vậy bạn cần tạo database này trước và import file `db.sql`.

## 1. Chạy code trực tiếp

### Bước 1: Khởi động các dịch vụ phụ thuộc

Ứng dụng cần MySQL, RabbitMQ và Redis đang chạy ở máy local.

Nếu bạn dùng Docker cho các dịch vụ này thì chỉ cần đảm bảo chúng đang listen ở:

- MySQL: `localhost:3306`
- RabbitMQ: `localhost:5672`
- Redis: `localhost:6379`

### Bước 2: Import database

Chạy file `db.sql` vào MySQL để tạo dữ liệu ban đầu và cấu trúc cần thiết.

### Bước 3: Cấu hình `.env`

Điền đầy đủ thông tin trong `.env` để ứng dụng đọc được biến môi trường khi khởi động.

### Bước 4: Chạy ứng dụng

Chạy bằng Maven Wrapper:

```bash
# macOS/Linux
./mvnw spring-boot:run

# Windows
mvnw.cmd spring-boot:run
```

Hoặc build rồi chạy jar:

```bash
# macOS/Linux
./mvnw clean package

# Windows
mvnw.cmd clean package

java -jar target/shop-0.0.1-SNAPSHOT.jar
```

Ứng dụng mặc định chạy ở port `8080`.

## 2. Chạy bằng Docker

### Bước 1: Build ứng dụng

File `Dockerfile` hiện tại copy file jar từ thư mục `target`, nên trước hết cần build project:

```bash
# macOS/Linux
./mvnw clean package

# Windows
mvnw.cmd clean package
```

### Bước 2: Build image Docker

```bash
docker build -t shop:latest .
```

### Bước 3: Chạy container

```bash
docker run --rm -p 8080:8080 --env-file .env shop:latest
```

Khi chạy bằng Docker, ứng dụng dùng profile `docker` và kết nối tới các dịch vụ bên ngoài thông qua `host.docker.internal` theo cấu hình trong `application-docker.yaml`.

Vì vậy MySQL, RabbitMQ và Redis phải được chạy sẵn ở máy host trước khi khởi động container.

## Cấu hình ứng dụng

- File cấu hình chung: `src/main/resources/application.yaml`
- File chạy local: `src/main/resources/application-local.yaml`
- File chạy Docker: `src/main/resources/application-docker.yaml`

## Ghi chú

- Ứng dụng đọc biến trong file `.env` khi khởi động.
- Nếu thay đổi cấu hình database hoặc cổng dịch vụ, hãy cập nhật lại cả `.env` và file cấu hình tương ứng.
- Nếu muốn chạy frontend riêng, biến `DOMAIN` trong `.env` dùng làm URL của frontend, mặc định là `http://localhost:3000`.
