INSERT INTO likes (user_id, product_id, created_at, updated_at)
SELECT
  u.id,
  p.id,
  DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 730) DAY),
  NOW()
FROM (
  SELECT id FROM users WHERE deleted_at IS NULL ORDER BY RAND() LIMIT 100
) u
CROSS JOIN (
  SELECT id FROM products WHERE deleted_at IS NULL ORDER BY RAND() LIMIT 1000
) p;
