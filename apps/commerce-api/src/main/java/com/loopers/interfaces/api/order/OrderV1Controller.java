package com.loopers.interfaces.api.order;

import com.loopers.application.order.OrderFacade;
import com.loopers.domain.user.UserModel;
import com.loopers.interfaces.api.ApiResponse;
import com.loopers.support.auth.LoginUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1")
public class OrderV1Controller implements OrderV1ApiSpec {

    private final OrderFacade orderFacade;

    @PostMapping("/orders")
    @ResponseStatus(HttpStatus.CREATED)
    @Override
    public ApiResponse<OrderV1Dto.OrderResponse> createOrder(
        @LoginUser UserModel user,
        @Valid @RequestBody OrderV1Dto.CreateRequest request
    ) {
        return ApiResponse.success(OrderV1Dto.OrderResponse.from(
            orderFacade.create(user.getId(), request.productId(), request.quantity())
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

    @GetMapping("/users/me/orders")
    @Override
    public ApiResponse<List<OrderV1Dto.OrderResponse>> getMyOrders(
        @LoginUser UserModel user
    ) {
        List<OrderV1Dto.OrderResponse> orders = orderFacade.findByUserId(user.getId()).stream()
            .map(OrderV1Dto.OrderResponse::from)
            .toList();
        return ApiResponse.success(orders);
    }

    @DeleteMapping("/orders/{orderId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Override
    public void cancelOrder(
        @LoginUser UserModel user,
        @PathVariable Long orderId
    ) {
        orderFacade.cancel(user.getId(), orderId);
    }
}
