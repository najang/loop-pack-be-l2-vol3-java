package com.loopers.support.auth;

import com.loopers.domain.user.UserModel;
import com.loopers.domain.user.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@RequiredArgsConstructor
@Component
public class OptionalAuthInterceptor implements HandlerInterceptor {

    private final UserService userService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String loginId = request.getHeader(AuthInterceptor.HEADER_LOGIN_ID);
        String password = request.getHeader(AuthInterceptor.HEADER_LOGIN_PW);

        if (loginId == null && password == null) {
            return true;
        }

        UserModel user = userService.authenticate(loginId, password);
        request.setAttribute(AuthInterceptor.ATTR_AUTHENTICATED_USER, user);

        return true;
    }
}
