package com.loopers.application.product;

import java.util.List;

/**
 * Page<ProductInfo>는 PageImpl 역직렬화 이슈로 Redis에 직접 저장할 수 없어,
 * 캐시 직렬화/역직렬화용 래퍼 record로 분리한다.
 * 복원 시: new PageImpl<>(entry.content(), pageable, entry.totalElements())
 */
public record ProductListCacheEntry(List<ProductInfo> content, long totalElements) {
}
