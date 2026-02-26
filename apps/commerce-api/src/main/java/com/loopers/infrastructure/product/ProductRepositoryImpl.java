package com.loopers.infrastructure.product;

import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@Component
public class ProductRepositoryImpl implements ProductRepository {

    private final ProductJpaRepository productJpaRepository;

    @Override
    public Optional<Product> findById(Long id) {
        return productJpaRepository.findByIdAndDeletedAtIsNull(id);
    }

    @Override
    public Page<Product> findAll(Long brandId, Pageable pageable) {
        if (brandId == null) {
            return productJpaRepository.findAllByDeletedAtIsNull(pageable);
        }
        return productJpaRepository.findAllByBrandIdAndDeletedAtIsNull(brandId, pageable);
    }

    @Override
    public List<Product> findByBrandId(Long brandId) {
        return productJpaRepository.findByBrandIdAndDeletedAtIsNull(brandId);
    }

    @Override
    public List<Product> findAllByIds(List<Long> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }
        return productJpaRepository.findByIdInAndDeletedAtIsNull(ids);
    }

    @Override
    public Product save(Product product) {
        return productJpaRepository.save(product);
    }
}
