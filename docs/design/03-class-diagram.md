# 도메인 객체 클래스 다이어그램

```mermaid
classDiagram
  %% ─── BaseEntity (공통 상위 클래스) ───
  %% modules/jpa BaseEntity 기반
  %% id, createdAt, updatedAt, deletedAt 을 제공하며
  %% delete()/restore() 멱등 연산 포함

  class BaseEntity {
    <<abstract>>
    #Long id
    #ZonedDateTime createdAt
    #ZonedDateTime updatedAt
    #ZonedDateTime deletedAt
    +delete()
    +restore()
  }

  %% ─── Domain Entities / Enums ───

  class User {
    -String loginId
    -String password
    -String name
    -LocalDate birthDate
    -String email
    -UserRole role
  }

  class UserRole {
    <<enumeration>>
    USER
    ADMIN
  }

  class Brand {
    -String name
    -String description
    +브랜드정보변경(name, description)
  }

  class Product {
    -Long brandId
    -String name
    -String description
    -int price
    -int stock
    -SellingStatus sellingStatus
    -int likeCount
    +재고차감(quantity)
    +재고복원(quantity)
    +좋아요수증가()
    +좋아요수감소()
    +주문가능여부() boolean
  }

  class SellingStatus {
    <<enumeration>>
    SELLING
    STOP
    SOLD_OUT
  }

  %% Like는 복합 키(userId + productId)를 사용하므로 BaseEntity 상속 불가
  %% @IdClass 또는 @EmbeddedId 방식으로 별도 구현
  class Like {
    -Long userId  "복합 키 구성 요소"
    -Long productId "복합 키 구성 요소"
    -ZonedDateTime createdAt
    -ZonedDateTime updatedAt
  }

  class CartItem {
    -Long userId
    -Long productId
    -int quantity
    +수량추가(quantity)
    +수량변경(quantity)
  }

  class Order {
    -Long userId
    -OrderStatus status
    -int totalPrice
    -ZonedDateTime orderedAt
    -List~OrderItem~ items
    +주문취소()
    +상태변경(newStatus)
    +총가격계산() int
  }

  class OrderStatus {
    <<enumeration>>
    ORDERED
    SHIPPING
    DELIVERED
    CANCELLED
  }

  class OrderItem {
    -Long orderId
    -Long productId  "참조용 식별자"
    -int quantity
    -int orderPrice
    %% Snapshot (주문 시점 고정 정보)
    -String productName
    -String brandName
  }

  %% ─── Inheritance (BaseEntity) ───
  BaseEntity <|-- User
  BaseEntity <|-- Brand
  BaseEntity <|-- Product
  BaseEntity <|-- CartItem
  BaseEntity <|-- Order
  BaseEntity <|-- OrderItem

  %% ─── Enum Relationships ───
  User --> UserRole
  Product --> SellingStatus
  Order --> OrderStatus

  %% ─── Aggregate Boundary ───
  %% Order가 Aggregate Root, OrderItem은 Order를 통해서만 생명주기 관리
  Order "1" *-- "*" OrderItem : 구성(애그리게잇)

  %% ─── Reference by ID (loose coupling) ───
  Product ..> Brand : brandId로 참조
  Like ..> User : userId 참조
  Like ..> Product : productId 참조
  CartItem ..> User : userId 참조
  CartItem ..> Product : productId 참조
  Order ..> User : userId 참조

  %% ─── Cross-domain 서비스 의존 (UseCase 레벨) ───
  %% 좋아요 UseCase → Product.좋아요수증가/감소() 호출
  %% 주문 UseCase → Product.재고차감/복원() 호출 (비관적 락, productId 오름차순)
  %% 브랜드 삭제 UseCase → Product.삭제() 연쇄 호출
  %% 주문 생성 UseCase → CartItem 삭제 (별도 트랜잭션, 실패해도 주문 유효)
```
---

## 설계 포인트

### 1. BaseEntity 상속 구조
- `BaseEntity`가 `id`, `createdAt`, `updatedAt`, `deletedAt`, `delete()`, `restore()`를 제공
- Brand, Product, CartItem, Order, OrderItem, User는 BaseEntity를 상속
- **예외**: `Like`는 복합 키(`userId` + `productId`)를 사용하므로 BaseEntity 상속 불가 → `@IdClass` 또는 `@EmbeddedId` 별도 구현

### 2. OrderStatus 상태 전이 규칙
- `ORDERED → SHIPPING` / `ORDERED → CANCELLED` / `SHIPPING → DELIVERED`만 허용
- `DELIVERED`, `CANCELLED`에서는 변경 불가
- `상태변경(newStatus)` 메서드 내에서 전이 가능 여부 검증

### 3. Product.주문가능여부() 판단 기준
- `sellingStatus == SELLING` **AND** `stock > 0` 복합 조건
- 두 조건 중 하나라도 불충족 시 주문 불가

### 5. Cross-domain 의존성 (UseCase 레벨)
| UseCase | 호출 대상 | 비고 |
|---------|-----------|------|
| 좋아요 등록/취소 | Product.좋아요수증가/감소() | likeCount 비정규화 갱신 |
| 주문 생성 | Product.재고차감() | 비관적 락, productId 오름차순 데드락 방지 |
| 주문 취소 | Product.재고복원() | 주문 생성의 역연산 |
| 브랜드 삭제 | Product.삭제() | 연쇄 soft delete |
| 주문 생성 | CartItem 삭제 | 별도 트랜잭션 (실패해도 주문 유효) |

### 6. Aggregate Boundary
- **Order Aggregate**: Order(Root) + OrderItem → OrderItem은 독립 Repository 없이 Order를 통해서만 접근
- 나머지 엔티티(User, Brand, Product, CartItem, Like)는 각각 독립 Aggregate Root

### 7. Order.totalPrice 계산
- `totalPrice = Σ(OrderItem.quantity × OrderItem.orderPrice)`
- 주문 생성 시 계산하여 저장 (조회 성능 최적화)