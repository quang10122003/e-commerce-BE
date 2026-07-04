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
  image varchar(120) NOT NULL,
  public_id_url varchar(120) not null,
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
  price DECIMAL(12,2) NOT NULL,
  purchases int not null default 0,
  stock INT NOT NULL,
  status ENUM('ACTIVE','INACTIVE') NOT NULL,
  category_id BIGINT,
  thumbnail TEXT,
  public_id_url varchar(120) not null,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  version BIGINT NOT NULL DEFAULT 0,

  FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE CASCADE
);

-- =========================================
-- PRODUCT IMAGES
-- =========================================
CREATE TABLE product_images (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  product_id BIGINT NOT NULL,
  image_url TEXT NOT NULL,
  public_id_url varchar(120) not null,
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
  order_code VARCHAR(50)  UNIQUE,

  user_id BIGINT NULL,
  status ENUM('PENDING','SHIPPING','COMPLETED','CANCELLED') NOT NULL,

  shipping_name VARCHAR(255) NOT NULL,
  shipping_phone VARCHAR(50) NOT NULL,
  shipping_address TEXT NOT NULL,
  payment_method Enum('COD','SEPAY') null,

  total_amount DECIMAL(12,2) DEFAULT 0,

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
  category_name VARCHAR(255),
  price DECIMAL(12,2) NOT NULL,
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
  method ENUM('COD', 'SEPAY') NOT NULL,
  status ENUM('PENDING','PAID','FAILED','PAID_LATE') NOT NULL,
expired_at TIMESTAMP NULL,
  transaction_ref VARCHAR(255),
   reference_code VARCHAR(255) NULL,
  paid_at TIMESTAMP NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

  FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE
);
-- =========================================
-- CHAT ROOMS
-- =========================================
CREATE TABLE chat_rooms (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  product_id BIGINT NULL,
  user_id BIGINT NOT NULL,
  admin_id BIGINT,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

  UNIQUE KEY unique_product_user (product_id, user_id),

  FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE SET NULL,
  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
  FOREIGN KEY (admin_id) REFERENCES users(id) ON DELETE SET NULL
);

-- =========================================
-- MESSAGES
-- =========================================
CREATE TABLE messages (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  room_id BIGINT NOT NULL,
  sender_id BIGINT,
  content TEXT NOT NULL,
  message_type ENUM('TEXT','SYSTEM') NOT NULL DEFAULT 'TEXT',
  is_read TINYINT(1) DEFAULT 0,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

  FOREIGN KEY (room_id) REFERENCES chat_rooms(id) ON DELETE CASCADE,
  FOREIGN KEY (sender_id) REFERENCES users(id) ON DELETE SET NULL
);

CREATE TABLE password_reset_token (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,

    token VARCHAR(255) NOT NULL UNIQUE,

    used TINYINT(1) NOT NULL DEFAULT 0,

    expired_at DATETIME,

    user_id BIGINT NOT NULL,

    FOREIGN KEY (user_id)
    REFERENCES users(id)
);

-- =========================================
-- TRIGGERS
-- =========================================
DELIMITER $$

/* =====================================================
   1. Kiểm tra tồn kho trước khi thêm sản phẩm vào đơn hàng
   ===================================================== */
CREATE TRIGGER trg_check_stock
BEFORE INSERT ON order_items
FOR EACH ROW
BEGIN
    DECLARE stock_val INT;

    SELECT stock
    INTO stock_val
    FROM products
    WHERE id = NEW.product_id;

    IF stock_val < NEW.quantity THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Not enough stock';
    END IF;
END$$


/* =====================================================
   2. Trừ tồn kho sau khi thêm sản phẩm vào đơn hàng
   ===================================================== */
CREATE TRIGGER trg_update_stock
AFTER INSERT ON order_items
FOR EACH ROW
BEGIN
    UPDATE products
    SET stock = stock - NEW.quantity
    WHERE id = NEW.product_id;
END$$


/* =====================================================
   3. Cập nhật tổng tiền đơn hàng tự động
   ===================================================== */
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


/* =====================================================
   4. Tự động tạo giỏ hàng khi đăng ký tài khoản
   ===================================================== */
CREATE TRIGGER trg_create_cart
AFTER INSERT ON users
FOR EACH ROW
BEGIN
    INSERT INTO carts(user_id)
    VALUES (NEW.id);
END$$

/* =====================================================
   5. Tăng purchases khi thêm order_item
   ===================================================== */
CREATE TRIGGER trg_increase_purchases
AFTER INSERT ON order_items
FOR EACH ROW
BEGIN
    UPDATE products
    SET purchases = purchases + NEW.quantity
    WHERE id = NEW.product_id;
END$$


/* =====================================================
   6. Giảm purchases khi order bị CANCELLED
   ===================================================== */
CREATE TRIGGER trg_decrease_purchases_on_cancel
AFTER UPDATE ON orders
FOR EACH ROW
BEGIN
    IF NEW.status = 'CANCELLED' AND OLD.status <> 'CANCELLED' THEN
        UPDATE products p
        JOIN order_items oi ON oi.product_id = p.id
        SET p.purchases = p.purchases - oi.quantity
        WHERE oi.order_id = NEW.id;
    END IF;
END$$

DELIMITER ;


-- =========================================
-- SEED DATA
-- =========================================

-- ROLES
INSERT INTO roles (name) VALUES ('USER'), ('ADMIN');

-- USERS
INSERT INTO users (email, password_hash, full_name, role_id) VALUES
('user1@gmail.com', '$2a$10$iZxdSlgLG4Jsd3GhNxDteOpM72w9f2Zd8TWHliE.wX8hgzLxvS6NO', 'Nguyen Van A', 1),
('user2@gmail.com', '$2a$10$iZxdSlgLG4Jsd3GhNxDteOpM72w9f2Zd8TWHliE.wX8hgzLxvS6NO', 'Tran Thi B', 1),
('user3@gmail.com', '$2a$10$iZxdSlgLG4Jsd3GhNxDteOpM72w9f2Zd8TWHliE.wX8hgzLxvS6NO', 'Le Van C', 1),
('admin@gmail.com', '$2a$10$iZxdSlgLG4Jsd3GhNxDteOpM72w9f2Zd8TWHliE.wX8hgzLxvS6NO', 'Admin', 2);

-- =========================
-- CATEGORIES (7)
-- =========================
INSERT INTO categories (name, image) VALUES
('Điện thoại','https://images.unsplash.com/photo-1511707171634-5f897ff02aa9'),
('Laptop','https://images.unsplash.com/photo-1517336714731-489689fd1ca8'),
('Phụ kiện','https://images.unsplash.com/photo-1585386959984-a4155224a1ad'),
('Tai nghe','https://images.unsplash.com/photo-1518444028785-8b0c5c8f5c3d'),
('Bàn phím','https://images.unsplash.com/photo-1517336714731-489689fd1ca8'),
('Chuột','https://images.unsplash.com/photo-1585386959984-a4155224a1ad'),
('Màn hình','https://images.unsplash.com/photo-1518444028785-8b0c5c8f5c3d');

-- =========================
-- PRODUCTS (~20)
-- =========================
INSERT INTO products (name, description, price, stock, status, category_id, thumbnail) VALUES
('iPhone 15', 'Apple flagship', 20000000, 10, 'ACTIVE', 1, ''),
('iPhone 14', 'Apple previous gen', 17000000, 12, 'ACTIVE', 1, ''),
('Samsung S23', 'Android flagship', 18000000, 15, 'ACTIVE', 1, ''),
('Xiaomi 13', 'Budget flagship', 12000000, 20, 'ACTIVE', 1, ''),

('Macbook M2', 'Apple laptop', 30000000, 5, 'ACTIVE', 2, ''),
('Macbook M1', 'Old gen', 22000000, 6, 'ACTIVE', 2, ''),
('Dell XPS 13', 'Premium laptop', 28000000, 7, 'ACTIVE', 2, ''),
('Asus ROG', 'Gaming laptop', 35000000, 4, 'ACTIVE', 2, ''),

('USB 64GB', 'Storage device', 200000, 50, 'ACTIVE', 3, ''),
('Sạc nhanh 20W', 'Fast charger', 300000, 40, 'ACTIVE', 3, ''),

('AirPods Pro', 'Apple earphone', 5000000, 25, 'ACTIVE', 4, ''),
('Sony WH-1000XM5', 'Noise canceling', 7000000, 10, 'ACTIVE', 4, ''),

('Keychron K6', 'Mechanical keyboard', 2500000, 15, 'ACTIVE', 5, ''),
('Logitech K380', 'Wireless keyboard', 800000, 20, 'ACTIVE', 5, ''),

('Logitech MX Master 3', 'Premium mouse', 2500000, 18, 'ACTIVE', 6, ''),
('Razer DeathAdder', 'Gaming mouse', 1200000, 22, 'ACTIVE', 6, ''),

('LG 27inch 4K', '4K Monitor', 8000000, 8, 'ACTIVE', 7, ''),
('Dell Ultrasharp', 'Professional monitor', 9000000, 6, 'ACTIVE', 7, ''),
('Samsung Odyssey', 'Gaming monitor', 10000000, 5, 'ACTIVE', 7, ''),
('AOC 24inch', 'Budget monitor', 4000000, 12, 'ACTIVE', 7, '');

-- =========================
-- CART ITEMS (mỗi user ~10)
-- =========================

-- User 1 (cart_id = 1)
INSERT INTO cart_items (cart_id, product_id, quantity) VALUES
(1,1,1),(1,2,1),(1,3,2),(1,4,1),(1,5,1),
(1,6,1),(1,7,1),(1,8,1),(1,9,3),(1,10,2);

-- User 2 (cart_id = 2)
INSERT INTO cart_items (cart_id, product_id, quantity) VALUES
(2,11,1),(2,12,1),(2,13,1),(2,14,2),(2,15,1),
(2,16,1),(2,17,1),(2,18,1),(2,19,1),(2,20,1);

-- User 3 (cart_id = 3)
INSERT INTO cart_items (cart_id, product_id, quantity) VALUES
(3,1,1),(3,5,1),(3,9,2),(3,10,1),(3,11,1),
(3,12,1),(3,13,1),(3,14,1),(3,15,1),(3,16,1);

-- IMAGES
INSERT INTO product_images (product_id, image_url) VALUES
(1,'https://images.unsplash.com/photo-1511707171634-5f897ff02aa9'),(1,'https://images.unsplash.com/photo-1511707171634-5f897ff02aa9'),
(2,'https://images.unsplash.com/photo-1511707171634-5f897ff02aa9'),
(4,'https://images.unsplash.com/photo-1511707171634-5f897ff02aa9');

-- ORDERS
INSERT INTO orders (user_id, status, shipping_name, shipping_phone, shipping_address) VALUES
(1,'PENDING','Nguyen Van A','0123','Hanoi'),
(2,'COMPLETED','Tran Thi B','0456','HCM');

-- ORDER ITEMS
INSERT INTO order_items (order_id, product_id, product_name, price, quantity) VALUES
(1,1,'iPhone 15',20000000,1),
(1,6,'AirPods Pro',5000000,2),
(2,4,'Macbook M2',30000000,1);

-- PAYMENTS
INSERT INTO payments (order_id, method, status) VALUES
(1,'COD','PENDING'),
(2,'VNPAY','PAID');

-- CHAT
INSERT INTO chat_rooms (product_id, user_id, admin_id) VALUES
(1,1,4),
(4,2,4);

INSERT INTO messages (room_id, sender_id, content) VALUES
(1,1,'Sản phẩm còn không?'),
(1,4,'Còn bạn nhé'),
(2,2,'Laptop này bảo hành bao lâu?');
