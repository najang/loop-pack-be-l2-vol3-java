package com.loopers.application.product;

import com.loopers.domain.product.Product;
import com.loopers.domain.product.SellingStatus;

public record ProductInfo(
    Long id,
    Long brandId,
    String name,
    String description,
    int price,
    int stock,
    SellingStatus sellingStatus,
    int likeCount,
    Boolean isLiked
) {
    public static ProductInfo from(Product product) {
        return new ProductInfo(
            product.getId(),
            product.getBrandId(),
            product.getName(),
            product.getDescription(),
            product.getPrice(),
            product.getStock(),
            product.getSellingStatus(),
            product.getLikeCount(),
            null
        );
    }

    public static ProductInfo from(Product product, Boolean isLiked) {
        return new ProductInfo(
            product.getId(),
            product.getBrandId(),
            product.getName(),
            product.getDescription(),
            product.getPrice(),
            product.getStock(),
            product.getSellingStatus(),
            product.getLikeCount(),
            isLiked
        );
    }
}
