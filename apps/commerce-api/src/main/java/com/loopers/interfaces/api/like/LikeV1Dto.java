package com.loopers.interfaces.api.like;

import com.loopers.application.like.LikeInfo;
import com.loopers.application.product.ProductInfo;
import org.springframework.data.domain.Page;

import java.util.List;

public class LikeV1Dto {

    public record LikeResponse(boolean liked, int likeCount) {
        public static LikeResponse from(LikeInfo info) {
            return new LikeResponse(info.liked(), info.likeCount());
        }
    }

    public record LikedProductResponse(
        Long id,
        Long brandId,
        String name,
        String description,
        int price,
        int stock,
        String sellingStatus,
        int likeCount,
        Boolean isLiked
    ) {
        public static LikedProductResponse from(ProductInfo info) {
            return new LikedProductResponse(
                info.id(),
                info.brandId(),
                info.name(),
                info.description(),
                info.price(),
                info.stock(),
                info.sellingStatus().name(),
                info.likeCount(),
                info.isLiked()
            );
        }
    }

    public record LikedProductPageResponse(
        List<LikedProductResponse> content,
        int page,
        int size,
        long totalElements
    ) {
        public static LikedProductPageResponse from(Page<ProductInfo> page) {
            List<LikedProductResponse> content = page.getContent().stream()
                .map(LikedProductResponse::from)
                .toList();
            return new LikedProductPageResponse(content, page.getNumber(), page.getSize(), page.getTotalElements());
        }
    }
}
