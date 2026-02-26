package com.loopers.support.auth;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AdminAuthInterceptor implements HandlerInterceptor {

    public static final String HEADER_LDAP = "X-Loopers-Ldap";
    public static final String ADMIN_VALUE = "loopers.admin";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String ldap = request.getHeader(HEADER_LDAP);
        if (!ADMIN_VALUE.equals(ldap)) {
            throw new CoreException(ErrorType.UNAUTHORIZED, "어드민 권한이 필요합니다.");
        }
        return true;
    }
}
