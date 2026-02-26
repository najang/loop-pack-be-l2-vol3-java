package com.loopers.interfaces.api.order;

import com.loopers.interfaces.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

@Tag(name = "Order Admin V1 API", description = "주문 관련 관리자 API 입니다.")
public interface OrderAdminV1ApiSpec {

    @Operation(
        summary = "관리자 주문 목록 조회",
        description = "기간 필터로 전체 주문 목록을 조회합니다."
    )
    ApiResponse<List<OrderAdminV1Dto.OrderResponse>> getOrders(
        String startAt,
        String endAt
    );

    @Operation(
        summary = "관리자 주문 단건 조회",
        description = "주문 ID로 주문 상세를 조회합니다."
    )
    ApiResponse<OrderAdminV1Dto.OrderResponse> getOrder(Long orderId);

    @Operation(
        summary = "주문 상태 변경",
        description = "주문의 상태를 변경합니다."
    )
    ApiResponse<OrderAdminV1Dto.OrderResponse> updateOrderStatus(
        Long orderId,
        OrderAdminV1Dto.UpdateStatusRequest request
    );
}
