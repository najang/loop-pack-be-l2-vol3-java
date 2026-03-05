package com.loopers.application.like;

import com.loopers.domain.like.Like;
import com.loopers.domain.like.LikeRepository;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductRepository;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@RequiredArgsConstructor
@Component
public class LikeApplicationService {

    private final LikeRepository likeRepository;
    private final ProductRepository productRepository;

    @Transactional
    public Product like(Long userId, Long productId) {
        Product product = productRepository.findByIdWithLock(productId)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "상품을 찾을 수 없습니다."));

        Optional<Like> existingLike = likeRepository.findByUserIdAndProductId(userId, productId);
        existingLike.ifPresentOrElse(
            like -> {},
            () -> {
                likeRepository.save(new Like(userId, productId));
                product.increaseLikes();
                productRepository.save(product);
            }
        );

        return product;
    }

    @Transactional
    public void unlike(Long userId, Long productId) {
        Product product = productRepository.findByIdWithLock(productId)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "상품을 찾을 수 없습니다."));

        likeRepository.findByUserIdAndProductId(userId, productId)
            .ifPresent(like -> {
                likeRepository.delete(like);
                product.decreaseLikes();
                productRepository.save(product);
            });
    }

    @Transactional(readOnly = true)
    public boolean isLiked(Long userId, Long productId) {
        return likeRepository.findByUserIdAndProductId(userId, productId).isPresent();
    }

    @Transactional(readOnly = true)
    public Page<Like> findByUserId(Long userId, Pageable pageable) {
        return likeRepository.findByUserId(userId, pageable);
    }
}
