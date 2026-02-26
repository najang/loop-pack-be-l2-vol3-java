package com.loopers.interfaces.api.product;

import com.loopers.application.product.ProductFacade;
import com.loopers.domain.user.UserModel;
import com.loopers.interfaces.api.ApiResponse;
import com.loopers.support.auth.LoginUser;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/products")
public class ProductV1Controller implements ProductV1ApiSpec {

    private final ProductFacade productFacade;

    @GetMapping
    @Override
    public ApiResponse<ProductV1Dto.ProductPageResponse> getProducts(
        @RequestParam(required = false) Long brandId,
        @RequestParam(defaultValue = "latest") String sort,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size
    ) {
        PageRequest pageable = PageRequest.of(page, size, toSort(sort));
        return ApiResponse.success(ProductV1Dto.ProductPageResponse.from(productFacade.findAll(brandId, pageable)));
    }

    @GetMapping("/{productId}")
    @Override
    public ApiResponse<ProductV1Dto.ProductResponse> getProduct(
        @PathVariable Long productId,
        @LoginUser UserModel user
    ) {
        Long userId = user != null ? user.getId() : null;
        return ApiResponse.success(ProductV1Dto.ProductResponse.from(productFacade.findById(productId, userId)));
    }

    private Sort toSort(String sort) {
        return switch (sort) {
            case "price_asc" -> Sort.by(Sort.Direction.ASC, "price.value");
            case "likes_desc" -> Sort.by(Sort.Direction.DESC, "likeCount.value");
            default -> Sort.by(Sort.Direction.DESC, "createdAt");
        };
    }
}
