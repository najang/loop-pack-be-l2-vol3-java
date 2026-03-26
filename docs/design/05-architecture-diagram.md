# 레이어 아키텍처 개요

HTTP Request

    ↓

| interfaces/api (Presentation Layer) |
| --- |
|  Controller · DTO · ApiSpec  |
| application  (Use Case Orchestration) |
| Facade · Info |
| domain  (Business Logic Layer) |
| Model · Service · Repository · Policy |
| infrastructure  (Persistence Layer) |
| JpaRepository · RepositoryImpl · Encoder · PgGateway · PgSimulatorClient · CallbackSignatureValidator |
| support  (Cross-Cutting Concerns) |
| auth · error |

    ↓

DATABASE (MySQL) / PG Simulator (HTTP)

## 1. interfaces/api (Presentation Layer)

**책임**

- HTTP 요청/응답 처리
- Bean Validation을 통한 입력 형식 검증
- API 문서(Swagger) 정의
- 통일된 응답 래핑 (ApiResponse<T>)

**공통 클래스**

| 클래스 | 책임 |
| --- | --- |
| ApiResponse<T> | 모든 API 응답을 { metadata, data } 형태로 래핑하는 Generic Record |
| ApiControllerAdvice | CoreException을 잡아 ApiResponse.fail()로 변환하는 전역 예외 핸들러 |

**도메인별 클래스 (패턴: {Domain}V1Controller / {Domain}V1Dto / {Domain}V1ApiSpec)**

| 도메인 | Controller | 주요 엔드포인트                                                                                                                                   |
| --- | --- |--------------------------------------------------------------------------------------------------------------------------------------------|
| User | UserV1Controller | POST /api/v1/users, GET /api/v1/users/me, PATCH /api/v1/users/password                                 |
| Brand | BrandV1Controller | GET /api/v1/brands/{brandId}                                                                                                               |
| Product | ProductV1Controller | GET /api/v1/products,                                                   GET /api/v1/products/{productId}                                   |
| Like | LikeV1Controller | POST/DELETE /api/v1/products/{productId}/likes,                                                           GET /api/v1/users/{userId}/likes |
| Cart | CartV1Controller | POST /api/v1/cart/items, GET /api/v1/cart, PUT/DELETE /api/v1/cart/items/{productId}                                                       |
| Order | OrderV1Controller | POST /api/v1/orders, GET /api/v1/orders, GET /api/v1/orders/{orderId}, PATCH /api/v1/orders/{orderId}/cancel                               |
| Payment Callback | PaymentCallbackV1Controller | POST /api/v1/payments/{paymentId}/callback (X-PG-Signature 서명 검증 후 콜백 처리)                                                            |
| Admin Brand | AdminBrandV1Controller | /api-admin/v1/brands CRUD                                                                                                                  |
| Admin Product | AdminProductV1Controller | /api-admin/v1/products CRUD                                                                                                                |
| Admin Order | AdminOrderV1Controller | /api-admin/v1/orders 조회/상태변경                                                                                                               |

DTO 설계 원칙: Java Record 사용, Bean Validation 어노테이션으로 입력 검증, from(Info) 팩토리 메서드로 응답 변환

## 2. application (Use Case Orchestration Layer)

**책임**

- 2개 이상 도메인 서비스를 조율하는 유스케이스 실행
- domain → interfaces 간 데이터 변환 (Model → Info)
- 트랜잭션 경계 관리 (특히 cross-domain 호출 시)

**UseCase(Facade)가 필요한 경우 vs 불필요한 경우**

- Facade 필요 (2개 이상 도메인 조율)


    | Facade | 조율 대상 도메인 | 유스케이스 |
    | --- | --- | --- |
    | ProductFacade | 상품 + 좋아요 | 상품 목록 조회 (로그인 시 isLiked 포함) |
    | ProductFacade | 상품 + 브랜드 + 좋아요 | 상품 상세 조회 (브랜드 정보 + isLiked) |
    | LikeFacade | 좋아요 + 상품 | 좋아요 등록/취소 (likeCount 갱신) |
    | LikeFacade  | 좋아요 + 상품 | 내 좋아요 목록 조회 (삭제 상품 필터링) |
    | CartFacade | 장바구니 + 상품 | 장바구니 담기 (상품 존재 확인) |
    | CartFacade | 장바구니 + 상품 + 브랜드 | 장바구니 조회 (상품·브랜드 정보 조합) |
    | OrderFacade | 주문 + 상품 + 장바구니 | 주문 생성 (재고 차감 + 장바구니 정리) |
    | OrderFacade | 주문 + 상품 | 주문 취소 (상태 변경 + 재고 복원) |
    | PaymentFacade | 주문 + 결제 | 주문+결제 생성 (단일 트랜잭션 + PG 요청), 콜백 처리 (결제 상태 + 주문 상태 전이) |
    | AdminBrandFacade | 브랜드 + 상품 | 브랜드 + 상품 |
    | AdminProductFacade | 상품 + 브랜드 | 상품 등록 (브랜드 존재 확인) |
- Facade 불필요 (단일 서비스 호출 → Controller에서 직접 Service 호출)


    | 유스케이스 | 호출 서비스 |
    | --- | --- |
    | 브랜드 상세 조회 | BrandService |
    | 장바구니 수량 변경 | CartService |
    | 장바구니 삭제 | CartService |
    | 주문 목록 조회 | OrderService |
    | 주문 상세 조회 | OrderService |
    | 어드민 브랜드 목록/등록/수정 | BrandService |
    | 어드민 상품 목록/수정/삭제 | ProductService |
    | 어드민 주문 관리 | OrderService |

**Info 클래스**

| 클래스 | 역할 |
| --- | --- |
| UserInfo | UserModel → 응답 변환 (maskedName 포함) |
| ProductInfo | 상품 + 브랜드 + isLiked 조합 결과 |
| CartInfo | 장바구니 + 상품 + 브랜드 조합 결과 |
| OrderInfo | 주문 + 주문아이템(스냅샷) 결과 |
| PaymentInfo | Payment 도메인 → 응답 변환 |

**트랜잭션 경계 (PaymentFacade)**

| 단계 | 트랜잭션 여부 | 이유 |
| --- | --- | --- |
| Order + Payment 저장 | 트랜잭션 내 (TransactionTemplate) | 원자성 보장 — 하나라도 실패 시 전체 롤백 |
| PG 요청 (PgGateway) | 트랜잭션 외부 | PG 응답 지연 시 DB 커넥션 점유 방지 |
| 콜백 처리 (handleCallback) | 트랜잭션 내 (@Transactional) | Payment 상태 + Order 상태 변경 원자성 보장 |

## 3. domain (Business Logic Layer)

**책임**

- 핵심 비즈니스 규칙 검증 및 실행
- 도메인 모델(Entity)이 자신의 상태를 관리
- Repository 인터페이스 정의 (구현은 infrastructure에 위임)
- Policy 클래스로 복잡한 비즈니스 규칙 분리

### **3-1. Domain Model (Entity)**

모든 엔티티는 BaseEntity를 상속 (예외: Like)

| 모델 | 주요 필드 | 비즈니스 메서드                                                                                 | 비고 |
| --- | --- |------------------------------------------------------------------------------------------| --- |
| UserModel | loginId, password, name, birthDate, email | getMaskedName(),                                                                         
changePassword() |  |
| Brand | name, description | changeBrandInfo(name, desc)                                                              |  |
| Product | brandId, name, description, price, stock, sellingStatus, likeCount | decreaseStock(), restoreStock(), increaseLikeCount(), decreaseLikeCount(), isOrderable() | sellingStatus + stock 복합 판단 |
| Like | userId(PK), productId(PK), createdAt, updatedAt |                                                                                          | BaseEntity 미상속 (복합 키). 물리 삭제 |
| CartItem | userId, productId, quantity |                                                                                          | 수량 1 이상 검증 |
| Order | userId, status, totalPrice, orderedAt, items | addQuantity(qty),                                                                        
changeQuantity(qty) | Aggregate Root |
| OrderItem | orderId, productId, quantity, orderPrice, productName, brandName | cancelOrder(), updateStatus(newStatus), getTotalPrice()                                  | Order의 하위 엔티티. 스냅샷 저장 |

**Enum**

| Enum | 값 | 용도 |
| --- | --- | --- |
| SellingStatus | SELLING, STOP, SOLD_OUT | 상품 판매 상태 |
| OrderStatus | ORDERED, SHIPPING, DELIVERED, CANCELLED | 주문 상태 전이 (ORDERED→SHIPPING, ORDERED→CANCELLED, SHIPPING→DELIVERED) |

**Aggregate Boundary**

- Order (Root) + OrderItem — OrderItem은 Order를 통해서만 생명주기 관리
- 나머지(User, Brand, Product, CartItem, Like)는 각각 독립 Aggregate Root

### 3-2. Domain Service

| 서비스 | 주요 책임 |
| --- | --- |
| UserService | 회원가입, 인증, 비밀번호 변경 |
| BrandService | 브랜드 CRUD, 활성 브랜드 조회 |
| ProductService | 상품 CRUD, 목록 조회(필터/정렬/페이징), 비관적 락 기반 재고 관리 |
| LikeService | 좋아요 등록/삭제(멱등), 사용자별 좋아요 목록 조회 |
| CartService | 장바구니 아이템 CRUD, 동일 상품 수량 누적, 주문 후 삭제 |
| OrderService | 주문 생성(재고 차감 + 스냅샷), 목록/상세 조회, 취소/상태 변경 |

### 3-3. Domain Repository (Interface)

| Repository | 주요 메서드                                                                                      |
| --- |---------------------------------------------------------------------------------------------|
| UserRepository | findById(), findByLoginId(), existsByLoginId(), save()                                      |
| BrandRepository | findById(), findAll(pageable), save(), delete()                                             |
| ProductRepository | findById(),  findByIdWithLock(), findAll(filter, sort, pageable), save(), deleteByBrandId() |
| LikeRepository | findByUserIdAndProductId(), findByUserId(), save(), delete()                                |
| CartRepository | findByUserIdAndProductId(), findByUserId(), save(), delete(), deleteByProductIds()          |
| OrderRepository | findById(), findByUserIdAndPeriod(), findAllByPeriod(), save()                              |

### 3-4. Domain Policy

| Policy | 책임 |
| --- | --- |
| PasswordPolicy | 비밀번호 길이/형식/생년월일 포함 여부 검증 (기존) |

## 4. infrastructure (Persistence Layer)

**책임**

- 도메인 Repository 인터페이스의 구현
- Spring Data JPA / QueryDSL을 사용한 실제 DB 접근
- 외부 기술 의존성 구현체 (PasswordEncoder 등)

**패턴: RepositoryImpl → JpaRepository 위임 (Adapter)**

| 구현체 | 구현 대상 인터페이스 | 사용 기술 |
| --- | --- | --- |
| UserRepositoryImpl | UserRepository | UserJpaRepository (기존) |
| BCryptPasswordEncoderImpl | PasswordEncoder | Spring Security BCrypt (기존) |
| BrandRepositoryImpl | BrandRepository | BrandJpaRepository |
| ProductRepositoryImpl | ProductRepository | ProductJpaRepository + QueryDSL (필터/정렬) |
| LikeRepositoryImpl | LikeRepository | LikeJpaRepository |
| CartRepositoryImpl | CartRepository | CartJpaRepository |
| OrderRepositoryImpl | OrderRepository | OrderJpaRepository + QueryDSL (기간 조회) |
| PaymentRepositoryImpl | PaymentRepository | PaymentJpaRepository |

**PG 연동 인프라 (infrastructure/pg)**

| 클래스 | 책임 |
| --- | --- |
| PgGateway | Resilience4j 래퍼 — CircuitBreaker + Retry + Bulkhead 적용. PG 장애 격리 및 fallback 처리 |
| PgSimulatorClient | OpenFeign HTTP 클라이언트 — PG 시뮬레이터(POST /pg/v1/payments) 호출 |
| CallbackSignatureValidator | HMAC-SHA256 서명 검증 — 위변조된 PG 콜백 차단 |
| PgPaymentRequest | PG 결제 요청 DTO (paymentId, orderId, cardType, cardNo, amount, callbackUrl) |
| PgPaymentResponse | PG 결제 응답 DTO (pgTransactionId) |
| PgCallbackRequest | PG 콜백 요청 DTO (paymentId, orderId, status, failureReason) |

## 5. support (Cross-cutting concerns)

**책임**

- 인증/인가 처리 (인터셉터, ArgumentResolver)
- 전역 예외 처리 및 에러 타입 정의

### 5-1. auth

| 클래스 | 책임 |
| --- | --- |
| @LoginUser | 컨트롤러 파라미터에 인증 사용자 주입을 위한 커스텀 어노테이션 |
| AuthInterceptor | 사용자 API 인증 검증 (세션/쿠키 기반) |
| AuthenticatedUserArgumentResolver | @LoginUser 파라미터를 UserModel로 리졸빙 |
| AuthenticationConfig | 인터셉터 경로 매핑, ArgumentResolver 등록 |

어드민 인증: X-Loopers-Ldap: loopers.admin 헤더로 식별 (사용자 인증과 별도 체계)

### 5-2. error

| 클래스 | 책임 |
| --- | --- |
| CoreException | 비즈니스 예외. ErrorType + 커스텀 메시지를 담는 RuntimeException |
| ErrorType (Enum) | INTERNAL_ERROR(500), BAD_REQUEST(400), UNAUTHORIZED(401), NOT_FOUND(404), CONFLICT(409) |

**Cross-Domain 의존성 (UseCase 레벨)**

시퀀스 다이어그램에서 식별된 도메인 간 의존 관계:

좋아요 UseCase    ──→ Product.좋아요수증가/감소()     (likeCount 비정규화 갱신)
주문 UseCase       ──→ Product.재고차감()                    (비관적 락, productId 오름차순)
주문 취소                ──→ Product.재고복원()                    (주문 생성의 역연산)
브랜드 삭제             ──→ Product.삭제()                           (연쇄 soft delete)
주문 생성                ──→ CartItem 삭제                             (별도 트랜잭션, 실패해도 주문 유효)
PaymentFacade         ──→ OrderApplicationService.create()     (단일 트랜잭션 내, PENDING 주문 생성)
                      ──→ PaymentApplicationService.create()   (동일 트랜잭션, PENDING 결제 생성)
                      ──→ PgGateway.requestPayment()           (트랜잭션 외부, Resilience4j 적용)
콜백 성공               ──→ OrderApplicationService.confirmPayment() (Order → PAID)
콜백 실패               ──→ OrderApplicationService.failPayment()    (재고 복원 + 쿠폰 복원 + Order → PAYMENT_FAILED)

## 6. PG 시뮬레이터 (apps/pg-simulator)

독립된 Spring Boot 애플리케이션으로 실제 PG사 역할을 대체한다.

| 항목 | 내용 |
| --- | --- |
| 역할 | PG 결제 요청 수신 → 처리 후 commerce-api에 콜백 전송 |
| 통신 방식 | commerce-api → PG Simulator: Feign HTTP, PG Simulator → commerce-api: HTTP 콜백 |
| 콜백 서명 | HMAC-SHA256 서명을 X-PG-Signature 헤더에 포함 |
| 장애 시뮬레이션 | 결제 실패 케이스 재현 가능 (failureReason 포함 콜백) |