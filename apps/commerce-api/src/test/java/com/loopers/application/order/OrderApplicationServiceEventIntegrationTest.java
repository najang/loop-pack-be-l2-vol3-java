package com.loopers.application.order;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.brand.BrandService;
import com.loopers.domain.event.UserActionEvent;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductService;
import com.loopers.domain.product.SellingStatus;
import com.loopers.domain.user.UserModel;
import com.loopers.infrastructure.user.UserJpaRepository;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@RecordApplicationEvents
@SpringBootTest
class OrderApplicationServiceEventIntegrationTest {

    @Autowired
    private OrderApplicationService orderApplicationService;

    @Autowired
    private ProductService productService;

    @Autowired
    private BrandService brandService;

    @Autowired
    private UserJpaRepository userJpaRepository;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    @Autowired
    private ApplicationEvents applicationEvents;

    private Long userId;
    private Long brandId;

    @BeforeEach
    void setUp() {
        Brand brand = brandService.create("Nike", null);
        brandId = brand.getId();
        UserModel user = userJpaRepository.save(new UserModel(
            "user1", "encoded", "홍길동", LocalDate.of(1990, 1, 1), "user1@test.com"
        ));
        userId = user.getId();
    }

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @DisplayName("주문 생성 시, ORDER_CREATED UserActionEvent가 발행된다.")
    @Test
    void publishesOrderCreatedEvent_whenOrderIsCreated() {
        // arrange
        Product product = productService.create(brandId, "에어맥스", "Nike Air Max", 10000, 10, SellingStatus.SELLING);

        // act
        var order = orderApplicationService.create(userId, product.getId(), 1, null);

        // assert
        long count = applicationEvents.stream(UserActionEvent.class)
            .filter(e -> e.eventType() == UserActionEvent.EventType.ORDER_CREATED
                && e.userId().equals(userId)
                && e.targetId().equals(order.getId()))
            .count();
        assertThat(count).isEqualTo(1);
    }
}
