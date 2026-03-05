package com.loopers.domain.like;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface LikeRepository {
    Optional<Like> findByUserIdAndProductId(Long userId, Long productId);

    List<Like> findByUserId(Long userId);

    Page<Like> findByUserId(Long userId, Pageable pageable);

    Like save(Like like);

    void delete(Like like);
}
