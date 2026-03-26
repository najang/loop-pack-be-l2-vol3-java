package com.loopers.interfaces.api.eventlog;

import com.loopers.application.eventlog.EventLogFacade;
import com.loopers.interfaces.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api-admin/v1/event-logs")
public class EventLogAdminV1Controller implements EventLogAdminV1ApiSpec {

    private final EventLogFacade eventLogFacade;

    @GetMapping
    @Override
    public ApiResponse<EventLogAdminV1Dto.EventLogPageResponse> getEventLogs(
        @PageableDefault(size = 20) Pageable pageable
    ) {
        return ApiResponse.success(EventLogAdminV1Dto.EventLogPageResponse.from(eventLogFacade.findAll(pageable)));
    }
}
