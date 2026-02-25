package com.loopers.domain.like;

import java.util.List;
import java.util.Optional;

public interface LikeRepository {
    Optional<Like> findByUserIdAndProductId(Long userId, Long productId);

    List<Like> findByUserId(Long userId);

    Like save(Like like);

    void delete(Like like);
}
