CREATE DATABASE IF NOT EXISTS diet_ai_planner DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE diet_ai_planner;

INSERT INTO user (username, email, password_hash, real_name, gender, birth_date, height, weight, activity_level, diet_goal, role, status, created_at, updated_at, deleted)
VALUES ('admin', 'admin@dietai.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '系统管理员', 1, '1990-01-01', 175.0, 70.0, 3, 'maintain', 'admin', 1, NOW(), NOW(), 0)
ON DUPLICATE KEY UPDATE id=id;

INSERT INTO food_category (name, parent_id, sort_order, icon, status) VALUES
('主食', 0, 1, 'rice', 1),
('肉类', 0, 2, 'meat', 1),
('蔬菜', 0, 3, 'vegetable', 1),
('水果', 0, 4, 'fruit', 1),
('奶制品', 0, 5, 'milk', 1),
('豆制品', 0, 6, 'bean', 1),
('零食', 0, 7, 'snack', 1),
('饮品', 0, 8, 'drink', 1),
('海鲜', 0, 9, 'seafood', 1),
('蛋类', 0, 10, 'egg', 1);

INSERT INTO food (name, category_id, calories, protein, carbohydrate, fat, fiber, image_url, source, status, created_by, created_at, updated_at) VALUES
('白米饭', 1, 116.00, 2.60, 25.90, 0.30, 0.30, NULL, 'system', 'approved', NULL, NOW(), NOW()),
('馒头', 1, 221.00, 7.00, 47.00, 1.10, 1.30, NULL, 'system', 'approved', NULL, NOW(), NOW()),
('面条(煮)', 1, 110.00, 3.50, 22.80, 0.40, 0.80, NULL, 'system', 'approved', NULL, NOW(), NOW()),
('糙米饭', 1, 111.00, 2.60, 23.50, 0.90, 1.80, NULL, 'system', 'approved', NULL, NOW(), NOW()),
('鸡胸肉', 2, 133.00, 31.00, 0.00, 3.60, 0.00, NULL, 'system', 'approved', NULL, NOW(), NOW()),
('猪瘦肉', 2, 143.00, 20.30, 1.50, 6.20, 0.00, NULL, 'system', 'approved', NULL, NOW(), NOW()),
('牛肉', 2, 125.00, 19.90, 0.00, 4.20, 0.00, NULL, 'system', 'approved', NULL, NOW(), NOW()),
('鸡蛋', 10, 144.00, 13.30, 1.50, 8.80, 0.00, NULL, 'system', 'approved', NULL, NOW(), NOW()),
('牛奶', 5, 54.00, 3.00, 3.40, 3.20, 0.00, NULL, 'system', 'approved', NULL, NOW(), NOW()),
('酸奶', 5, 72.00, 2.50, 9.30, 2.70, 0.00, NULL, 'system', 'approved', NULL, NOW(), NOW()),
('番茄', 3, 15.00, 0.90, 3.30, 0.20, 0.50, NULL, 'system', 'approved', NULL, NOW(), NOW()),
('西兰花', 3, 36.00, 4.10, 4.30, 0.60, 3.70, NULL, 'system', 'approved', NULL, NOW(), NOW()),
('菠菜', 3, 28.00, 2.90, 3.60, 0.40, 2.70, NULL, 'system', 'approved', NULL, NOW(), NOW()),
('苹果', 4, 53.00, 0.20, 13.70, 0.20, 2.10, NULL, 'system', 'approved', NULL, NOW(), NOW()),
('香蕉', 4, 93.00, 1.40, 22.00, 0.20, 1.20, NULL, 'system', 'approved', NULL, NOW(), NOW()),
('橙子', 4, 48.00, 0.80, 11.10, 0.20, 0.60, NULL, 'system', 'approved', NULL, NOW(), NOW()),
('豆腐', 6, 82.00, 8.10, 4.20, 3.70, 0.40, NULL, 'system', 'approved', NULL, NOW(), NOW()),
('清蒸鱼', 9, 109.00, 19.60, 0.00, 3.60, 0.00, NULL, 'system', 'approved', NULL, NOW(), NOW()),
('虾', 9, 87.00, 18.60, 0.80, 0.80, 0.00, NULL, 'system', 'approved', NULL, NOW(), NOW()),
('燕麦片', 1, 377.00, 13.50, 66.30, 6.70, 5.30, NULL, 'system', 'approved', NULL, NOW(), NOW());

INSERT INTO nutrition_standard (gender, age_min, age_max, calories_kcal, protein_g, carb_g, fat_g) VALUES
(1, 18, 30, 2600, 65.00, 325.00, 72.00),
(1, 31, 50, 2400, 60.00, 300.00, 67.00),
(1, 51, 70, 2200, 55.00, 275.00, 61.00),
(2, 18, 30, 2100, 55.00, 263.00, 58.00),
(2, 31, 50, 1900, 50.00, 238.00, 53.00),
(2, 51, 70, 1700, 45.00, 213.00, 47.00);