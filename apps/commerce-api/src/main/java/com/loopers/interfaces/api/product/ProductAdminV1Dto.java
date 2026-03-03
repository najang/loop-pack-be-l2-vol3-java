package com.loopers.interfaces.api.product;

import com.loopers.application.product.ProductInfo;
import com.loopers.domain.product.SellingStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.domain.Page;

import java.util.List;

public class ProductAdminV1Dto {

    public record CreateRequest(
        @NotNull(message = "브랜드 ID는 필수입니다.")
        Long brandId,
        @NotBlank(message = "상품명은 비어있을 수 없습니다.")
        String name,
        String description,
        @Min(value = 0, message = "가격은 0 이상이어야 합니다.")
        int price,
        @Min(value = 0, message = "재고는 0 이상이어야 합니다.")
        int stock,
        @NotNull(message = "판매 상태는 필수입니다.")
        SellingStatus sellingStatus
    ) {}

    public record UpdateRequest(
        @NotBlank(message = "상품명은 비어있을 수 없습니다.")
        String name,
        String description,
        @Min(value = 0, message = "가격은 0 이상이어야 합니다.")
        int price,
        @NotNull(message = "판매 상태는 필수입니다.")
        SellingStatus sellingStatus
    ) {}

    public record ProductResponse(
        Long id,
        Long brandId,
        String name,
        String description,
        int price,
        int stock,
        String sellingStatus,
        int likeCount
    ) {
        public static ProductResponse from(ProductInfo info) {
            return new ProductResponse(
                info.id(),
                info.brandId(),
                info.name(),
                info.description(),
                info.price(),
                info.stock(),
                info.sellingStatus().name(),
                info.likeCount()
            );
        }
    }

    public record ProductPageResponse(
        List<ProductResponse> content,
        int page,
        int size,
        long totalElements
    ) {
        public static ProductPageResponse from(Page<ProductInfo> page) {
            List<ProductResponse> content = page.getContent().stream()
                .map(ProductResponse::from)
                .toList();
            return new ProductPageResponse(content, page.getNumber(), page.getSize(), page.getTotalElements());
        }
    }
}
