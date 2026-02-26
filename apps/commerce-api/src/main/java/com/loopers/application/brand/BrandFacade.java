package com.loopers.application.brand;

import com.loopers.domain.brand.BrandService;
import com.loopers.domain.product.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class BrandFacade {

    private final BrandService brandService;
    private final ProductService productService;

    public BrandInfo findById(Long brandId) {
        return BrandInfo.from(brandService.findById(brandId));
    }

    public Page<BrandInfo> findAll(Pageable pageable) {
        return brandService.findAll(pageable).map(BrandInfo::from);
    }

    public BrandInfo create(String name, String description) {
        return BrandInfo.from(brandService.create(name, description));
    }

    public BrandInfo update(Long brandId, String name, String description) {
        return BrandInfo.from(brandService.update(brandId, name, description));
    }

    public void delete(Long brandId) {
        brandService.findById(brandId);
        productService.deleteByBrandId(brandId);
        brandService.delete(brandId);
    }
}
