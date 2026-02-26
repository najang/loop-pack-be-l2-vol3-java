package com.loopers.interfaces.api.order;

import com.loopers.domain.user.UserModel;
import com.loopers.interfaces.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

@Tag(name = "Order V1 API", description = "주문 관련 사용자 API 입니다.")
public interface OrderV1ApiSpec {

    @Operation(
        summary = "주문 생성",
        description = "상품을 주문합니다. 재고를 차감하고 주문을 생성합니다."
    )
    ApiResponse<OrderV1Dto.OrderResponse> createOrder(
        @Parameter(hidden = true) UserModel user,
        OrderV1Dto.CreateRequest request
    );

    @Operation(
        summary = "주문 단건 조회",
        description = "본인의 주문을 ID로 조회합니다."
    )
    ApiResponse<OrderV1Dto.OrderResponse> getOrder(
        @Parameter(hidden = true) UserModel user,
        Long orderId
    );

    @Operation(
        summary = "내 주문 목록 조회",
        description = "로그인한 사용자의 전체 주문 목록을 조회합니다."
    )
    ApiResponse<List<OrderV1Dto.OrderResponse>> getMyOrders(
        @Parameter(hidden = true) UserModel user
    );

    @Operation(
        summary = "주문 취소",
        description = "본인의 주문을 취소합니다. 재고가 복원됩니다."
    )
    void cancelOrder(
        @Parameter(hidden = true) UserModel user,
        Long orderId
    );
}
