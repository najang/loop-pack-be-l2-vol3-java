package com.loopers.domain.product;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface ProductRepository {
    Optional<Product> findById(Long id);

    Page<Product> findAll(Long brandId, Pageable pageable);

    Product save(Product product);
}
