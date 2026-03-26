package com.loopers.application.like;

import com.loopers.domain.event.LikeEvent;
import com.loopers.domain.event.UserActionEvent;
import com.loopers.domain.like.Like;
import com.loopers.domain.like.LikeRepository;
import com.loopers.domain.product.ProductRepository;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Component
public class LikeApplicationService {

    private final LikeRepository likeRepository;
    private final ProductRepository productRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public void like(Long userId, Long productId) {
        productRepository.findById(productId)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "상품을 찾을 수 없습니다."));

        likeRepository.findByUserIdAndProductId(userId, productId)
            .ifPresentOrElse(
                like -> {},
                () -> {
                    likeRepository.save(new Like(userId, productId));
                    eventPublisher.publishEvent(LikeEvent.of(LikeEvent.Type.LIKED, productId, userId));
                    eventPublisher.publishEvent(new UserActionEvent(UserActionEvent.EventType.LIKED, userId, productId, null));
                }
            );
    }

    @Transactional
    public void unlike(Long userId, Long productId) {
        productRepository.findById(productId)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "상품을 찾을 수 없습니다."));

        likeRepository.findByUserIdAndProductId(userId, productId)
            .ifPresent(like -> {
                likeRepository.delete(like);
                eventPublisher.publishEvent(LikeEvent.of(LikeEvent.Type.UNLIKED, productId, userId));
                eventPublisher.publishEvent(new UserActionEvent(UserActionEvent.EventType.UNLIKED, userId, productId, null));
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
