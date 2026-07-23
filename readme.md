# Shop

Shop là dự án backend Spring Boot cho hệ thống ecommerce.

## Yêu cầu dự án
- ngrok
- Java 21
- Maven 3.9+
- MySQL 8
- RabbitMQ
- Redis
- Docker và Docker Compose nếu muốn chạy bằng container
- File `.env` để chứa biến môi trường

## Cấu hình cần có

Sao chép file mẫu(.env.example) và cập nhật giá trị thật:

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
- `AUTH_TOKEN`

hãy tạo tài khoản ở các dịch vụ cần trong biến .env để có thể lấy đc các key để cấu hình .env

## 1. Chạy code trực tiếp

### Bước 1: Khởi động các dịch vụ phụ thuộc

Ứng dụng cần MySQL, ngrok, RabbitMQ và Redis đang chạy ở máy local.

Nếu bạn dùng Docker cho các dịch vụ này thì chỉ cần đảm bảo chúng đang listen ở:

- MySQL: `localhost:3306`
- RabbitMQ: `localhost:5672`
- Redis: `localhost:6379`
- ngrok: `localhost:4040`

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

### Bước 5: Chạy ngrok để lấy URL public

Mở terminal và chạy:

```bash
ngrok http 8080
```

Sau đó truy cập `http://localhost:4040` để lấy URL public vừa được tạo.

### Bước 6: Cấu hình webhook Sepay để test thanh toán

1. Truy cập [https://my.sepay.vn/testmode/dashboard](https://my.sepay.vn/testmode/dashboard)
2. Vào mục **Tích hợp webhook** → **Thêm webhook**
3. Điền các thông tin:
   - **URL nhận webhook**: `[URL lấy ở bước 5]/api/payments/sepay/webhook`
   - **Định dạng dữ liệu**: `Json`
   - **Loại giao dịch**: `Tất cả`
4. Bật 2 tùy chọn:
   - **Dùng để xác thực thanh toán**
   - **Chỉ gửi khi có mã thanh toán**
5. Cấu hình mã thanh toán:
   - **Tiền tố**: `DH`
   - **Hậu tố**: từ 3 đến 30 ký tự, gồm số và chữ
6. Mục **Bảo mật** → **API key**: điền giá trị giống với `SECRET_KEY_WEBHOOK_SEPAY` trong `.env`

## 2. Chạy bằng Docker

### Bước 1: Khởi động ứng dụng bằng Docker Compose

Đảm bảo đã cấu hình đầy đủ file `.env` (xem phần [Cấu hình cần có](#cấu-hình-cần-có)), sau đó chạy:

```bash
docker compose up 
```

Lệnh này sẽ tự động build và khởi động ứng dụng cùng các dịch vụ cần thiết theo cấu hình trong `docker-compose.yml`.

> Nếu đã thay đổi code và cần build lại image, chạy `docker compose up --build`.

Khi chạy bằng Docker Compose, ứng dụng dùng profile `docker`. Các dịch vụ phụ thuộc (MySQL, RabbitMQ, Redis, ngrok) đã được định nghĩa trong `docker-compose.yml` và sẽ tự động khởi động cùng ứng dụng; với dịch vụ nào không được khai báo trong `docker-compose.yml`, ứng dụng sẽ kết nối tới máy host thông qua `host.docker.internal` theo cấu hình trong `application-docker.yaml`.

### Bước 2: Cấu hình webhook Sepay để test thanh toán

1. Truy cập `http://localhost:4040` để lấy URL public của ngrok.
2. Truy cập [https://my.sepay.vn/testmode/dashboard](https://my.sepay.vn/testmode/dashboard)
3. Vào mục **Tích hợp webhook** → **Thêm webhook**
4. Điền các thông tin:
   - **URL nhận webhook**: `[URL ngrok lấy ở bước trên]/api/payments/sepay/webhook`
   - **Định dạng dữ liệu**: `Json`
   - **Loại giao dịch**: `Tất cả`
5. Bật 2 tùy chọn:
   - **Dùng để xác thực thanh toán**
   - **Chỉ gửi khi có mã thanh toán**
6. Cấu hình mã thanh toán:
   - **Tiền tố**: `DH`
   - **Hậu tố**: từ 3 đến 30 ký tự, gồm số và chữ
7. Mục **Bảo mật** → **API key**: điền giá trị giống với `SECRET_KEY_WEBHOOK_SEPAY` trong `.env`

## Cấu hình ứng dụng

- File cấu hình chung: `src/main/resources/application.yaml`
- File chạy local: `src/main/resources/application-local.yaml`
- File chạy Docker: `src/main/resources/application-docker.yaml`

## Ghi chú

- Ứng dụng đọc biến trong file `.env` khi khởi động.
- Nếu thay đổi cấu hình database hoặc cổng dịch vụ, hãy cập nhật lại cả `.env` và file cấu hình tương ứng.
- Nếu muốn chạy frontend riêng, biến `DOMAIN` trong `.env` dùng làm URL của frontend, mặc định là `http://localhost:3000`.