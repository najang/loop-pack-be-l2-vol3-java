package com.loopers.application.order;

import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderService;
import com.loopers.domain.order.OrderStatus;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.List;

@RequiredArgsConstructor
@Component
public class OrderFacade {

    private final OrderService orderService;

    public OrderInfo create(Long userId, Long productId, int quantity, Long userCouponId) {
        return OrderInfo.from(orderService.create(userId, productId, quantity, userCouponId));
    }

    @Transactional(readOnly = true)
    public OrderInfo findById(Long userId, Long orderId) {
        Order order = orderService.findById(orderId);
        if (!order.getUserId().equals(userId)) {
            throw new CoreException(ErrorType.NOT_FOUND, "주문을 찾을 수 없습니다.");
        }
        return OrderInfo.from(order);
    }

    @Transactional(readOnly = true)
    public List<OrderInfo> findByUserId(Long userId) {
        return orderService.findByUserId(userId).stream()
            .map(OrderInfo::from)
            .toList();
    }

    @Transactional(readOnly = true)
    public List<OrderInfo> findByUserIdAndPeriod(Long userId, ZonedDateTime startAt, ZonedDateTime endAt) {
        return orderService.findByUserIdAndPeriod(userId, startAt, endAt).stream()
            .map(OrderInfo::from)
            .toList();
    }

    public void cancel(Long userId, Long orderId) {
        Order order = orderService.findById(orderId);
        if (!order.getUserId().equals(userId)) {
            throw new CoreException(ErrorType.NOT_FOUND, "주문을 찾을 수 없습니다.");
        }
        orderService.cancel(orderId);
    }

    @Transactional(readOnly = true)
    public List<OrderInfo> findAllByPeriod(ZonedDateTime startAt, ZonedDateTime endAt) {
        return orderService.findAllByPeriod(startAt, endAt).stream()
            .map(OrderInfo::from)
            .toList();
    }

    @Transactional(readOnly = true)
    public OrderInfo findByIdForAdmin(Long orderId) {
        return OrderInfo.from(orderService.findById(orderId));
    }

    public OrderInfo changeStatus(Long orderId, OrderStatus status) {
        Order order = orderService.findById(orderId);
        order.changeStatus(status);
        return OrderInfo.from(orderService.save(order));
    }
}
