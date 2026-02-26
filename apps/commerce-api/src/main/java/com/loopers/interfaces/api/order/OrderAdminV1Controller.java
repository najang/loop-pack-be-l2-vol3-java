package com.loopers.interfaces.api.order;

import com.loopers.application.order.OrderFacade;
import com.loopers.interfaces.api.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.ZonedDateTime;
import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api-admin/v1/orders")
public class OrderAdminV1Controller implements OrderAdminV1ApiSpec {

    private final OrderFacade orderFacade;

    @GetMapping
    @Override
    public ApiResponse<List<OrderAdminV1Dto.OrderResponse>> getOrders(
        @RequestParam String startAt,
        @RequestParam String endAt
    ) {
        ZonedDateTime start = ZonedDateTime.parse(startAt);
        ZonedDateTime end = ZonedDateTime.parse(endAt);
        List<OrderAdminV1Dto.OrderResponse> orders = orderFacade.findAllByPeriod(start, end).stream()
            .map(OrderAdminV1Dto.OrderResponse::from)
            .toList();
        return ApiResponse.success(orders);
    }

    @GetMapping("/{orderId}")
    @Override
    public ApiResponse<OrderAdminV1Dto.OrderResponse> getOrder(@PathVariable Long orderId) {
        return ApiResponse.success(OrderAdminV1Dto.OrderResponse.from(orderFacade.findByIdForAdmin(orderId)));
    }

    @PatchMapping("/{orderId}/status")
    @Override
    public ApiResponse<OrderAdminV1Dto.OrderResponse> updateOrderStatus(
        @PathVariable Long orderId,
        @Valid @RequestBody OrderAdminV1Dto.UpdateStatusRequest request
    ) {
        return ApiResponse.success(OrderAdminV1Dto.OrderResponse.from(
            orderFacade.changeStatus(orderId, request.status())
        ));
    }
}
