package com.loopers.infrastructure.like;

import com.loopers.domain.like.Like;
import com.loopers.domain.like.LikeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@Component
public class LikeRepositoryImpl implements LikeRepository {

    private final LikeJpaRepository likeJpaRepository;

    @Override
    public Optional<Like> findByUserIdAndProductId(Long userId, Long productId) {
        return likeJpaRepository.findByIdUserIdAndIdProductId(userId, productId);
    }

    @Override
    public List<Like> findByUserId(Long userId) {
        return likeJpaRepository.findByIdUserId(userId);
    }

    @Override
    public Like save(Like like) {
        return likeJpaRepository.save(like);
    }

    @Override
    public void delete(Like like) {
        likeJpaRepository.delete(like);
    }
}
