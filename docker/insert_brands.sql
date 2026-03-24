INSERT INTO brands (name, description, created_at, updated_at)
WITH RECURSIVE seq AS (
  SELECT 1 AS n
  UNION ALL
  SELECT n + 1 FROM seq WHERE n < 100
)
SELECT
  CONCAT(
    ELT(1 + ((n - 1) % 10),
      '스포츠', '패션', '아웃도어', '캐주얼', '럭셔리',
      '빈티지', '모던', '클래식', '어반', '스트리트'),
    ELT(1 + FLOOR((n - 1) / 10) % 10,
      '코리아', '인터내셔널', '컴퍼니', '스튜디오', '라이프',
      '웨어', '브랜드', '하우스', '컬렉션', '팩토리'),
    '_', n
  ),
  CONCAT(
    ELT(1 + ((n - 1) % 5), '글로벌', '국내', '유럽', '미국', '아시아'),
    ' ',
    ELT(1 + ((n - 1) % 4), '스포츠', '패션', '라이프스타일', '아웃도어'),
    ' 브랜드'
  ),
  DATE_SUB(NOW(), INTERVAL ((n - 1) % 365) DAY),
  NOW()
FROM seq;
