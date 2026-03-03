package com.loopers.domain.order;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.brand.BrandService;
import com.loopers.domain.coupon.CouponService;
import com.loopers.domain.coupon.CouponTemplate;
import com.loopers.domain.coupon.CouponType;
import com.loopers.domain.coupon.UserCoupon;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductService;
import com.loopers.domain.product.SellingStatus;
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

import java.time.ZonedDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
class OrderServiceIntegrationTest {

    private static final Long USER_ID = 1L;
    private static final Long OTHER_USER_ID = 2L;
    private static final String PRODUCT_NAME = "에어맥스";
    private static final int PRICE = 10000;
    private static final int STOCK = 10;

    private Long brandId;

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ProductService productService;

    @Autowired
    private BrandService brandService;

    @Autowired
    private CouponService couponService;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    @BeforeEach
    void setUp() {
        Brand brand = brandService.create("Nike", null);
        brandId = brand.getId();
    }

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @DisplayName("주문 생성 시,")
    @Nested
    class Create {

        @DisplayName("쿠폰 없이 정상 주문이면, status=ORDERED, originalTotalPrice 계산, 재고 차감이 확인된다.")
        @Test
        void createsOrder_withCorrectStatusAndTotalPrice() {
            // arrange
            Product product = productService.create(brandId, PRODUCT_NAME, null, PRICE, STOCK, SellingStatus.SELLING);

            // act
            Order order = orderService.create(USER_ID, product.getId(), 3, null);

            // assert
            assertAll(
                () -> assertThat(order.getUserId()).isEqualTo(USER_ID),
                () -> assertThat(order.getStatus()).isEqualTo(OrderStatus.ORDERED),
                () -> assertThat(order.getOriginalTotalPrice()).isEqualTo(PRICE * 3),
                () -> assertThat(order.getDiscountAmount()).isEqualTo(0),
                () -> assertThat(order.getFinalTotalPrice()).isEqualTo(PRICE * 3),
                () -> assertThat(order.getUserCouponId()).isNull(),
                () -> assertThat(order.getItems()).hasSize(1)
            );

            Product updatedProduct = productService.findById(product.getId());
            assertThat(updatedProduct.getStock()).isEqualTo(STOCK - 3);
        }

        @DisplayName("FIXED 쿠폰을 적용하면, discountAmount가 차감되고 finalTotalPrice가 계산된다.")
        @Test
        void createsOrder_withFixedCouponApplied() {
            // arrange
            Product product = productService.create(brandId, PRODUCT_NAME, null, PRICE, STOCK, SellingStatus.SELLING);
            CouponTemplate template = couponService.saveTemplate(
                new CouponTemplate("정액 할인", CouponType.FIXED, 3000, null, ZonedDateTime.now().plusDays(7))
            );
            UserCoupon userCoupon = couponService.issue(USER_ID, template.getId());

            // act
            Order order = orderService.create(USER_ID, product.getId(), 2, userCoupon.getId());

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
            UserCoupon userCoupon = couponService.issue(USER_ID, template.getId());

            // act
            Order order = orderService.create(USER_ID, product.getId(), 2, userCoupon.getId());

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
            UserCoupon userCoupon = couponService.issue(USER_ID, template.getId());
            orderService.create(USER_ID, product.getId(), 1, userCoupon.getId());

            // act
            CoreException ex = assertThrows(CoreException.class,
                () -> orderService.create(USER_ID, product.getId(), 1, userCoupon.getId()));

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
            UserCoupon otherUserCoupon = couponService.issue(OTHER_USER_ID, template.getId());

            // act
            CoreException ex = assertThrows(CoreException.class,
                () -> orderService.create(USER_ID, product.getId(), 1, otherUserCoupon.getId()));

            // assert
            assertThat(ex.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }

        @DisplayName("판매 중단(STOP) 상품이면, BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsBadRequest_whenProductIsStop() {
            // arrange
            Product product = productService.create(brandId, PRODUCT_NAME, null, PRICE, STOCK, SellingStatus.STOP);

            // act
            CoreException ex = assertThrows(CoreException.class, () -> orderService.create(USER_ID, product.getId(), 1, null));

            // assert
            assertThat(ex.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }

        @DisplayName("재고 부족이면, BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsBadRequest_whenStockIsInsufficient() {
            // arrange
            Product product = productService.create(brandId, PRODUCT_NAME, null, PRICE, 2, SellingStatus.SELLING);

            // act
            CoreException ex = assertThrows(CoreException.class, () -> orderService.create(USER_ID, product.getId(), 5, null));

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
            Order created = orderService.create(USER_ID, product.getId(), 1, null);

            // act
            Order found = orderService.findById(created.getId());

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
            orderService.create(USER_ID, product.getId(), 1, null);
            orderService.create(USER_ID, product.getId(), 1, null);
            orderService.create(OTHER_USER_ID, product.getId(), 1, null);

            // act
            List<Order> result = orderService.findByUserId(USER_ID);

            // assert
            assertThat(result).hasSize(2);
            assertThat(result).allMatch(o -> o.getUserId().equals(USER_ID));
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
            Order order = orderService.create(USER_ID, product.getId(), 3, null);

            // act
            orderService.cancel(order.getId());

            // assert
            Order cancelled = orderService.findById(order.getId());
            assertThat(cancelled.getStatus()).isEqualTo(OrderStatus.CANCELLED);

            Product restored = productService.findById(product.getId());
            assertThat(restored.getStock()).isEqualTo(STOCK);
        }

        @DisplayName("DELIVERED 상태인 주문을 취소하면, BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsBadRequest_whenCancellingDeliveredOrder() {
            // arrange
            Product product = productService.create(brandId, PRODUCT_NAME, null, PRICE, STOCK, SellingStatus.SELLING);
            Order order = orderService.create(USER_ID, product.getId(), 1, null);
            order.changeStatus(OrderStatus.DELIVERED);
            orderRepository.save(order);

            // act
            CoreException ex = assertThrows(CoreException.class, () -> orderService.cancel(order.getId()));

            // assert
            assertThat(ex.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }
    }
}
