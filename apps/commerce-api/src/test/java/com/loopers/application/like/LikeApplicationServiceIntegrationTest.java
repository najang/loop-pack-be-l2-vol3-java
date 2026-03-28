package com.loopers.application.like;

import com.loopers.domain.event.LikeEvent;
import com.loopers.domain.event.UserActionEvent;
import com.loopers.domain.like.Like;
import com.loopers.domain.like.LikeRepository;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductService;
import com.loopers.domain.product.SellingStatus;
import com.loopers.infrastructure.eventlog.EventLogJpaRepository;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@RecordApplicationEvents
@SpringBootTest
class LikeApplicationServiceIntegrationTest {

    private static final Long BRAND_ID = 1L;
    private static final Long USER_ID = 1L;

    @Autowired
    private LikeApplicationService likeApplicationService;

    @Autowired
    private ProductService productService;

    @Autowired
    private LikeRepository likeRepository;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    @Autowired
    private ApplicationEvents applicationEvents;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private EventLogJpaRepository eventLogJpaRepository;

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @DisplayName("like() 시,")
    @Nested
    class LikeAction {

        @DisplayName("like 하면 Like 레코드가 저장된다.")
        @Test
        void savesLikeRecord_whenLiked() {
            // arrange
            Product product = productService.create(BRAND_ID, "에어맥스", "Nike Air Max", 100000, 10, SellingStatus.SELLING);

            // act
            likeApplicationService.like(USER_ID, product.getId());

            // assert
            assertThat(likeRepository.findByUserIdAndProductId(USER_ID, product.getId())).isPresent();
        }

        @DisplayName("like 하면 LikeEvent(LIKED)가 발행된다.")
        @Test
        void publishesLikedEvent_whenLiked() {
            // arrange
            Product product = productService.create(BRAND_ID, "에어맥스", "Nike Air Max", 100000, 10, SellingStatus.SELLING);

            // act
            likeApplicationService.like(USER_ID, product.getId());

            // assert
            long count = applicationEvents.stream(LikeEvent.class)
                .filter(e -> e.type() == LikeEvent.Type.LIKED && e.productId().equals(product.getId()))
                .count();
            assertThat(count).isEqualTo(1);
        }

        @DisplayName("like 하면 UserActionEvent(LIKED)가 발행된다.")
        @Test
        void publishesUserActionLikedEvent_whenLiked() {
            // arrange
            Product product = productService.create(BRAND_ID, "에어맥스", "Nike Air Max", 100000, 10, SellingStatus.SELLING);

            // act
            likeApplicationService.like(USER_ID, product.getId());

            // assert
            long count = applicationEvents.stream(UserActionEvent.class)
                .filter(e -> e.eventType() == UserActionEvent.EventType.LIKED
                    && e.userId().equals(USER_ID)
                    && e.targetId().equals(product.getId()))
                .count();
            assertThat(count).isEqualTo(1);
        }

        @DisplayName("동일 사용자가 like를 2번 호출해도 Like 레코드가 1개만 저장된다 (멱등).")
        @Test
        void likeRecordRemainsOne_whenSameUserLikesTwice() {
            // arrange
            Product product = productService.create(BRAND_ID, "에어맥스", "Nike Air Max", 100000, 10, SellingStatus.SELLING);

            // act
            likeApplicationService.like(USER_ID, product.getId());
            likeApplicationService.like(USER_ID, product.getId());

            // assert
            // 두 번째 호출에서는 이미 Like가 있으므로 이벤트도 1번만 발행됨
            long eventCount = applicationEvents.stream(LikeEvent.class)
                .filter(e -> e.type() == LikeEvent.Type.LIKED)
                .count();
            assertThat(eventCount).isEqualTo(1);
        }

        @DisplayName("삭제된 상품에 like 시도하면, NOT_FOUND 예외가 발생한다.")
        @Test
        void throwsNotFound_whenProductIsDeleted() {
            // arrange
            Product product = productService.create(BRAND_ID, "에어맥스", "Nike Air Max", 100000, 10, SellingStatus.SELLING);
            productService.delete(product.getId());

            // act
            CoreException ex = assertThrows(CoreException.class, () -> likeApplicationService.like(USER_ID, product.getId()));

            // assert
            assertThat(ex.getErrorType()).isEqualTo(ErrorType.NOT_FOUND);
        }
    }

    @DisplayName("unlike() 시,")
    @Nested
    class Unlike {

        @DisplayName("like 후 unlike 하면 Like 레코드가 삭제된다.")
        @Test
        void deletesLikeRecord_whenUnliked() {
            // arrange
            Product product = productService.create(BRAND_ID, "에어맥스", "Nike Air Max", 100000, 10, SellingStatus.SELLING);
            likeApplicationService.like(USER_ID, product.getId());

            // act
            likeApplicationService.unlike(USER_ID, product.getId());

            // assert
            assertThat(likeRepository.findByUserIdAndProductId(USER_ID, product.getId())).isEmpty();
        }

        @DisplayName("unlike 하면 UserActionEvent(UNLIKED)가 발행된다.")
        @Test
        void publishesUserActionUnlikedEvent_whenUnliked() {
            // arrange
            Product product = productService.create(BRAND_ID, "에어맥스", "Nike Air Max", 100000, 10, SellingStatus.SELLING);
            likeApplicationService.like(USER_ID, product.getId());

            // act
            likeApplicationService.unlike(USER_ID, product.getId());

            // assert
            long count = applicationEvents.stream(UserActionEvent.class)
                .filter(e -> e.eventType() == UserActionEvent.EventType.UNLIKED
                    && e.userId().equals(USER_ID)
                    && e.targetId().equals(product.getId()))
                .count();
            assertThat(count).isEqualTo(1);
        }

        @DisplayName("unlike 하면 LikeEvent(UNLIKED)가 발행된다.")
        @Test
        void publishesUnlikedEvent_whenUnliked() {
            // arrange
            Product product = productService.create(BRAND_ID, "에어맥스", "Nike Air Max", 100000, 10, SellingStatus.SELLING);
            likeApplicationService.like(USER_ID, product.getId());

            // act
            likeApplicationService.unlike(USER_ID, product.getId());

            // assert
            long count = applicationEvents.stream(LikeEvent.class)
                .filter(e -> e.type() == LikeEvent.Type.UNLIKED && e.productId().equals(product.getId()))
                .count();
            assertThat(count).isEqualTo(1);
        }
    }

    @DisplayName("트랜잭션 롤백 시,")
    @Nested
    class RollbackScenario {

        @DisplayName("like() 트랜잭션이 롤백되면, EventLog가 저장되지 않는다.")
        @Test
        void noEventLogSaved_whenLikeTransactionRollsBack() {
            // arrange
            Product product = productService.create(BRAND_ID, "에어맥스", "Nike Air Max", 100000, 10, SellingStatus.SELLING);

            // act: setRollbackOnly()로 강제 롤백
            transactionTemplate.executeWithoutResult(status -> {
                likeApplicationService.like(USER_ID, product.getId());
                status.setRollbackOnly();
            });

            // assert: 트랜잭션 미커밋 → AFTER_COMMIT 리스너 미발동 → EventLog 없음
            assertThat(eventLogJpaRepository.count()).isZero();
        }

        @DisplayName("unlike() 트랜잭션이 롤백되면, EventLog가 저장되지 않는다.")
        @Test
        void noEventLogSaved_whenUnlikeTransactionRollsBack() {
            // arrange: 이벤트 없이 직접 Like 저장 (likeRepository.save → 이벤트 미발행)
            Product product = productService.create(BRAND_ID, "에어맥스", "Nike Air Max", 100000, 10, SellingStatus.SELLING);
            likeRepository.save(new Like(USER_ID, product.getId()));

            // act: unlike() 트랜잭션 강제 롤백
            transactionTemplate.executeWithoutResult(status -> {
                likeApplicationService.unlike(USER_ID, product.getId());
                status.setRollbackOnly();
            });

            // assert: 커밋 없음 → EventLog 없음
            assertThat(eventLogJpaRepository.count()).isZero();
        }
    }
}
