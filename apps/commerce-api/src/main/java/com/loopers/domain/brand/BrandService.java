package com.loopers.domain.brand;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Component
public class BrandService {

    private final BrandRepository brandRepository;

    @Transactional(readOnly = true)
    public Brand findById(Long id) {
        return brandRepository.findById(id)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "브랜드를 찾을 수 없습니다."));
    }

    @Transactional(readOnly = true)
    public Page<Brand> findAll(Pageable pageable) {
        return brandRepository.findAll(pageable);
    }

    @Transactional
    public Brand create(String name, String description) {
        Brand brand = new Brand(name, description);
        return brandRepository.save(brand);
    }

    @Transactional
    public Brand update(Long id, String name, String description) {
        Brand brand = findById(id);
        brand.changeBrandInfo(name, description);
        return brandRepository.save(brand);
    }

    @Transactional
    public void delete(Long id) {
        Brand brand = findById(id);
        brand.delete();
        brandRepository.save(brand);
    }
}
