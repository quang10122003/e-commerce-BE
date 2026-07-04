# E-commerce Frontend System Design (Production-Ready)

## 1) Scope va Muc Tieu

Tai lieu nay thiet ke he thong **frontend** cho e-commerce fullstack voi:

- Next.js (App Router), SEO-friendly, SSR/ISR
- Redux Toolkit + RTK Query cho state va data fetching
- JWT auth + RBAC (USER/ADMIN)
- Tich hop checkout/payment, realtime chat, analytics dashboard
- Scale muc tieu >= 10k users

Muc tieu la tao mot frontend architecture de trien khai duoc trong production, de maintain, de mo rong.

## 2) Lua Chon Kien Truc Tong The

### Quyet Dinh

- **Backend recommendation:** Modular Monolith (NestJS) trong giai doan 1.
- **Frontend:** Mot Next.js app duy nhat, tach domain theo module.

### Trade-off Monolith vs Microservices

- **Modular Monolith (chon):**
  - Uu diem: nhanh ship, don gian deploy, de debug, transaction dong bo cho cart/order/payment.
  - Nhuoc diem: ve sau can tactical scaling theo module.
- **Microservices:**
  - Uu diem: scale doc lap tung domain.
  - Nhuoc diem: do phuc tap cao som (distributed tracing, saga, eventual consistency, ops cost).

**Ly do chon:** voi 10k users, modular monolith + Redis + CDN + indexing da du de dat SLO. Sau nay co the tach `chat` va `analytics` thanh service rieng neu traffic tang manh.

## 3) Kien Truc Frontend (Next.js App Router)

## 3.1 Frontend Architecture Diagram (ASCII)

```text
                        +-----------------------------+
                        |          End Users          |
                        +--------------+--------------+
                                       |
                               HTTPS / WSS
                                       |
               +-----------------------v------------------------+
               |               Next.js Frontend                 |
               |  App Router + Server Components + Middleware  |
               |------------------------------------------------|
               |  UI Layer                                      |
               |  - Public pages (/ , /products/[id], /cart...)|
               |  - Admin pages (/admin/*)                      |
               |------------------------------------------------|
               |  State Layer                                   |
               |  - Redux Toolkit (auth/ui)                     |
               |  - RTK Query (api cache, invalidation)         |
               |------------------------------------------------|
               |  Infra Layer                                   |
               |  - Axios/fetch wrapper                         |
               |  - Socket client (chat)                        |
               |  - Error boundary + logging                    |
               +-----------------------+------------------------+
                                       |
                              REST API / WebSocket
                                       |
                    +------------------v-------------------+
                    |          Backend (NestJS)            |
                    | Auth/User/Product/Cart/Order/...     |
                    +--------+----------------+-------------+
                             |                |
                   +---------v-----+   +------v------+
                   | PostgreSQL     |   | Redis Cache |
                   +---------------+   +-------------+
                             |
                       +-----v------+
                       | Object/CDN |
                       |  (images)  |
                       +------------+
```

## 3.2 Frontend Folder Structure

```text
src/
  app/
    (public)/
      page.tsx
      products/
        [id]/page.tsx
      cart/page.tsx
      checkout/page.tsx
      orders/page.tsx
      orders/[id]/page.tsx
      chat/[productId]/page.tsx
    (admin)/
      admin/
        layout.tsx
        page.tsx
        products/page.tsx
        products/new/page.tsx
        products/[id]/edit/page.tsx
        users/page.tsx
        orders/page.tsx
        analytics/page.tsx
        chat/page.tsx
    api/
      auth/refresh/route.ts
  components/
    common/
    product/
    cart/
    checkout/
    order/
    chat/
    admin/
  lib/
    api/baseApi.ts
    api/endpoints/*.ts
    auth/token.ts
    socket/client.ts
    utils/*.ts
  store/
    index.ts
    slices/authSlice.ts
    slices/uiSlice.ts
  middleware.ts
  types/
    api.ts
    domain.ts
```

### Quy tac module

- Domain-first: `product`, `cart`, `order`, `chat`, `admin`.
- Moi module co:
  - `components`
  - `hooks`
  - `api endpoints` (RTK Query injectEndpoints)
  - `types`

## 4) Routing va Rendering Strategy

## 4.1 Routing

- `/` : product listing (SEO page)
- `/products/[id]` : product detail (SEO page)
- `/cart` : gio hang (auth)
- `/checkout` : checkout (auth)
- `/orders`, `/orders/[id]` : lich su va chi tiet don (auth)
- `/chat/[productId]` : chat user-admin theo san pham (auth)
- `/admin/*` : khu vuc admin (RBAC ADMIN)

## 4.2 SSR / ISR / CSR Phan Tach

- **SSR**:
  - Product list/search pages can SEO theo query quan trong.
  - Product detail.
- **ISR**:
  - Homepage category blocks, featured products (`revalidate: 60-300s`).
- **CSR**:
  - Cart live update.
  - Checkout form.
  - Chat realtime.
  - Admin dashboard table interactions.

### Trade-off

- SSR tang TTFB mot chut nhung duoc SEO va crawlability.
- ISR giam load backend cho page it thay doi.
- CSR cho UX interactive nhanh hon trong khu auth/admin.

## 5) State Management Design (Redux Toolkit + RTK Query)

## 5.1 Store Boundary

- Global state (Redux slices):
  - `auth`: user profile, role, session flags
  - `ui`: theme, modal, toast, filters tam thoi
- Server state (RTK Query):
  - products, cart, orders, users, analytics, chat history

## 5.2 RTK Query Cache Policy

- `keepUnusedDataFor`: 60s-300s tuy endpoint.
- `providesTags/invalidatesTags`:
  - Product CRUD invalidates `Product`, `ProductList`.
  - Cart update invalidates `Cart`.
  - Order create invalidates `OrderList`, `Cart`.
- Polling cho admin order dashboard: 15-30s.
- Re-fetch on focus/reconnect cho du lieu quan trong.

## 5.3 Error Handling

- Base query normalize response theo standard.
- Global interceptor:
  - 401 -> refresh token flow.
  - refresh fail -> logout + redirect `/login`.
- Error boundaries cho segment admin/public.

## 6) AuthN/AuthZ va Middleware

## 6.1 JWT Strategy

- Access token: short TTL (10-15 phut).
- Refresh token: long TTL (7-30 ngay), luu HTTP-only cookie.
- Frontend giu access token trong memory (hoac secure cookie neu BFF).

## 6.2 Next.js Middleware

- Kiem tra route protected:
  - `/cart`, `/checkout`, `/orders/*`, `/chat/*`, `/admin/*`
- Redirect:
  - unauth -> `/login?next=...`
  - user role != ADMIN vao `/admin/*` -> `/403`

## 6.3 RBAC tren UI

- Route-level guard + component-level guard.
- Hidden actions:
  - USER khong thay nut CRUD admin.
  - ADMIN co full action cho user/product/order.

## 7) Product Module Frontend Design

## 7.1 User Features

- Product list:
  - filters: category, keyword, sort
  - pagination
- Product detail:
  - image gallery
  - stock status
  - add-to-cart

## 7.2 Admin Features

- Product table with search/filter/status.
- CRUD form:
  - name, description, price, stock, category, status
  - multi-image upload (thumbnail + gallery)
- Soft delete:
  - `status = INACTIVE`, khong xoa hard row.

## 7.3 Image Strategy

- Upload len object storage (S3-compatible).
- Su dung image CDN URL trong product payload.
- Frontend render qua `next/image`.

## 8) Cart va Checkout UX Flow

## 8.1 Cart Rules

- Add item, update qty, remove item.
- Validate stock moi lan update.
- Cart luu DB, sync cross-device.

## 8.2 Checkout

1. User nhap shipping info.
2. Review order (items + shipping + tong tien).
3. Chon payment: Stripe/VNPay/COD.
4. Tao order (`Pending`), tao payment record (`Pending`).
5. Redirect payment gateway neu prepaid.

## 8.3 Idempotency

- Frontend gui `Idempotency-Key` khi place order.
- Tranh double submit do network retry.

## 9) Order Module va Snapshot

## 9.1 User

- `GET /orders/my-orders`
- `GET /orders/{id}`
- Trang thai: Pending, Shipping, Completed, Cancelled

## 9.2 Admin

- `GET /admin/orders`
- `PATCH /admin/orders/{id}/status`

## 9.3 Vi sao OrderItems can snapshot

`OrderItems` phai luu:
- `productName`
- `price`
- (khuyen nghi them `thumbnail`, `sku`)

Ly do:
- Bao toan lich su giao dich tai thoi diem mua.
- Product co the bi doi ten, doi gia, inactive, hoac xoa.
- Bao cao doanh thu va doi soat thanh toan khong bi sai lech.

## 10) Realtime Chat Design (User <-> Admin)

## 10.1 WebSocket Flow

```text
User open /chat/{productId}
  -> connect socket with JWT
  -> emit join_room { productId }
Admin dashboard chat
  -> connect socket with JWT
  -> join same room
Message send
  -> server validate auth + room
  -> persist DB (messages)
  -> broadcast room
Offline user
  -> message saved as unread
  -> fetch history on reconnect
```

## 10.2 Client Events

- `join_room`
- `message_send`
- `message_receive`
- `message_ack`
- `typing` (optional)

## 10.3 Message Payload

```json
{
  "roomId": "product:123:user:456",
  "productId": 123,
  "senderId": 456,
  "senderRole": "USER",
  "content": "San pham nay con mau den khong?",
  "type": "TEXT",
  "createdAt": "2026-03-27T10:00:00.000Z"
}
```

## 11) Performance, Cache, SEO

## 11.1 Frontend Performance

- Code splitting theo route segment.
- Dynamic import cho chart, editor, heavy admin components.
- Image optimization:
  - `next/image`
  - responsive sizes + lazy loading
- Bundle guard:
  - Tach libs admin khoi public bundle.

## 11.2 Cache Strategy

- Browser cache:
  - static assets `Cache-Control: immutable`.
- Next data cache:
  - ISR pages revalidate 60-300s.
- RTK Query cache:
  - in-memory va invalidation by tags.
- Redis (backend):
  - cache hot products/search results.

## 11.3 SEO

- Metadata per product detail (title, description, open graph).
- Structured data JSON-LD (Product, Offer).
- Canonical URL.
- Server-rendered product content.

## 12) Security Design

- JWT access + refresh.
- RBAC tren route va action.
- XSS:
  - sanitize rich text
  - strict CSP
- CSRF:
  - SameSite cookie + CSRF token voi state-changing requests neu dung cookie auth
- SQL injection:
  - backend ORM + prepared statements.
- Rate limit endpoint auth/chat.
- Validate input client + server schema (zod/class-validator).

## 13) Database Schema (Logical)

## 13.1 ERD Relationship Summary

- Roles (1) - (N) Users
- Categories (1) - (N) Products
- Products (1) - (N) ProductImages
- Users (1) - (1) Cart
- Cart (1) - (N) CartItems
- Users (1) - (N) Orders
- Orders (1) - (N) OrderItems
- Orders (1) - (N) Payments
- ChatRooms (1) - (N) Messages
- Products (1) - (N) ChatRooms (theo product context)

## 13.2 Schema DDL (PostgreSQL style)

```sql
-- =========================================
-- CREATE DATABASE
-- =========================================
CREATE DATABASE ecommerce_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE ecommerce_db;

-- =========================================
-- ROLES
-- =========================================
CREATE TABLE roles (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(20) UNIQUE NOT NULL
);

-- =========================================
-- USERS
-- =========================================
CREATE TABLE users (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  email VARCHAR(255) UNIQUE NOT NULL,
  password_hash TEXT NOT NULL,
  full_name VARCHAR(255) NOT NULL,
  role_id BIGINT NOT NULL,
  is_locked TINYINT(1) DEFAULT 0,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

  FOREIGN KEY (role_id) REFERENCES roles(id)
);

-- =========================================
-- CATEGORIES
-- =========================================
CREATE TABLE categories (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(120) NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- =========================================
-- PRODUCTS
-- =========================================
CREATE TABLE products (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(255) NOT NULL,
  description TEXT,
  price DECIMAL(12) NOT NULL,
  stock INT NOT NULL,
  status ENUM('ACTIVE','INACTIVE') NOT NULL,
  category_id BIGINT,
  thumbnail TEXT,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

  FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE CASCADE
);

-- =========================================
-- PRODUCT IMAGES
-- =========================================
CREATE TABLE product_images (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  product_id BIGINT NOT NULL,
  image_url TEXT NOT NULL,
  sort_order INT DEFAULT 0,

  FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE
);

-- =========================================
-- CARTS
-- =========================================
CREATE TABLE carts (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT UNIQUE NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- =========================================
-- CART ITEMS
-- =========================================
CREATE TABLE cart_items (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  cart_id BIGINT NOT NULL,
  product_id BIGINT NOT NULL,
  quantity INT NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

  UNIQUE KEY unique_cart_product (cart_id, product_id),

  FOREIGN KEY (cart_id) REFERENCES carts(id) ON DELETE CASCADE,
  FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE
);

-- =========================================
-- ORDERS
-- =========================================
CREATE TABLE orders (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NULL,
  status ENUM('Pending','Shipping','Completed','Cancelled') NOT NULL,
  shipping_name VARCHAR(255) NOT NULL,
  shipping_phone VARCHAR(50) NOT NULL,
  shipping_address TEXT NOT NULL,
  total_amount DECIMAL(12) DEFAULT 0,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL
);

-- =========================================
-- ORDER ITEMS
-- =========================================
CREATE TABLE order_items (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  order_id BIGINT NOT NULL,
  product_id BIGINT NULL,
  product_name VARCHAR(255) NOT NULL,
  price DECIMAL(12) NOT NULL,
  quantity INT NOT NULL,
  thumbnail TEXT,

  FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE
);

-- =========================================
-- PAYMENTS
-- =========================================
CREATE TABLE payments (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  order_id BIGINT NOT NULL,
  method ENUM('COD','STRIPE','VNPAY') NOT NULL,
  status ENUM('Pending','Paid','Failed') NOT NULL,
  transaction_ref VARCHAR(255),
  paid_at TIMESTAMP NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

  FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE
);

-- =========================================
-- CHAT ROOMS
-- =========================================
CREATE TABLE chat_rooms (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  product_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  admin_id BIGINT,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

  UNIQUE KEY unique_product_user (product_id, user_id),

  FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE,
  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
  FOREIGN KEY (admin_id) REFERENCES users(id) ON DELETE SET NULL
);

-- =========================================
-- MESSAGES
-- =========================================
CREATE TABLE messages (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  room_id BIGINT NOT NULL,
  sender_id BIGINT NOT NULL,
  content TEXT NOT NULL,
  message_type ENUM('TEXT') DEFAULT 'TEXT',
  is_read TINYINT(1) DEFAULT 0,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

  FOREIGN KEY (room_id) REFERENCES chat_rooms(id) ON DELETE CASCADE,
  FOREIGN KEY (sender_id) REFERENCES users(id) ON DELETE CASCADE
);

-- =========================================
-- TRIGGERS
-- =========================================
DELIMITER $$

CREATE TRIGGER trg_check_stock
BEFORE INSERT ON order_items
FOR EACH ROW
BEGIN
  DECLARE stock_val INT;
  SELECT stock INTO stock_val FROM products WHERE id = NEW.product_id;

  IF stock_val < NEW.quantity THEN
    SIGNAL SQLSTATE '45000'
    SET MESSAGE_TEXT = 'Not enough stock';
  END IF;
END$$

CREATE TRIGGER trg_update_stock
AFTER INSERT ON order_items
FOR EACH ROW
BEGIN
  UPDATE products
  SET stock = stock - NEW.quantity
  WHERE id = NEW.product_id;
END$$

CREATE TRIGGER trg_update_total
AFTER INSERT ON order_items
FOR EACH ROW
BEGIN
  UPDATE orders
  SET total_amount = (
    SELECT SUM(price * quantity)
    FROM order_items
    WHERE order_id = NEW.order_id
  )
  WHERE id = NEW.order_id;
END$$

CREATE TRIGGER trg_create_cart
AFTER INSERT ON users
FOR EACH ROW
BEGIN
  INSERT INTO carts(user_id) VALUES (NEW.id);
END$$

DELIMITER ;

-- =========================================
-- SEED DATA
-- =========================================

-- ROLES
INSERT INTO roles (name) VALUES ('USER'), ('ADMIN');

-- USERS
INSERT INTO users (email, password_hash, full_name, role_id) VALUES
('user1@gmail.com', '123', 'Nguyen Van A', 1),
('user2@gmail.com', '123', 'Tran Thi B', 1),
('user3@gmail.com', '123', 'Le Van C', 1),
('admin@gmail.com', '123', 'Admin', 2);

-- CATEGORIES
INSERT INTO categories (name) VALUES
('Điện thoại'),
('Laptop'),
('Phụ kiện');

-- PRODUCTS
INSERT INTO products (name, description, price, stock, status, category_id, thumbnail) VALUES
('iPhone 15', 'Apple flagship', 20000000, 10, 'ACTIVE', 1, 'iphone.jpg'),
('Samsung S23', 'Android flagship', 18000000, 15, 'ACTIVE', 1, 'samsung.jpg'),
('Xiaomi 13', 'Budget flagship', 12000000, 20, 'ACTIVE', 1, 'xiaomi.jpg'),

('Macbook M2', 'Apple laptop', 30000000, 5, 'ACTIVE', 2, 'macbook.jpg'),
('Dell XPS 13', 'Premium laptop', 28000000, 7, 'ACTIVE', 2, 'dell.jpg'),

('AirPods Pro', 'Apple earphone', 5000000, 25, 'ACTIVE', 3, 'airpods.jpg'),
('Logitech Mouse', 'Wireless mouse', 700000, 30, 'ACTIVE', 3, 'mouse.jpg');

-- IMAGES
INSERT INTO product_images (product_id, image_url) VALUES
(1,'iphone1.jpg'),(1,'iphone2.jpg'),
(2,'samsung1.jpg'),
(4,'mac1.jpg');

-- ORDERS
INSERT INTO orders (user_id, status, shipping_name, shipping_phone, shipping_address) VALUES
(1,'Pending','Nguyen Van A','0123','Hanoi'),
(2,'Completed','Tran Thi B','0456','HCM');

-- ORDER ITEMS
INSERT INTO order_items (order_id, product_id, product_name, price, quantity) VALUES
(1,1,'iPhone 15',20000000,1),
(1,6,'AirPods Pro',5000000,2),
(2,4,'Macbook M2',30000000,1);

-- PAYMENTS
INSERT INTO payments (order_id, method, status) VALUES
(1,'COD','Pending'),
(2,'VNPAY','Paid');

-- CHAT
INSERT INTO chat_rooms (product_id, user_id, admin_id) VALUES
(1,1,4),
(4,2,4);

INSERT INTO messages (room_id, sender_id, content) VALUES
(1,1,'Sản phẩm còn không?'),
(1,4,'Còn bạn nhé'),
(2,2,'Laptop này bảo hành bao lâu?');
```

## 13.3 Indexing Strategy

- `products(status, category_id, created_at desc)`
- `products USING gin(to_tsvector('simple', name || ' ' || coalesce(description,'')))`
- `orders(user_id, created_at desc)`
- `orders(status, created_at desc)`
- `messages(room_id, created_at desc)`
- `cart_items(cart_id, product_id)` unique index

## 14) API Design (Frontend Contract)

## 14.1 Response Standard

### Success

```json
{
  "success": true,
  "message": "string",
  "data": {},
  "error": null,
  "timestamp": "ISO8601"
}
```

### Error

```json
{
  "success": false,
  "message": "Error message",
  "data": null,
  "error": {
    "code": "ERROR_CODE",
    "details": []
  },
  "timestamp": "ISO8601"
}
```

## 14.2 Pagination Standard

```json
{
  "items": [],
  "page": 1,
  "pageSize": 20,
  "totalItems": 245,
  "totalPages": 13
}
```

## 14.3 Product APIs

- `GET /products?keyword=&categoryId=&sort=&page=&pageSize=`
- `GET /products/{id}`
- `POST /admin/products`
- `PUT /admin/products/{id}`
- `DELETE /admin/products/{id}` (soft delete -> INACTIVE)

## 14.4 Cart APIs

- `GET /cart`
- `POST /cart`
- `PUT /cart/{id}`
- `DELETE /cart/{id}`

## 14.5 Order APIs

- `POST /orders`
- `GET /orders/my-orders`
- `GET /admin/orders`
- `PATCH /admin/orders/{id}/status`

## 14.6 User APIs

- `GET /admin/users`
- `PATCH /admin/users/{id}/lock`

## 14.7 Chat APIs + WebSocket

- WebSocket endpoint: `/ws/chat` (Socket.IO hoac native ws gateway)
- REST history:
  - `GET /chat/rooms`
  - `GET /chat/rooms/{roomId}/messages?page=1&pageSize=30`

## 15) JSON Response Examples

## 15.1 Product List

```json
{
  "success": true,
  "message": "Products fetched",
  "data": {
    "items": [
      {
        "id": 101,
        "name": "Nike Air Zoom",
        "price": 129.99,
        "stock": 20,
        "status": "ACTIVE",
        "categoryId": 5,
        "thumbnail": "https://cdn.example.com/p/101-thumb.jpg",
        "createdAt": "2026-03-20T10:00:00.000Z",
        "updatedAt": "2026-03-25T08:00:00.000Z"
      }
    ],
    "page": 1,
    "pageSize": 20,
    "totalItems": 241,
    "totalPages": 13
  },
  "error": null,
  "timestamp": "2026-03-27T10:20:00.000Z"
}
```

## 15.2 Product Detail

```json
{
  "success": true,
  "message": "Product detail fetched",
  "data": {
    "id": 101,
    "name": "Nike Air Zoom",
    "description": "Lightweight running shoes",
    "price": 129.99,
    "stock": 20,
    "status": "ACTIVE",
    "categoryId": 5,
    "thumbnail": "https://cdn.example.com/p/101-thumb.jpg",
    "images": [
      "https://cdn.example.com/p/101-1.jpg",
      "https://cdn.example.com/p/101-2.jpg"
    ],
    "createdAt": "2026-03-20T10:00:00.000Z",
    "updatedAt": "2026-03-25T08:00:00.000Z"
  },
  "error": null,
  "timestamp": "2026-03-27T10:20:00.000Z"
}
```

## 15.3 Cart

```json
{
  "success": true,
  "message": "Cart fetched",
  "data": {
    "id": 88,
    "userId": 456,
    "items": [
      {
        "id": 9001,
        "productId": 101,
        "productName": "Nike Air Zoom",
        "thumbnail": "https://cdn.example.com/p/101-thumb.jpg",
        "price": 129.99,
        "quantity": 2,
        "lineTotal": 259.98,
        "stock": 20
      }
    ],
    "subTotal": 259.98
  },
  "error": null,
  "timestamp": "2026-03-27T10:20:00.000Z"
}
```

## 15.4 Order

```json
{
  "success": true,
  "message": "Order placed",
  "data": {
    "id": 5001,
    "status": "Pending",
    "paymentStatus": "Pending",
    "paymentMethod": "STRIPE",
    "items": [
      {
        "productId": 101,
        "productName": "Nike Air Zoom",
        "price": 129.99,
        "quantity": 2
      }
    ],
    "totalAmount": 259.98,
    "shipping": {
      "name": "Nguyen Van A",
      "phone": "0900000000",
      "address": "123 Nguyen Trai, Q1, HCM"
    },
    "createdAt": "2026-03-27T10:20:00.000Z"
  },
  "error": null,
  "timestamp": "2026-03-27T10:20:00.000Z"
}
```

## 16) Frontend-Backend Integration Contracts

## 16.1 Product List Query Parameters

- `keyword: string`
- `categoryId: number`
- `sort: newest | price_asc | price_desc`
- `page: number`
- `pageSize: number`

## 16.2 Checkout Request

```json
{
  "shippingName": "Nguyen Van A",
  "shippingPhone": "0900000000",
  "shippingAddress": "123 Nguyen Trai, Q1, HCM",
  "paymentMethod": "STRIPE",
  "idempotencyKey": "f2ad1f5f-2bcd-4c0f-a1f9-72b1d2ca9f8d"
}
```

## 17) Deployment va Operability

## 17.1 Deployment Topology

- Frontend: Vercel (multi-region edge)
- Backend: Docker (Kubernetes/ECS/Fly/Render)
- DB: PostgreSQL managed
- Redis: managed
- Object storage + CDN cho media

## 17.2 CI/CD

- PR checks:
  - lint, typecheck, unit tests
  - e2e smoke tests
- CD:
  - preview environment per PR
  - production promotion co rollback

## 17.3 Observability

- Frontend:
  - Web Vitals (LCP, CLS, INP)
  - Sentry cho client + server actions
- Backend:
  - metrics (RPS, p95 latency, error rate)
  - distributed tracing

## 18) Fault Tolerance va Scalability

- Graceful fallback:
  - neu chat ws fail -> cho phep retry + fetch polling tam thoi.
- Circuit break logic tren API client cho endpoint non-critical.
- Retry voi backoff cho GET idempotent.
- Feature flags cho rollout payment method moi.
- Read-heavy traffic:
  - cache product list/detail, ISR + Redis.

## 19) Implementation Roadmap (Frontend)

1. Setup project baseline: Next App Router + Redux Toolkit + RTK Query + lint/test.
2. Build auth flow + middleware + role guard.
3. Product listing/detail SSR-ISR + search/filter/sort.
4. Cart + checkout + order history.
5. Admin modules: product/user/order management.
6. Realtime chat module.
7. Analytics dashboard.
8. Performance hardening + security headers + observability.

## 20) Ket Luan Kien Truc

Thiet ke nay toi uu cho giai doan production dau tien:

- SEO va performance tot nho SSR/ISR + CDN + cache layer
- De maintain nho module-based frontend va response standard thong nhat
- Du an toan nho JWT refresh flow, middleware guard, RBAC, CSP/CSRF strategy
- San sang scale 10k+ users va co lo trinh tach service khi can

Neu ban muon, buoc tiep theo minh co the scaffold ngay `src/` structure + boilerplate RTK Query endpoints + middleware auth theo dung tai lieu nay.
