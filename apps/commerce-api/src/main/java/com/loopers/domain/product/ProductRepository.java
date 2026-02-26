package com.loopers.domain.product;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface ProductRepository {
    Optional<Product> findById(Long id);

    Optional<Product> findByIdWithLock(Long id);

    Page<Product> findAll(Long brandId, Pageable pageable);

    List<Product> findByBrandId(Long brandId);

    List<Product> findAllByIds(List<Long> ids);

    Product save(Product product);
}
