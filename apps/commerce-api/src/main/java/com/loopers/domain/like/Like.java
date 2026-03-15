package com.loopers.domain.like;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;

import java.time.ZonedDateTime;

@Getter
@Entity
@Table(name = "likes", indexes = {
    @Index(name = "idx_user_created_at", columnList = "user_id, created_at DESC")
})
public class Like {

    @EmbeddedId
    private LikeId id;

    @Column(name = "created_at", nullable = false, updatable = false)
    private ZonedDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private ZonedDateTime updatedAt;

    protected Like() {
    }

    public Like(Long userId, Long productId) {
        if (userId == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "사용자 ID는 필수입니다.");
        }
        if (productId == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "상품 ID는 필수입니다.");
        }
        this.id = new LikeId(userId, productId);
    }

    @PrePersist
    private void prePersist() {
        ZonedDateTime now = ZonedDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    private void preUpdate() {
        this.updatedAt = ZonedDateTime.now();
    }

    public Long getUserId() {
        return id.getUserId();
    }

    public Long getProductId() {
        return id.getProductId();
    }
}
