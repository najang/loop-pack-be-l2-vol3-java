package com.loopers.interfaces.api.product;

import com.loopers.application.product.ProductFacade;
import com.loopers.interfaces.api.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api-admin/v1/products")
public class ProductAdminV1Controller implements ProductAdminV1ApiSpec {

    private final ProductFacade productFacade;

    @GetMapping
    @Override
    public ApiResponse<ProductAdminV1Dto.ProductPageResponse> getProducts(
        @RequestParam(required = false) Long brandId,
        @PageableDefault(size = 10) Pageable pageable
    ) {
        return ApiResponse.success(ProductAdminV1Dto.ProductPageResponse.from(productFacade.findAll(brandId, pageable)));
    }

    @GetMapping("/{productId}")
    @Override
    public ApiResponse<ProductAdminV1Dto.ProductResponse> getProduct(@PathVariable Long productId) {
        return ApiResponse.success(ProductAdminV1Dto.ProductResponse.from(productFacade.findById(productId)));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Override
    public ApiResponse<ProductAdminV1Dto.ProductResponse> createProduct(
        @Valid @RequestBody ProductAdminV1Dto.CreateRequest request
    ) {
        return ApiResponse.success(ProductAdminV1Dto.ProductResponse.from(
            productFacade.create(
                request.brandId(),
                request.name(),
                request.description(),
                request.price(),
                request.stock(),
                request.sellingStatus()
            )
        ));
    }

    @PatchMapping("/{productId}")
    @Override
    public ApiResponse<ProductAdminV1Dto.ProductResponse> updateProduct(
        @PathVariable Long productId,
        @Valid @RequestBody ProductAdminV1Dto.UpdateRequest request
    ) {
        return ApiResponse.success(ProductAdminV1Dto.ProductResponse.from(
            productFacade.update(
                productId,
                request.name(),
                request.description(),
                request.price(),
                request.sellingStatus()
            )
        ));
    }

    @DeleteMapping("/{productId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Override
    public void deleteProduct(@PathVariable Long productId) {
        productFacade.delete(productId);
    }
}
