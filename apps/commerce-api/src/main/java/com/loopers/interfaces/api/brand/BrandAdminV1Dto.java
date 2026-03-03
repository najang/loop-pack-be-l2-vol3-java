package com.loopers.interfaces.api.brand;

import com.loopers.application.brand.BrandInfo;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.domain.Page;

import java.util.List;

public class BrandAdminV1Dto {

    public record CreateRequest(
        @NotBlank(message = "브랜드명은 비어있을 수 없습니다.")
        String name,
        String description
    ) {}

    public record UpdateRequest(
        @NotBlank(message = "브랜드명은 비어있을 수 없습니다.")
        String name,
        String description
    ) {}

    public record BrandResponse(Long id, String name, String description) {
        public static BrandResponse from(BrandInfo info) {
            return new BrandResponse(info.id(), info.name(), info.description());
        }
    }

    public record BrandPageResponse(List<BrandResponse> content, int page, int size, long totalElements) {
        public static BrandPageResponse from(Page<BrandInfo> page) {
            List<BrandResponse> content = page.getContent().stream()
                .map(BrandResponse::from)
                .toList();
            return new BrandPageResponse(content, page.getNumber(), page.getSize(), page.getTotalElements());
        }
    }
}
