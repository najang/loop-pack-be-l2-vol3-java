package com.loopers.domain.like;

import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductRepository;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@Component
public class LikeService {

    private final LikeRepository likeRepository;
    private final ProductRepository productRepository;

    @Transactional
    public void toggle(Long userId, Long productId) {
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "상품을 찾을 수 없습니다."));

        Optional<Like> existingLike = likeRepository.findByUserIdAndProductId(userId, productId);
        if (existingLike.isPresent()) {
            likeRepository.delete(existingLike.get());
            product.decreaseLikes();
        } else {
            likeRepository.save(new Like(userId, productId));
            product.increaseLikes();
        }
        productRepository.save(product);
    }

    @Transactional(readOnly = true)
    public List<Long> findLikedProductIds(Long userId) {
        return likeRepository.findByUserId(userId).stream()
            .map(Like::getProductId)
            .toList();
    }
}
