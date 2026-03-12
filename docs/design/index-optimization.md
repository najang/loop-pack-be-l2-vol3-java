# 1. 상품 목록 조회

- 실행 환경: 브랜드 10,000개, 상품 100,000개, soft-delete 비율 5%

## 1-1. 좋아요 순 정렬

**쿼리**

```sql
SELECT *
  FROM products
 WHERE deleted_at IS NULL
 ORDER BY like_count DESC
 LIMIT 20;
```

**분석**

| type | rows | Extra |
| --- | --- | --- |
| ALL | 99465 | Using where; Using filesort |

**문제**

- `deleted_at IS NULL` 조건의 선택도가 낮아 필터링만으로는 조회 대상 건수를 충분히 줄이기 어려움
- `like_count DESC` 정렬에 사용할 인덱스가 없어 전체 데이터를 스캔한 뒤 `filesort`를 수행함
- 상위 20개 상품만 필요하지만, 이를 위해 전체 데이터 약 9.9만 건을 읽고 정렬하는 비효율이 발생함

**실행 시간**

약 76.4ms

**인덱스 설계**

```java
@Index(name="idx_like_count", columnList="like_count DESC")
```

**설계 근거**

- 이 쿼리의 주요 비용은 `WHERE deleted_at IS NULL` 필터링보다 `ORDER BY like_count DESC LIMIT 20` 정렬 처리에서 발생함
- soft-delete 비율이 5%로 낮아 `deleted_at` 조건만으로는 충분한 범위 축소 효과를 기대하기 어려우므로, 정렬 기준인 `like_count`에 인덱스를 두는 것이 더 효과적임
- `like_count DESC` 인덱스를 사용하면 좋아요 수가 높은 순서대로 정렬된 데이터를 인덱스에서 바로 읽을 수 있어 `filesort`를 제거할 수 있음
- 또한 인덱스를 앞에서부터 스캔하면서 `deleted_at IS NULL` 조건을 확인하고, 상위 20건이 확보되는 시점에 즉시 스캔을 종료할 수 있음
- 이를 통해 불필요한 전체 스캔 및 정렬 비용을 크게 줄일 수 있음

**인덱스 생성 후 분석**

| key | type | rows | Extra |
| --- | --- | --- | --- |
| idx_like_count | index | 20 | Using where |

**개선 결과**

- actual rows: 99,465 → 20 (약 **99.98% 감소**, 약 **4,973배 개선**)
- actual time: 76.4ms → 0.478ms (약 **99.37% 감소**, 약 **160배 개선**)

---

## 1-2. 브랜드별 좋아요 순 정렬

**쿼리**

```sql
SELECT * 
  FROM products
 WHERE brand_id = ?
   AND deleted_at IS NULL
 ORDER BY like_count DESC
 LIMIT 20;
```

**분석**

| type | rows | Extra |
| --- | --- | --- |
| ALL | 99465 | Using where; Using filesort |

**문제**

- `brand_id = ?` 조건으로 특정 브랜드만 조회하지만, 이를 활용할 인덱스가 없어 전체 테이블을 스캔함
- `deleted_at IS NULL` 조건은 추가 필터 역할만 수행하고, `like_count DESC` 정렬에 사용할 인덱스가 없어 `filesort`가 발생함
- 특정 브랜드의 상위 20개 상품만 필요함에도 불구하고, 브랜드 필터링과 정렬을 위해 전체 데이터 약 9.9만 건을 읽고 정렬해야 하는 비효율이 발생함

**실행 시간**

약 56.9ms

**인덱스 설계**

```java
@Index(name="idx_brand_like_count", columnList="brand_id, like_count DESC")
```

**설계 근거**

- 이 쿼리는 `brand_id = ?`로 조회 범위를 먼저 좁힌 뒤, 해당 브랜드 내에서 `like_count DESC` 기준으로 내림차순 정렬 후 상위 20건만 조회하는 패턴임
- 따라서 `brand_id`를 선두 컬럼으로 두고, 그 뒤에 `like_count DESC`를 배치한 복합 인덱스를 설계하면 브랜드별 데이터 범위를 빠르게 찾으면서 좋아요 정렬도 인덱스로 처리할 수 있음
- `deleted_at IS NULL` 조건은 인덱스에 포함되지 않았지만, 좋아요 순으로 정렬된 인덱스를 앞에서부터 스캔하면서 조건을 확인하고 20건이 채워지는 시점에 즉시 스캔을 종료할 수 있음
- 이를 통해 전체 스캔과 `filesort`를 제거하고, 브랜드별 인기 상품 조회를 매우 빠르게 수행할 수 있음

**인덱스 생성 후 분석**

| key | type | rows | Extra |
| --- | --- | --- | --- |
| idx_brand_like_count | ref | 1067 | Using where |

`EXPLAIN`의 `rows=1067`은 최종 반환 건수가 아니라 옵티마이저가 예상한 후보 행 수이다.

실제 실행에서는 인덱스를 따라 좋아요 순으로 조회하면서 20건만 읽고 바로 종료되었다.

**개선 결과**

- actual rows: 99,465 → 20 (약 **99.98% 감소**, 약 **4,973배 개선**)
- actual time: 56.9ms → 0.611ms (약 **98.93% 감소**, 약 **93.1배 개선**)

---

## 1-3. 최신 순 정렬

**쿼리**

```sql
SELECT * 
  FROM products
 WHERE deleted_at IS NULL
 ORDER BY created_at DESC
 LIMIT 20;
```

**분석**

| type | rows | Extra |
| --- | --- | --- |
| ALL | 99465 | Using where; Using filesort |

**문제**

- `deleted_at IS NULL` 조건의 선택도가 낮아 필터링만으로는 조회 대상 건수를 충분히 줄이기 어려움
- `created_at DESC` 정렬에 사용할 인덱스가 없어 전체 데이터를 스캔한 뒤 `filesort`를 수행함
- 최신 상품 20건만 필요하지만, 이를 위해 불필요한 대량 스캔과 정렬이 발생함

**실행 시간**

약 90.2ms

**인덱스 설계**

```java
@Index(name="idx_created_at", columnList="created_at DESC")
```

**설계 근거**

- 이 쿼리의 주요 비용은 `WHERE deleted_at IS NULL` 필터링보다 `ORDER BY created_at DESC LIMIT 20` 정렬 처리에서 발생함
- soft-delete 비율이 5%로 낮아 `deleted_at` 조건만으로는 충분한 범위 축소 효과를 기대하기 어려우므로, 정렬 기준인 `created_at`에 인덱스를 두는 것이 더 효과적임
- `created_at DESC` 인덱스를 사용하면 최신 순으로 정렬된 데이터를 인덱스에서 바로 읽을 수 있어 `filesort`를 제거할 수 있음
- 또한 인덱스를 앞에서부터 스캔하면서 `deleted_at IS NULL` 조건을 확인하고, 상위 20건이 확보되는 시점에 즉시 스캔을 종료할 수 있음
- 이를 통해 불필요한 전체 스캔 및 정렬 비용을 크게 줄일 수 있음

**인덱스 생성 후 분석**

| key | type | rows | Extra |
| --- | --- | --- | --- |
| idx_created_at | index | 20 | Using where |

**개선 결과**

- actual rows: 99,465 → 20 (약 **99.98% 감소**, 약 **4,973배 개선**)
- actual time: 90.2ms → 0.184ms (약 **99.80% 감소**, 약 **490.2배 개선**)

---

## 1-4. 브랜드별 최신 순 정렬

**쿼리**

```sql
SELECT * 
  FROM products
 WHERE brand_id = ?
   AND deleted_at IS NULL
 ORDER BY created_at DESC
 LIMIT 20;
```

**분석**

| type | rows | Extra |
| --- | --- | --- |
| ALL | 99465 | Using where; Using filesort |

**문제**

- `brand_id = ?` 조건으로 특정 브랜드만 조회하지만, 이를 활용할 인덱스가 없어 전체 테이블을 스캔함
- `deleted_at IS NULL` 조건은 추가 필터 역할만 수행하고, `created_at DESC` 정렬에 사용할 인덱스가 없어 `filesort`가 발생함
- 특정 브랜드의 최신 상품 20건만 필요함에도 불구하고, 브랜드 필터링과 정렬을 위해 전체 데이터 약 9.9만 건을 읽고 정렬해야 하는 비효율이 발생함

**실행 시간**

약 70ms

**인덱스 설계**

```java
@Index(name="idx_brand_created_at", columnList="brand_id, created_at DESC")
```

**설계 근거**

- 이 쿼리는 `brand_id = ?`로 조회 범위를 먼저 좁힌 뒤, 해당 브랜드 내에서 `created_at DESC` 기준으로 내림차순 정렬 후 상위 20건만 조회하는 패턴임
- 따라서 `brand_id`를 선두 컬럼으로 두고, 그 뒤에 `created_at DESC`를 배치한 복합 인덱스를 설계하면 브랜드별 데이터 범위를 빠르게 찾으면서 최신순 정렬도 인덱스로 처리할 수 있음
- `deleted_at IS NULL` 조건은 인덱스에 포함되지 않았지만, 최신순으로 정렬된 인덱스를 앞에서부터 스캔하면서 조건을 확인하고 20건이 채워지는 시점에 즉시 스캔을 종료할 수 있음
- 이를 통해 전체 스캔과 `filesort`를 제거하고, 브랜드별 최신 상품 조회를 매우 빠르게 수행할 수 있음

**인덱스 생성 후 분석**

| key | type | rows | Extra |
| --- | --- | --- | --- |
| idx_brand_created_at | ref | 1067 | Using where |

`EXPLAIN`의 `rows=1067`은 최종 반환 건수가 아니라 옵티마이저가 예상한 후보 행 수이다.

실제 실행에서는 인덱스를 따라 최신순으로 조회하면서 20건만 읽고 바로 종료되었다.

**개선 결과**

- actual rows: 99,465 → 20 (약 **99.98% 감소**, 약 **4,973배 개선**)
- actual time: 70ms → 0.805ms (약 **98.85% 감소**, 약 **87.0배 개선**)

---

## 1-5. 가격 순 정렬

**쿼리**

```sql
SELECT * 
  FROM products
 WHERE deleted_at IS NULL
 ORDER BY price ASC
 LIMIT 20;
```

**분석**

| type | rows | Extra |
| --- | --- | --- |
| ALL | 99465 | Using where; Using filesort |

**문제**

- `deleted_at IS NULL` 조건의 선택도가 낮아 필터링만으로는 조회 대상 건수를 충분히 줄이기 어려움
- `price ASC` 정렬에 사용할 인덱스가 없어 전체 데이터를 스캔한 뒤 `filesort`를 수행함
- 최저가 상품 20건만 필요하지만, 이를 위해 전체 데이터 약 9.9만 건을 읽고 정렬하는 비효율이 발생함

**실행 시간**

약 74.7ms

**인덱스 설계**

```java
@Index(name="idx_price", columnList="price ASC")
```

**설계 근거**

- 이 쿼리의 주요 비용은 `WHERE deleted_at IS NULL` 필터링보다 `ORDER BY price ASC LIMIT 20` 정렬 처리에서 발생함
- soft-delete 비율이 5%로 낮아 `deleted_at` 조건만으로는 충분한 범위 축소 효과를 기대하기 어려우므로, 정렬 기준인 `price`에 인덱스를 두는 것이 더 효과적임
- `price ASC` 인덱스를 사용하면 가격이 낮은 순서대로 정렬된 데이터를 인덱스에서 바로 읽을 수 있어 `filesort`를 제거할 수 있음
- 또한 인덱스를 앞에서부터 스캔하면서 `deleted_at IS NULL` 조건을 확인하고, 상위 20건이 확보되는 시점에 즉시 스캔을 종료할 수 있음
- 이를 통해 불필요한 전체 스캔 및 정렬 비용을 크게 줄일 수 있음

**인덱스 생성 후 분석**

| key | type | rows | Extra |
| --- | --- | --- | --- |
| idx_price | index | 20 | Using where |

**개선 결과**

- actual rows: 99,465 → 20 (약 **99.98% 감소**, 약 **4,973배 개선**)
- actual time: 74.7ms → 0.369ms (약 **99.51% 감소**, 약 **202.4배 개선**)

---

## 1-6. 브랜드별 가격 순 정렬

**쿼리**

```sql
SELECT * 
  FROM products
 WHERE brand_id = ?
   AND deleted_at IS NULL
 ORDER BY price ASC
 LIMIT 20;
```

**분석**

| type | rows | Extra |
| --- | --- | --- |
| ALL | 99465 | Using where; Using filesort |

**문제**

- `brand_id = ?` 조건으로 특정 브랜드만 조회하지만, 이를 활용할 인덱스가 없어 전체 테이블을 스캔함
- `deleted_at IS NULL` 조건은 추가 필터 역할만 수행하고, `price ASC` 정렬에 사용할 인덱스가 없어 `filesort`가 발생함
- 특정 브랜드의 최저가 상품 20건만 필요함에도 불구하고, 브랜드 필터링과 정렬을 위해 전체 데이터 약 9.9만 건을 읽고 정렬해야 하는 비효율이 발생함

**실행 시간**

약 67.1ms

**인덱스 설계**

```java
@Index(name="idx_brand_price", columnList="brand_id, price ASC")
```

**설계 근거**

- 이 쿼리는 `brand_id = ?`로 조회 범위를 먼저 좁힌 뒤, 해당 브랜드 내에서 `price ASC` 기준으로 오름차순 정렬 후 상위 20건만 조회하는 패턴임
- 따라서 `brand_id`를 선두 컬럼으로 두고, 그 뒤에 `price ASC`를 배치한 복합 인덱스를 설계하면 브랜드별 데이터 범위를 빠르게 찾으면서 가격 정렬도 인덱스로 처리할 수 있음
- `deleted_at IS NULL` 조건은 인덱스에 포함되지 않았지만, 가격순으로 정렬된 인덱스를 앞에서부터 스캔하면서 조건을 확인하고 20건이 채워지는 시점에 즉시 스캔을 종료할 수 있음
- 이를 통해 전체 스캔과 `filesort`를 제거하고, 브랜드별 최저가 상품 조회를 매우 빠르게 수행할 수 있음

**인덱스 생성 후 분석**

| key | type | rows | Extra |
| --- | --- | --- | --- |
| idx_brand_price | ref | 1067 | Using where |

`EXPLAIN`의 `rows=1067`은 최종 반환 건수가 아니라 옵티마이저가 예상한 후보 행 수이다.

실제 실행에서는 인덱스를 따라 가격 오름차순으로 조회하면서 20건만 읽고 바로 종료되었다.

**개선 결과**

- actual rows: 99,465 → 20 (약 **99.98% 감소**, 약 **4,973배 개선**)
- actual time: 67.1ms → 0.36ms (약 **99.46% 감소**, 약 **186.4배 개선**)

---

# 2. 사용자별 좋아요 목록 조회

- 실행 환경: 좋아요 100,000개(유저 100명, 상품 1000개)

**쿼리**

```sql
EXPLAIN 
SELECT * FROM likes
 WHERE user_id = ?
 ORDER BY created_at DESC;
```

**분석**

| type | rows | Extra |
| --- | --- | --- |
| ALL | 99341 | Using where; Using filesort |

**문제**

- `user_id = ?` 조건으로 특정 사용자의 좋아요 목록만 조회하면 되지만, 이를 활용할 인덱스가 없어 전체 테이블을 스캔함
- 조회 대상은 특정 사용자의 좋아요 데이터에 불과하지만, 전체 좋아요 데이터 약 9.9만 건을 모두 읽은 뒤 `WHERE user_id = ?` 조건으로 필터링하는 비효율이 발생함
- 또한 `ORDER BY created_at DESC` 정렬에 사용할 인덱스가 없어, 필터링 이후 결과에 대해 추가적인 `filesort`가 수행됨
- 즉, 사용자 조건 필터링과 최신순 정렬을 모두 테이블 스캔 + 정렬로 처리하고 있어 데이터가 증가할수록 조회 성능이 크게 저하될 수 있음

**실행 시간**

약 44.3ms

**인덱스 설계**

```java
@Index(name="idx_user_created_at", columnList="user_id, created_at DESC")
```

**설계 근거**

- 이 쿼리는 `user_id = ?`로 특정 사용자의 좋아요만 조회한 뒤, `created_at DESC` 기준으로 최신순 정렬하는 패턴임
- 따라서 `user_id`를 선두 컬럼으로 두고, 그 뒤에 `created_at DESC`를 배치한 복합 인덱스를 설계하면 사용자별 데이터 범위를 빠르게 찾으면서 정렬도 인덱스로 처리할 수 있음
- 단일 `user_id` 인덱스만으로는 사용자별 범위 탐색은 가능하지만, `created_at DESC` 정렬까지 처리하지 못해 별도의 `filesort`가 발생할 수 있음
- 반면 `user_id, created_at DESC` 복합 인덱스는 특정 사용자에 해당하는 좋아요를 최신순으로 정렬된 상태 그대로 읽을 수 있으므로 추가 정렬 비용을 제거할 수 있음
- 이를 통해 전체 테이블 스캔과 `filesort`를 제거하고, 사용자별 좋아요 목록을 효율적으로 조회할 수 있음

**인덱스 생성 후 분석**

| key | type | rows | Extra |
| --- | --- | --- | --- |
| idx_user_created_at | ref | 1000 |  |

**개선 결과**

- actual rows: 99341 → 1000 (약 **98.99% 감소**, 약 **99.3배 개선**)
- actual time: 44.3ms → 0.666ms (약 **98.50% 감소**, 약 **66.5배 개선**)

---

# 사용자별 주문 목록 조회

- 실행 환경: 주문 100,000개(유저 100명)

**쿼리**

```sql
SELECT *
  FROM orders
 WHERE user_id = ?
 ORDER BY created_at DESC;
```

**분석**

| type | rows | Extra |
| --- | --- | --- |
| ALL | 99875 | Using where; Using filesort |

**문제**

- `user_id = ?` 조건으로 특정 사용자의 주문 목록만 조회하면 되지만, 이를 활용할 인덱스가 없어 전체 테이블을 스캔함
- 조회 대상은 특정 사용자의 주문 데이터에 불과하지만, 전체 주문 데이터 10만 건을 모두 읽은 뒤 `WHERE user_id = ?` 조건으로 필터링하는 비효율이 발생함
- 또한 `ORDER BY created_at DESC` 정렬에 사용할 인덱스가 없어, 필터링 이후 결과에 대해 추가적인 `filesort`가 수행됨
- 즉, 사용자 조건 필터링과 최신순 정렬을 모두 테이블 스캔 + 정렬로 처리하고 있어 주문 데이터가 증가할수록 조회 성능이 저하될 수 있음

**실행 시간**

약 53.2ms

**인덱스 설계**

```java
@Index(name="idx_user_created_at", columnList="user_id, created_at DESC")
```

**설계 근거**

- 이 쿼리는 `user_id = ?`로 특정 사용자의 주문만 조회한 뒤, `created_at DESC` 기준으로 최신순 정렬하는 패턴임
- 따라서 `user_id`를 선두 컬럼으로 두고, 그 뒤에 `created_at DESC`를 배치한 복합 인덱스를 설계하면 사용자별 데이터 범위를 빠르게 찾으면서 정렬도 인덱스로 처리할 수 있음
- 단일 `user_id` 인덱스만으로는 사용자별 범위 탐색은 가능하지만, `created_at DESC` 정렬까지 처리하지 못해 별도의 `filesort`가 발생할 수 있음
- 반면 `user_id, created_at DESC` 복합 인덱스는 특정 사용자에 해당하는 주문을 최신순으로 정렬된 상태 그대로 읽을 수 있으므로 추가 정렬 비용을 제거할 수 있음
- 이를 통해 전체 테이블 스캔과 `filesort`를 제거하고, 사용자별 주문 목록을 효율적으로 조회할 수 있음

**인덱스 생성 후 분석**

| key | type | rows | Extra |
| --- | --- | --- | --- |
| idx_user_created_at | ref | 1000 |  |

`EXPLAIN`의 `rows=1000`은 최종 반환 건수가 아니라 옵티마이저가 예상한 후보 행 수이다.

실제 실행에서는 `user_id = ?`에 해당하는 주문 데이터만 인덱스를 통해 조회하고, `created_at DESC` 순서도 인덱스에 이미 반영되어 있으므로 추가적인 정렬 없이 결과를 빠르게 반환할 수 있었다.

**개선 결과**

- actual rows: 100,000 → 1,000 (약 **99.00% 감소**, 약 **100배 개선**)
- actual time: 53.2ms → 0.352ms (**약 99.34% 감소**, **약 151.1배 개선**)