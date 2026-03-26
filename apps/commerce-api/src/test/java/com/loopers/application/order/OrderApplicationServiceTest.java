package com.loopers.application.order;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.brand.BrandRepository;
import com.loopers.domain.coupon.CouponService;
import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderItem;
import com.loopers.domain.order.OrderRepository;
import com.loopers.domain.outbox.OutboxEvent;
import com.loopers.domain.outbox.OutboxRepository;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductRepository;
import org.springframework.context.ApplicationEventPublisher;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderApplicationServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long PRODUCT_ID = 100L;
    private static final Long ORDER_ID = 1L;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private BrandRepository brandRepository;

    @Mock
    private CouponService couponService;

    @Mock
    private OutboxRepository outboxRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private OrderApplicationService orderApplicationService;

    @DisplayName("주문 생성 시,")
    @Nested
    class Create {

        @DisplayName("상품이 존재하지 않으면, NOT_FOUND 예외가 발생한다.")
        @Test
        void throwsNotFound_whenProductDoesNotExist() {
            // arrange
            when(productRepository.findByIdWithLock(PRODUCT_ID)).thenReturn(Optional.empty());

            // act
            CoreException ex = assertThrows(CoreException.class, () -> orderApplicationService.create(USER_ID, PRODUCT_ID, 1, null));

            // assert
            assertThat(ex.getErrorType()).isEqualTo(ErrorType.NOT_FOUND);
        }

        @DisplayName("canOrder()가 false이면, BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsBadRequest_whenProductCannotBeOrdered() {
            // arrange
            Product product = mock(Product.class);
            when(productRepository.findByIdWithLock(PRODUCT_ID)).thenReturn(Optional.of(product));
            when(product.canOrder()).thenReturn(false);

            // act
            CoreException ex = assertThrows(CoreException.class, () -> orderApplicationService.create(USER_ID, PRODUCT_ID, 1, null));

            // assert
            assertThat(ex.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }

        @DisplayName("정상 주문이면, product.deductStock + productRepository.save + orderRepository.save가 호출된다.")
        @Test
        void deductsStockAndSavesOrder_whenOrderIsValid() {
            // arrange
            Product product = mock(Product.class);
            Brand brand = mock(Brand.class);
            when(productRepository.findByIdWithLock(PRODUCT_ID)).thenReturn(Optional.of(product));
            when(product.canOrder()).thenReturn(true);
            when(product.getBrandId()).thenReturn(1L);
            when(product.getName()).thenReturn("테스트상품");
            when(product.getPrice()).thenReturn(1000);
            when(brandRepository.findById(1L)).thenReturn(Optional.of(brand));
            when(brand.getName()).thenReturn("테스트브랜드");
            when(productRepository.save(product)).thenReturn(product);
            Order savedOrder = mock(Order.class);
            when(savedOrder.getId()).thenReturn(ORDER_ID);
            when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);

            // act
            orderApplicationService.create(USER_ID, PRODUCT_ID, 2, null);

            // assert
            verify(product, times(1)).deductStock(2);
            verify(productRepository, times(1)).save(product);
            verify(orderRepository, times(1)).save(any(Order.class));
            verify(outboxRepository, times(1)).save(any(OutboxEvent.class));
        }
    }

    @DisplayName("주문 단건 조회 시,")
    @Nested
    class FindById {

        @DisplayName("주문이 존재하면, 주문을 반환한다.")
        @Test
        void returnsOrder_whenOrderExists() {
            // arrange
            Order order = mock(Order.class);
            when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));

            // act
            Order result = orderApplicationService.findById(ORDER_ID);

            // assert
            assertThat(result).isEqualTo(order);
        }

        @DisplayName("주문이 존재하지 않으면, NOT_FOUND 예외가 발생한다.")
        @Test
        void throwsNotFound_whenOrderDoesNotExist() {
            // arrange
            when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.empty());

            // act
            CoreException ex = assertThrows(CoreException.class, () -> orderApplicationService.findById(ORDER_ID));

            // assert
            assertThat(ex.getErrorType()).isEqualTo(ErrorType.NOT_FOUND);
        }
    }

    @DisplayName("사용자별 주문 목록 조회 시,")
    @Nested
    class FindByUserId {

        @DisplayName("해당 user의 주문 목록을 반환한다.")
        @Test
        void returnsOrderList_forGivenUser() {
            // arrange
            Order order1 = mock(Order.class);
            Order order2 = mock(Order.class);
            when(orderRepository.findByUserId(USER_ID)).thenReturn(List.of(order1, order2));

            // act
            List<Order> result = orderApplicationService.findByUserId(USER_ID);

            // assert
            assertThat(result).containsExactly(order1, order2);
        }
    }

    @DisplayName("주문 취소 시,")
    @Nested
    class Cancel {

        @DisplayName("주문이 존재하지 않으면, NOT_FOUND 예외가 발생한다.")
        @Test
        void throwsNotFound_whenOrderDoesNotExist() {
            // arrange
            when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.empty());

            // act
            CoreException ex = assertThrows(CoreException.class, () -> orderApplicationService.cancel(ORDER_ID));

            // assert
            assertThat(ex.getErrorType()).isEqualTo(ErrorType.NOT_FOUND);
            verify(orderRepository, never()).save(any());
        }

        @DisplayName("정상 취소이면, order.cancel() + product.restoreStock + productRepository.save + orderRepository.save가 호출된다.")
        @Test
        void restoresStockAndCancelsOrder_whenCancelIsValid() {
            // arrange
            OrderItem item = mock(OrderItem.class);
            when(item.getProductId()).thenReturn(PRODUCT_ID);
            when(item.getQuantity()).thenReturn(2);

            Order order = mock(Order.class);
            when(order.getItems()).thenReturn(List.of(item));
            when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));

            Product product = mock(Product.class);
            when(productRepository.findByIdWithLock(PRODUCT_ID)).thenReturn(Optional.of(product));
            when(productRepository.save(product)).thenReturn(product);
            when(orderRepository.save(order)).thenReturn(order);

            // act
            orderApplicationService.cancel(ORDER_ID);

            // assert
            verify(product, times(1)).restoreStock(2);
            verify(productRepository, times(1)).save(product);
            verify(order, times(1)).cancel();
            verify(orderRepository, times(1)).save(order);
        }
    }
}
