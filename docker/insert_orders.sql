-- ============================================================
-- insert_orders.sql
-- orders 10만 건 + order_items 10만 건 삽입
-- 100명 × 1,000건 균등 분배
-- ============================================================

SET SESSION cte_max_recursion_depth = 1000;

-- Step 1: orders 10만 건 삽입
INSERT INTO orders (user_id, status, original_total_price, discount_amount, final_total_price, user_coupon_id, created_at, updated_at)
WITH RECURSIVE seq AS (
  SELECT 1 AS n UNION ALL SELECT n + 1 FROM seq WHERE n < 1000
),
base AS (
  SELECT
    u.id AS user_id,
    FLOOR(10000 + RAND() * 490000) AS price,
    RAND() AS r
  FROM (SELECT id FROM users WHERE deleted_at IS NULL) u
  CROSS JOIN seq
)
SELECT
  user_id,
  CASE
    WHEN r < 0.4 THEN 'ORDERED'
    WHEN r < 0.7 THEN 'SHIPPING'
    WHEN r < 0.9 THEN 'DELIVERED'
    ELSE 'CANCELLED'
  END,
  price,
  0,
  price,
  NULL,
  DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 730) DAY),
  NOW()
FROM base;

-- Step 2: order_items 10만 건 삽입 (MOD 순환 매핑 — 누락 없이 전 주문 커버)
INSERT INTO order_items (order_id, product_id, product_name, brand_name, quantity, unit_price, created_at, updated_at)
SELECT
  o.id,
  p.id,
  p.name,
  b.name AS brand_name,
  1 + FLOOR(RAND() * 5),
  p.price,
  o.created_at,
  NOW()
FROM (
  SELECT id, created_at, ROW_NUMBER() OVER (ORDER BY id) - 1 AS rn
  FROM orders
  WHERE deleted_at IS NULL
) o
JOIN (
  SELECT id, name, brand_id, price,
         ROW_NUMBER() OVER (ORDER BY id) - 1 AS rn
  FROM products
  WHERE deleted_at IS NULL
) p ON p.rn = o.rn MOD (SELECT COUNT(*) FROM products WHERE deleted_at IS NULL)
JOIN brands b ON p.brand_id = b.id;

-- ============================================================
-- 검증 쿼리
-- SELECT COUNT(*) FROM orders;                                        -- 100000
-- SELECT COUNT(DISTINCT user_id) FROM orders;                         -- 100
-- SELECT user_id, COUNT(*) cnt FROM orders GROUP BY user_id LIMIT 3; -- 각 1000건
-- SELECT COUNT(*) FROM order_items;                                   -- 100000
-- SELECT COUNT(DISTINCT order_id) FROM order_items;                   -- 100000
-- ============================================================
