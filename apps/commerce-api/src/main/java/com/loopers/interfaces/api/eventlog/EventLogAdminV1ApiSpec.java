package com.loopers.interfaces.api.eventlog;

import com.loopers.interfaces.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Pageable;

@Tag(name = "EventLog Admin", description = "이벤트 로그 어드민 API")
public interface EventLogAdminV1ApiSpec {

    @Operation(summary = "이벤트 로그 목록 조회", description = "이벤트 로그를 페이징하여 조회합니다.")
    ApiResponse<EventLogAdminV1Dto.EventLogPageResponse> getEventLogs(Pageable pageable);
}
