INSERT INTO users (login_id, password, name, email, birth_date, created_at, updated_at)
WITH RECURSIVE seq AS (
  SELECT 1 AS n
  UNION ALL
  SELECT n + 1 FROM seq WHERE n < 100
)
SELECT
  CONCAT('user', n),
  '$2y$10$tMg9qjlkEdYLAZ86XRSXm.Y5L40MNxNQZHMXG75FP9XC6g8mAMZg2',
  CONCAT(
    ELT(1 + ((n - 1) % 10), '김', '이', '박', '최', '정', '강', '조', '윤', '장', '임'),
    ELT(1 + ((n - 1) % 20),
      '민준', '서준', '도윤', '예준', '시우',
      '하준', '지후', '지훈', '준서', '준우',
      '현우', '우진', '건우', '민재', '지원',
      '수아', '서연', '지우', '하은', '민서')
  ),
  CONCAT('user', n, '@loopers.com'),
  DATE_SUB('2000-01-01', INTERVAL FLOOR(RAND() * 3650) DAY),
  DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 365) DAY),
  NOW()
FROM seq;
