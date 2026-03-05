package com.loopers.infrastructure.like;

import com.loopers.domain.like.Like;
import com.loopers.domain.like.LikeId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LikeJpaRepository extends JpaRepository<Like, LikeId> {
    Optional<Like> findByIdUserIdAndIdProductId(Long userId, Long productId);

    List<Like> findByIdUserId(Long userId);

    Page<Like> findByIdUserId(Long userId, Pageable pageable);
}
