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
 * like_count는 Kafka 이벤트 소비 후 비동기로 갱신된다 (Eventual Consistency).
 * commerce-api: 읽기 전용 조회 (likeCount 표시용)
 * commerce-streamer: 쓰기 (Kafka Consumer가 upsert)
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
}
