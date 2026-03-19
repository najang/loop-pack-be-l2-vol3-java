# CLAUDE.md

## 프로젝트 개요

Loopers 커머스 백엔드 멀티 모듈 프로젝트 (Java + Spring Boot)

## 기술 스택 및 버전

| 구분 | 기술 | 버전 |
|------|------|------|
| Language | Java | 21 |
| Build | Gradle (Kotlin DSL) | 8.13 |
| Framework | Spring Boot | 3.4.4 |
| Dependency Mgmt | Spring Dependency Management | 1.1.7 |
| Cloud | Spring Cloud Dependencies | 2024.0.1 |
| DB | MySQL | 8.0 |
| ORM | Spring Data JPA + QueryDSL (Jakarta) | - |
| Cache | Redis (Master/Replica) | 7.0 |
| Messaging | Kafka (KRaft mode) | 3.5.1 |
| API 문서 | SpringDoc OpenAPI | 2.7.0 |
| 보안 | Spring Security Crypto | - |
| 모니터링 | Micrometer + Prometheus + Grafana | - |
| 테스트 | JUnit 5, Mockito 5.14.0, Testcontainers | - |

## 모듈 구조

```
Root (loopers-java-spring-template)
├── apps (실행 가능한 Spring Boot 애플리케이션)
│   ├── commerce-api       — REST API 서버 (Web, Swagger, Security Crypto)
│   ├── commerce-batch     — 배치 처리 (Spring Batch)
│   └── commerce-streamer  — 스트리밍 처리 (Kafka Consumer)
├── modules (재사용 가능한 인프라 설정)
│   ├── jpa                — JPA + QueryDSL + MySQL + HikariCP
│   ├── redis              — Redis Master/Replica 구성
│   └── kafka              — Kafka Producer/Consumer 구성
└── supports (부가 기능 add-on)
    ├── jackson            — Jackson 직렬화 설정
    ├── logging            — Logback + Slack Appender
    └── monitoring         — Actuator + Prometheus
```

## 패키지 구조 (commerce-api 기준)

Layered Architecture를 따른다.

```
com.loopers/
├── interfaces/api/     — Controller, DTO, ApiSpec (presentation)
├── application/        — Facade, Info (use case orchestration)
├── domain/             — Model, Service, Repository, Policy (business logic)
├── infrastructure/     — JpaRepository, RepositoryImpl, Encoder 구현체
└── support/            — auth(인터셉터, ArgumentResolver), error(CoreException, ErrorType)
```

## 빌드 및 실행

```bash
# 인프라 실행 (MySQL, Redis, Kafka)
docker-compose -f ./docker/infra-compose.yml up

# 모니터링 실행 (Prometheus, Grafana)
docker-compose -f ./docker/monitoring-compose.yml up

# 전체 빌드
./gradlew build

# 특정 모듈 테스트
./gradlew :apps:commerce-api:test

# 특정 테스트 클래스 실행
./gradlew :apps:commerce-api:test --tests "com.loopers.interfaces.api.UserV1ApiE2ETest"
```

## 테스트 환경

- 프로필: `test` (자동 적용 - `systemProperty("spring.profiles.active", "test")`)
- 타임존: `Asia/Seoul`
- DB: Testcontainers (MySQL) — 테스트 시 자동으로 컨테이너 생성
- DDL: `hibernate.ddl-auto=create` (테스트 시 스키마 자동 생성)
- 커버리지: JaCoCo (XML 리포트)

## 커밋 컨벤션

Conventional Commits + 한글 메시지

```
<type>: <한글 설명>
```

| type | 용도 |
|------|------|
| `feat` | 새로운 기능 구현 |
| `test` | 테스트 코드 추가/수정 |
| `refactor` | 기능 변경 없는 코드 리팩토링 |
| `fix` | 버그 수정 |
| `docs` | 문서 수정 |
| `chore` | 빌드, 의존성, 설정 변경 |

예시:
```
feat: 비밀번호 변경 기능 구현
test: 비밀번호 변경 테스트 코드 추가
refactor: RESTful 컨벤션에 맞게 API URL 변경
```

## 코드 컨벤션

- 최대 줄 길이: 130자 (테스트 파일은 제한 없음, `.editorconfig` 참고)
- 파일 끝에 개행 삽입
- Lombok 사용 (`@RequiredArgsConstructor`, `@Getter` 등)
- DTO: Java Record 사용, Bean Validation 어노테이션으로 입력 검증 (`@NotBlank`, `@Size`, `@Email`)
- 도메인 모델에서 비즈니스 규칙 검증, Web 레이어(DTO)에서 입력 형식 검증
- API 응답: `ApiResponse<T>` 래핑
- 예외: `CoreException(ErrorType, message)` 사용

## 프로필

`local` | `test` | `dev` | `qa` | `prd`

- `prd` 환경에서는 Swagger UI 비활성화
- `test` 환경은 Testcontainers 기반으로 외부 인프라 불필요

## 개발 규칙
### 진행 Workflow - 증강 코딩
- **대원칙** : 방향성 및 주요 의사 결정은 개발자에게 제안만 할 수 있으며, 최종 승인된 사항을 기반으로 작업을 수행.
- **중간 결과 보고** : AI 가 반복적인 동작을 하거나, 요청하지 않은 기능을 구현, 테스트 삭제를 임의로 진행할 경우 개발자가 개입.
- **설계 주도권 유지** : AI 가 임의판단을 하지 않고, 방향성에 대한 제안 등을 진행할 수 있으나 개발자의 승인을 받은 후 수행.

### 개발 Workflow - TDD (Red > Green > Refactor)
- 모든 테스트는 3A 원칙으로 작성할 것 (Arrange - Act - Assert)
#### 1. Red Phase : 실패하는 테스트 먼저 작성
- 요구사항을 만족하는 기능 테스트 케이스 작성
- 테스트 예시
#### 2. Green Phase : 테스트를 통과하는 코드 작성
- Red Phase 의 테스트가 모두 통과할 수 있는 코드 작성
- 오버엔지니어링 금지
#### 3. Refactor Phase : 불필요한 코드 제거 및 품질 개선
- 불필요한 private 함수 지양, 객체지향적 코드 작성
- unused import 제거
- 성능 최적화
- 모든 테스트 케이스가 통과해야 함

## 주의사항
### 1. Never Do
- 실제 동작하지 않는 코드, 불필요한 Mock 데이터를 이요한 구현을 하지 말 것
- null-safety 하지 않게 코드 작성하지 말 것 (Java 의 경우, Optional 을 활용할 것)
- println 코드 남기지 말 것

### 2. Recommendation
- 실제 API 를 호출해 확인하는 E2E 테스트 코드 작성
- 재사용 가능한 객체 설계
- 성능 최적화에 대한 대안 및 제안
- 개발 완료된 API 의 경우, `.http/**.http` 에 분류해 작성

### 3. Priority
1. 실제 동작하는 해결책만 고려
2. null-safety, thread-safety 고려
3. 테스트 가능한 구조로 설계
4. 기존 코드 패턴 분석 후 일관성 유지