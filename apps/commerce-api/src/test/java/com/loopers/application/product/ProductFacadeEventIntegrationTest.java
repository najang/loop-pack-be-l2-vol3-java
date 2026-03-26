package com.loopers.application.product;

import com.loopers.domain.event.UserActionEvent;
import com.loopers.domain.product.ProductService;
import com.loopers.domain.product.SellingStatus;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;

import static org.assertj.core.api.Assertions.assertThat;

@RecordApplicationEvents
@SpringBootTest
class ProductFacadeEventIntegrationTest {

    @Autowired
    private ProductFacade productFacade;

    @Autowired
    private ProductService productService;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    @Autowired
    private ApplicationEvents applicationEvents;

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @DisplayName("findById() 시,")
    @Nested
    class FindById {

        @DisplayName("로그인 사용자가 상품을 조회하면, PRODUCT_VIEWED 이벤트가 발행된다.")
        @Test
        void publishesProductViewedEvent_whenUserIsLoggedIn() {
            // arrange
            var product = productService.create(1L, "테스트 상품", "설명", 10000, 10, SellingStatus.SELLING);

            // act
            productFacade.findById(product.getId(), 1L);

            // assert
            long count = applicationEvents.stream(UserActionEvent.class)
                .filter(e -> e.eventType() == UserActionEvent.EventType.PRODUCT_VIEWED
                    && e.userId().equals(1L)
                    && e.targetId().equals(product.getId()))
                .count();
            assertThat(count).isEqualTo(1);
        }

        @DisplayName("비로그인 사용자가 상품을 조회하면, PRODUCT_VIEWED 이벤트가 발행되지 않는다.")
        @Test
        void doesNotPublishEvent_whenUserIsAnonymous() {
            // arrange
            var product = productService.create(1L, "테스트 상품", "설명", 10000, 10, SellingStatus.SELLING);

            // act
            productFacade.findById(product.getId(), null);

            // assert
            long count = applicationEvents.stream(UserActionEvent.class).count();
            assertThat(count).isEqualTo(0);
        }
    }
}
