package com.loopers.application.like;

import com.loopers.application.product.ProductInfo;
import com.loopers.domain.like.Like;
import com.loopers.application.like.LikeApplicationService;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Component
public class LikeFacade {

    private final LikeApplicationService likeService;
    private final ProductService productService;

    public LikeInfo like(Long userId, Long productId) {
        Product product = likeService.like(userId, productId);
        return new LikeInfo(true, product.getLikeCount());
    }

    public void unlike(Long userId, Long productId) {
        likeService.unlike(userId, productId);
    }

    public Page<ProductInfo> findLikedProducts(Long userId, Pageable pageable) {
        Page<Like> likes = likeService.findByUserId(userId, pageable);
        List<Long> ids = likes.getContent().stream().map(Like::getProductId).toList();
        Map<Long, Product> productMap = productService.findAllByIds(ids).stream()
            .collect(Collectors.toMap(Product::getId, p -> p));
        List<ProductInfo> infos = ids.stream()
            .filter(productMap::containsKey)
            .map(id -> ProductInfo.from(productMap.get(id), true))
            .toList();
        return new PageImpl<>(infos, pageable, likes.getTotalElements());
    }
}
