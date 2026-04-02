package com.loopers.support.auth;

import com.loopers.domain.queue.EntryTokenService;
import com.loopers.domain.user.UserModel;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@RequiredArgsConstructor
@Component
public class QueueEntryTokenInterceptor implements HandlerInterceptor {

    public static final String HEADER_ENTRY_TOKEN = "X-Entry-Token";

    private final EntryTokenService entryTokenService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        UserModel user = (UserModel) request.getAttribute(AuthInterceptor.ATTR_AUTHENTICATED_USER);
        String token = request.getHeader(HEADER_ENTRY_TOKEN);

        try {
            if (!entryTokenService.validate(user.getId(), token)) {
                throw new CoreException(ErrorType.FORBIDDEN, "입장 토큰이 필요합니다.");
            }
        } catch (DataAccessException e) {
            // Graceful Degradation: Redis 장애 시 서비스 차단
            throw new CoreException(ErrorType.SERVICE_UNAVAILABLE, "대기열 서비스를 일시적으로 사용할 수 없습니다.");
        }

        return true;
    }
}
