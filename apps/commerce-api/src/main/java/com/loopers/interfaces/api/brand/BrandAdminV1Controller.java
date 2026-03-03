package com.loopers.interfaces.api.brand;

import com.loopers.application.brand.BrandFacade;
import com.loopers.application.brand.BrandInfo;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api-admin/v1/brands")
public class BrandAdminV1Controller implements BrandAdminV1ApiSpec {

    private final BrandFacade brandFacade;

    @GetMapping
    @Override
    public ApiResponse<BrandAdminV1Dto.BrandPageResponse> getBrands(
        @PageableDefault(size = 10) Pageable pageable
    ) {
        return ApiResponse.success(BrandAdminV1Dto.BrandPageResponse.from(brandFacade.findAll(pageable)));
    }

    @GetMapping("/{brandId}")
    @Override
    public ApiResponse<BrandAdminV1Dto.BrandResponse> getBrand(@PathVariable Long brandId) {
        BrandInfo info = brandFacade.findById(brandId);
        return ApiResponse.success(BrandAdminV1Dto.BrandResponse.from(info));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Override
    public ApiResponse<BrandAdminV1Dto.BrandResponse> createBrand(
        @Valid @RequestBody BrandAdminV1Dto.CreateRequest request
    ) {
        BrandInfo info = brandFacade.create(request.name(), request.description());
        return ApiResponse.success(BrandAdminV1Dto.BrandResponse.from(info));
    }

    @PatchMapping("/{brandId}")
    @Override
    public ApiResponse<BrandAdminV1Dto.BrandResponse> updateBrand(
        @PathVariable Long brandId,
        @Valid @RequestBody BrandAdminV1Dto.UpdateRequest request
    ) {
        BrandInfo info = brandFacade.update(brandId, request.name(), request.description());
        return ApiResponse.success(BrandAdminV1Dto.BrandResponse.from(info));
    }

    @DeleteMapping("/{brandId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Override
    public void deleteBrand(@PathVariable Long brandId) {
        brandFacade.delete(brandId);
    }
}
