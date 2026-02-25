package com.loopers.domain.product;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Component
public class ProductService {

    private final ProductRepository productRepository;

    @Transactional(readOnly = true)
    public Product findById(Long id) {
        return productRepository.findById(id)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "상품을 찾을 수 없습니다."));
    }

    @Transactional(readOnly = true)
    public Page<Product> findAll(Long brandId, Pageable pageable) {
        return productRepository.findAll(brandId, pageable);
    }

    @Transactional
    public Product create(Long brandId, String name, String description, int price, int stock, SellingStatus sellingStatus) {
        Product product = new Product(brandId, name, description, price, stock, sellingStatus);
        return productRepository.save(product);
    }

    @Transactional
    public Product update(Long id, String name, String description, int price, SellingStatus sellingStatus) {
        Product product = findById(id);
        product.changeProductInfo(name, description, price, sellingStatus);
        return productRepository.save(product);
    }

    @Transactional
    public void delete(Long id) {
        Product product = findById(id);
        product.delete();
        productRepository.save(product);
    }
}
