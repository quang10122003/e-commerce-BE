-- Chuyển các cột tiền sang DECIMAL(12) để lưu giá trị VND dạng số nguyên.
ALTER TABLE products MODIFY price DECIMAL(12) NOT NULL;
ALTER TABLE orders MODIFY total_amount DECIMAL(12) DEFAULT 0;
ALTER TABLE order_items MODIFY price DECIMAL(12) NOT NULL;
