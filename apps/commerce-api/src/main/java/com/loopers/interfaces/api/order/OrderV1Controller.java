package com.loopers.interfaces.api.order;

import com.loopers.application.order.OrderFacade;
import com.loopers.application.payment.PaymentFacade;
import com.loopers.domain.user.UserModel;
import com.loopers.interfaces.api.ApiResponse;
import com.loopers.support.auth.LoginUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.ZonedDateTime;
import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1")
public class OrderV1Controller implements OrderV1ApiSpec {

    private final OrderFacade orderFacade;
    private final PaymentFacade paymentFacade;

    @PostMapping("/orders")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @Override
    public ApiResponse<OrderV1Dto.OrderResponse> createOrder(
        @LoginUser UserModel user,
        @Valid @RequestBody OrderV1Dto.CreateRequest request
    ) {
        return ApiResponse.success(OrderV1Dto.OrderResponse.from(
            paymentFacade.createOrderAndPay(
                user.getId(),
                request.productId(),
                request.quantity(),
                request.couponId(),
                request.cardType(),
                request.cardNo()
            )
        ));
    }

    @GetMapping("/orders/{orderId}")
    @Override
    public ApiResponse<OrderV1Dto.OrderResponse> getOrder(
        @LoginUser UserModel user,
        @PathVariable Long orderId
    ) {
        return ApiResponse.success(OrderV1Dto.OrderResponse.from(
            orderFacade.findById(user.getId(), orderId)
        ));
    }

    @GetMapping("/orders")
    @Override
    public ApiResponse<List<OrderV1Dto.OrderResponse>> getMyOrders(
        @LoginUser UserModel user,
        @RequestParam String startAt,
        @RequestParam String endAt
    ) {
        ZonedDateTime start = ZonedDateTime.parse(startAt);
        ZonedDateTime end = ZonedDateTime.parse(endAt);
        List<OrderV1Dto.OrderResponse> orders = orderFacade.findByUserIdAndPeriod(user.getId(), start, end).stream()
            .map(OrderV1Dto.OrderResponse::from)
            .toList();
        return ApiResponse.success(orders);
    }

    @PatchMapping("/orders/{orderId}/cancel")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Override
    public void cancelOrder(
        @LoginUser UserModel user,
        @PathVariable Long orderId
    ) {
        orderFacade.cancel(user.getId(), orderId);
    }
}
