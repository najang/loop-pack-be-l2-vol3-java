SET SESSION cte_max_recursion_depth = 100000;

INSERT INTO products (
  brand_id, name, description, price, stock,
  selling_status, like_count, version, created_at, updated_at, deleted_at
)
WITH RECURSIVE seq AS (
  SELECT 1 AS n
  UNION ALL
  SELECT n + 1 FROM seq WHERE n < 100000
),
brand_range AS (
  SELECT MIN(id) AS min_id, MAX(id) AS max_id FROM brands WHERE deleted_at IS NULL
)
SELECT
  -- brand_id: 랜덤 분포
  (SELECT FLOOR(min_id + RAND() * (max_id - min_id + 1)) FROM brand_range),

  -- name: 카테고리 10종 + 번호
  CONCAT(
    ELT(1 + ((seq.n - 1) % 10),
      '운동화', '패딩', '청바지', '티셔츠', '가방',
      '모자', '양말', '재킷', '반바지', '후드'),
    '_', seq.n
  ),

  -- description: 33% NULL
  IF(seq.n % 3 = 0, NULL, CONCAT('상품 설명 ', seq.n)),

  -- price: 10,000 ~ 500,000 (만 단위, 50단계 균등)
  (1 + ((seq.n - 1) % 50)) * 10000,

  -- stock: 0 ~ 200 (201단계 순환)
  (seq.n - 1) % 201,

  -- selling_status: SELLING 70%, STOP 20%, SOLD_OUT 10%
  ELT(
    CASE
      WHEN (seq.n % 10) < 7 THEN 1
      WHEN (seq.n % 10) < 9 THEN 2
      ELSE 3
    END,
    'SELLING', 'STOP', 'SOLD_OUT'
  ),

  -- like_count: 0 ~ 4999 (long-tail 분포)
  FLOOR(POW(RAND(), 3) * 5000),

  0,

  -- created_at: 최근 2년 내 균등 분포
  DATE_SUB(NOW(), INTERVAL ((seq.n - 1) % 730) DAY),

  NOW(),

  -- deleted_at: 5% soft-delete
  IF(RAND() < 0.05, DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 365) DAY), NULL)

FROM seq;
