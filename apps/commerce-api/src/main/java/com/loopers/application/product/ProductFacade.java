package com.loopers.application.product;

import com.loopers.domain.brand.BrandService;
import com.loopers.domain.like.LikeService;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductService;
import com.loopers.domain.product.SellingStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class ProductFacade {

    private final ProductService productService;
    private final BrandService brandService;
    private final LikeService likeService;

    public ProductInfo findById(Long productId) {
        return findById(productId, null);
    }

    public ProductInfo findById(Long productId, Long userId) {
        Product product = productService.findById(productId);
        Boolean isLiked = userId != null ? likeService.isLiked(userId, productId) : null;
        return ProductInfo.from(product, isLiked);
    }

    public Page<ProductInfo> findAll(Long brandId, Pageable pageable) {
        return productService.findAll(brandId, pageable).map(ProductInfo::from);
    }

    public ProductInfo create(Long brandId, String name, String description, int price, int stock, SellingStatus sellingStatus) {
        brandService.findById(brandId);
        return ProductInfo.from(productService.create(brandId, name, description, price, stock, sellingStatus));
    }

    public ProductInfo update(Long productId, String name, String description, int price, SellingStatus sellingStatus) {
        return ProductInfo.from(productService.update(productId, name, description, price, sellingStatus));
    }

    public void delete(Long productId) {
        productService.delete(productId);
    }
}
