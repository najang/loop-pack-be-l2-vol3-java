package com.loopers.support.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@RequiredArgsConstructor
@Configuration
public class AuthenticationConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;
    private final AdminAuthInterceptor adminAuthInterceptor;
    private final OptionalAuthInterceptor optionalAuthInterceptor;
    private final AuthenticatedUserArgumentResolver authenticatedUserArgumentResolver;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
            .addPathPatterns("/api/v1/users/me")
            .addPathPatterns("/api/v1/users/password")
            .addPathPatterns("/api/v1/products/*/likes")
            .addPathPatterns("/api/v1/users/*/likes")
            .addPathPatterns("/api/v1/orders")
            .addPathPatterns("/api/v1/orders/*")
            .addPathPatterns("/api/v1/orders/*/cancel")
            .addPathPatterns("/api/v1/cart")
            .addPathPatterns("/api/v1/cart/items")
            .addPathPatterns("/api/v1/cart/items/*")
            .addPathPatterns("/api/v1/coupons/*/issue")
            .addPathPatterns("/api/v1/users/me/coupons")
            .addPathPatterns("/api/v1/users/me/points/charge");

        registry.addInterceptor(adminAuthInterceptor)
            .addPathPatterns("/api-admin/v1/**");

        registry.addInterceptor(optionalAuthInterceptor)
            .addPathPatterns("/api/v1/products/*");
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(authenticatedUserArgumentResolver);
    }
}
