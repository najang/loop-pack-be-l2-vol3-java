package com.loopers.interfaces.api.queue;

import com.loopers.domain.user.UserModel;
import com.loopers.interfaces.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Queue V1 API", description = "대기열 관련 사용자 API 입니다.")
public interface QueueV1ApiSpec {

    @Operation(
        summary = "대기열 진입",
        description = "대기열에 진입합니다. 이미 진입한 경우 현재 순번을 반환합니다."
    )
    ApiResponse<QueueV1Dto.QueueStatusResponse> enter(
        @Parameter(hidden = true) UserModel user
    );

    @Operation(
        summary = "순번 조회",
        description = "현재 순번과 예상 대기 시간을 조회합니다. 토큰이 발급된 경우 token 필드에 포함됩니다."
    )
    ApiResponse<QueueV1Dto.QueueStatusResponse> getPosition(
        @Parameter(hidden = true) UserModel user
    );
}
