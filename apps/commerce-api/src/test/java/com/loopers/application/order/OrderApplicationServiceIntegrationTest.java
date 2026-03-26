package com.loopers.application.order;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.brand.BrandService;
import com.loopers.domain.coupon.CouponService;
import com.loopers.domain.coupon.CouponTemplate;
import com.loopers.domain.coupon.CouponType;
import com.loopers.domain.coupon.UserCoupon;
import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderRepository;
import com.loopers.domain.order.OrderStatus;
import com.loopers.domain.outbox.OutboxEvent;
import com.loopers.infrastructure.outbox.OutboxEventJpaRepository;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductService;
import com.loopers.domain.product.SellingStatus;
import com.loopers.domain.user.UserModel;
import com.loopers.infrastructure.user.UserJpaRepository;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
class OrderApplicationServiceIntegrationTest {

    private static final String PRODUCT_NAME = "에어맥스";
    private static final int PRICE = 10000;
    private static final int STOCK = 10;

    private Long brandId;
    private Long userId;
    private Long otherUserId;

    @Autowired
    private OrderApplicationService orderApplicationService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ProductService productService;

    @Autowired
    private BrandService brandService;

    @Autowired
    private CouponService couponService;

    @Autowired
    private UserJpaRepository userJpaRepository;

    @Autowired
    private OutboxEventJpaRepository outboxEventJpaRepository;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    @BeforeEach
    void setUp() {
        Brand brand = brandService.create("Nike", null);
        brandId = brand.getId();

        UserModel user = userJpaRepository.save(new UserModel(
            "user1", "encoded", "홍길동", LocalDate.of(1990, 1, 1), "user1@test.com"
        ));
        userId = user.getId();

        UserModel otherUser = userJpaRepository.save(new UserModel(
            "user2", "encoded", "김철수", LocalDate.of(1991, 2, 2), "user2@test.com"
        ));
        otherUserId = otherUser.getId();
    }

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @DisplayName("주문 생성 시,")
    @Nested
    class Create {

        @DisplayName("쿠폰 없이 정상 주문이면, status=PAYMENT_PENDING, originalTotalPrice 계산, 재고 차감이 확인된다.")
        @Test
        void createsOrder_withCorrectStatusAndTotalPrice() {
            // arrange
            Product product = productService.create(brandId, PRODUCT_NAME, null, PRICE, STOCK, SellingStatus.SELLING);

            // act
            Order order = orderApplicationService.create(userId, product.getId(), 3, null);

            // assert
            assertAll(
                () -> assertThat(order.getUserId()).isEqualTo(userId),
                () -> assertThat(order.getStatus()).isEqualTo(OrderStatus.PAYMENT_PENDING),
                () -> assertThat(order.getOriginalTotalPrice()).isEqualTo(PRICE * 3),
                () -> assertThat(order.getDiscountAmount()).isEqualTo(0),
                () -> assertThat(order.getFinalTotalPrice()).isEqualTo(PRICE * 3),
                () -> assertThat(order.getUserCouponId()).isNull(),
                () -> assertThat(order.getItems()).hasSize(1)
            );

            Product updatedProduct = productService.findById(product.getId());
            assertThat(updatedProduct.getStock()).isEqualTo(STOCK - 3);
        }

        @DisplayName("정상 주문이면, outbox_events에 ORDER_CREATED 레코드가 생성된다.")
        @Test
        void savesOutboxEvent_whenOrderCreated() {
            // arrange
            Product product = productService.create(brandId, PRODUCT_NAME, null, PRICE, STOCK, SellingStatus.SELLING);

            // act
            Order order = orderApplicationService.create(userId, product.getId(), 1, null);

            // assert
            List<OutboxEvent> events = outboxEventJpaRepository.findByPublishedFalseOrderByCreatedAtAsc();
            assertThat(events).hasSize(1);
            assertThat(events.get(0).getTopic()).isEqualTo("order-events");
            assertThat(events.get(0).getPartitionKey()).isEqualTo(String.valueOf(order.getId()));
        }

        @DisplayName("FIXED 쿠폰을 적용하면, discountAmount가 차감되고 finalTotalPrice가 계산된다.")
        @Test
        void createsOrder_withFixedCouponApplied() {
            // arrange
            Product product = productService.create(brandId, PRODUCT_NAME, null, PRICE, STOCK, SellingStatus.SELLING);
            CouponTemplate template = couponService.saveTemplate(
                new CouponTemplate("정액 할인", CouponType.FIXED, 3000, null, ZonedDateTime.now().plusDays(7))
            );
            UserCoupon userCoupon = couponService.issue(userId, template.getId());

            // act
            Order order = orderApplicationService.create(userId, product.getId(), 2, userCoupon.getId());

            // assert
            assertAll(
                () -> assertThat(order.getOriginalTotalPrice()).isEqualTo(20000),
                () -> assertThat(order.getDiscountAmount()).isEqualTo(3000),
                () -> assertThat(order.getFinalTotalPrice()).isEqualTo(17000),
                () -> assertThat(order.getUserCouponId()).isEqualTo(userCoupon.getId())
            );
        }

        @DisplayName("RATE 쿠폰을 적용하면, 정률 할인이 적용된다.")
        @Test
        void createsOrder_withRateCouponApplied() {
            // arrange
            Product product = productService.create(brandId, PRODUCT_NAME, null, PRICE, STOCK, SellingStatus.SELLING);
            CouponTemplate template = couponService.saveTemplate(
                new CouponTemplate("10% 할인", CouponType.RATE, 10, null, ZonedDateTime.now().plusDays(7))
            );
            UserCoupon userCoupon = couponService.issue(userId, template.getId());

            // act
            Order order = orderApplicationService.create(userId, product.getId(), 2, userCoupon.getId());

            // assert
            assertAll(
                () -> assertThat(order.getOriginalTotalPrice()).isEqualTo(20000),
                () -> assertThat(order.getDiscountAmount()).isEqualTo(2000),
                () -> assertThat(order.getFinalTotalPrice()).isEqualTo(18000)
            );
        }

        @DisplayName("이미 사용된 쿠폰으로 주문하면, BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsBadRequest_whenCouponAlreadyUsed() {
            // arrange
            Product product = productService.create(brandId, PRODUCT_NAME, null, PRICE, STOCK, SellingStatus.SELLING);
            CouponTemplate template = couponService.saveTemplate(
                new CouponTemplate("정액 할인", CouponType.FIXED, 1000, null, ZonedDateTime.now().plusDays(7))
            );
            UserCoupon userCoupon = couponService.issue(userId, template.getId());
            orderApplicationService.create(userId, product.getId(), 1, userCoupon.getId());

            // act
            CoreException ex = assertThrows(CoreException.class,
                () -> orderApplicationService.create(userId, product.getId(), 1, userCoupon.getId()));

            // assert
            assertThat(ex.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }

        @DisplayName("타 유저의 쿠폰으로 주문하면, BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsBadRequest_whenCouponBelongsToOtherUser() {
            // arrange
            Product product = productService.create(brandId, PRODUCT_NAME, null, PRICE, STOCK, SellingStatus.SELLING);
            CouponTemplate template = couponService.saveTemplate(
                new CouponTemplate("정액 할인", CouponType.FIXED, 1000, null, ZonedDateTime.now().plusDays(7))
            );
            UserCoupon otherUserCoupon = couponService.issue(otherUserId, template.getId());

            // act
            CoreException ex = assertThrows(CoreException.class,
                () -> orderApplicationService.create(userId, product.getId(), 1, otherUserCoupon.getId()));

            // assert
            assertThat(ex.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }

        @DisplayName("판매 중단(STOP) 상품이면, BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsBadRequest_whenProductIsStop() {
            // arrange
            Product product = productService.create(brandId, PRODUCT_NAME, null, PRICE, STOCK, SellingStatus.STOP);

            // act
            CoreException ex = assertThrows(CoreException.class, () -> orderApplicationService.create(userId, product.getId(), 1, null));

            // assert
            assertThat(ex.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }

        @DisplayName("재고 부족이면, BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsBadRequest_whenStockIsInsufficient() {
            // arrange
            Product product = productService.create(brandId, PRODUCT_NAME, null, PRICE, 2, SellingStatus.SELLING);

            // act
            CoreException ex = assertThrows(CoreException.class, () -> orderApplicationService.create(userId, product.getId(), 5, null));

            // assert
            assertThat(ex.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }
    }

    @DisplayName("주문 단건 조회 시,")
    @Nested
    class FindById {

        @DisplayName("존재하는 주문이면, 정상 조회된다.")
        @Test
        void returnsOrder_whenOrderExists() {
            // arrange
            Product product = productService.create(brandId, PRODUCT_NAME, null, PRICE, STOCK, SellingStatus.SELLING);
            Order created = orderApplicationService.create(userId, product.getId(), 1, null);

            // act
            Order found = orderApplicationService.findById(created.getId());

            // assert
            assertThat(found.getId()).isEqualTo(created.getId());
        }
    }

    @DisplayName("사용자별 주문 목록 조회 시,")
    @Nested
    class FindByUserId {

        @DisplayName("해당 user의 주문만 반환되고, 다른 user의 주문은 제외된다.")
        @Test
        void returnsOnlyUserOrders_excludingOtherUsers() {
            // arrange
            Product product = productService.create(brandId, PRODUCT_NAME, null, PRICE, STOCK, SellingStatus.SELLING);
            orderApplicationService.create(userId, product.getId(), 1, null);
            orderApplicationService.create(userId, product.getId(), 1, null);
            orderApplicationService.create(otherUserId, product.getId(), 1, null);

            // act
            List<Order> result = orderApplicationService.findByUserId(userId);

            // assert
            assertThat(result).hasSize(2);
            assertThat(result).allMatch(o -> o.getUserId().equals(userId));
        }
    }

    @DisplayName("주문 취소 시,")
    @Nested
    class Cancel {

        @DisplayName("ORDERED 상태에서 취소하면, 상태가 CANCELLED로 변경되고 재고가 복구된다.")
        @Test
        void cancelsOrder_andRestoresStock() {
            // arrange
            Product product = productService.create(brandId, PRODUCT_NAME, null, PRICE, STOCK, SellingStatus.SELLING);
            Order order = orderApplicationService.create(userId, product.getId(), 3, null);
            order.confirmPayment();
            orderRepository.save(order);

            // act
            orderApplicationService.cancel(order.getId());

            // assert
            Order cancelled = orderApplicationService.findById(order.getId());
            assertThat(cancelled.getStatus()).isEqualTo(OrderStatus.CANCELLED);

            Product restored = productService.findById(product.getId());
            assertThat(restored.getStock()).isEqualTo(STOCK);
        }

        @DisplayName("DELIVERED 상태인 주문을 취소하면, BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsBadRequest_whenCancellingDeliveredOrder() {
            // arrange
            Product product = productService.create(brandId, PRODUCT_NAME, null, PRICE, STOCK, SellingStatus.SELLING);
            Order order = orderApplicationService.create(userId, product.getId(), 1, null);
            order.changeStatus(OrderStatus.DELIVERED);
            orderRepository.save(order);

            // act
            CoreException ex = assertThrows(CoreException.class, () -> orderApplicationService.cancel(order.getId()));

            // assert
            assertThat(ex.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }
    }
}
