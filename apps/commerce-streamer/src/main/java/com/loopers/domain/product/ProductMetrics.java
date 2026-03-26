package com.loopers.domain.product;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;

/**
 * 상품 집계 지표 (product_metrics).
 * commerce-streamer: Kafka 이벤트 소비 후 like_count를 upsert한다.
 */
@Getter
@Entity
@Table(name = "product_metrics")
public class ProductMetrics {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    private Long version;

    @Column(name = "product_id", nullable = false, unique = true)
    private Long productId;

    @Column(name = "like_count", nullable = false)
    private int likeCount;

    @Column(name = "order_count", nullable = false)
    private int orderCount;

    protected ProductMetrics() {
    }

    public ProductMetrics(Long productId, int likeCount) {
        this.productId = productId;
        this.likeCount = likeCount;
        this.orderCount = 0;
    }

    public ProductMetrics(Long productId, int likeCount, int orderCount) {
        this.productId = productId;
        this.likeCount = likeCount;
        this.orderCount = orderCount;
    }

    public void increaseLikeCount() {
        this.likeCount++;
    }

    public void decreaseLikeCount() {
        if (this.likeCount > 0) {
            this.likeCount--;
        }
    }

    public void increaseOrderCount() {
        this.orderCount++;
    }
}
