package com.loopers.domain.cart;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.brand.BrandService;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductService;
import com.loopers.domain.product.SellingStatus;
import com.loopers.domain.user.UserModel;
import com.loopers.infrastructure.user.UserJpaRepository;
import com.loopers.utils.ConcurrencyTestHelper;
import com.loopers.utils.ConcurrencyTestHelper.ConcurrencyResult;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class CartConcurrencyTest {

    private static final int THREAD_COUNT = 100;

    @Autowired
    private CartService cartService;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private ProductService productService;

    @Autowired
    private BrandService brandService;

    @Autowired
    private UserJpaRepository userJpaRepository;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @DisplayName("동일 유저가 같은 상품에 동시에 addQuantity(1)을 100번 요청하면, 1건만 성공하고 최종 수량은 유실되지 않는다.")
    @Test
    void concurrentAddQuantity_onlyOneSucceedsAndQuantityIsCorrect() throws Exception {
        // arrange
        Brand brand = brandService.create("Nike", null);
        Product product = productService.create(brand.getId(), "운동화", null, 10000, 100, SellingStatus.SELLING);
        Long productId = product.getId();

        UserModel user = userJpaRepository.save(new UserModel(
            "cartuser", "encoded", "장바구니유저", LocalDate.of(1990, 1, 1), "cartuser@test.com"
        ));
        Long userId = user.getId();

        // 초기 장바구니 생성 (수량 1)
        cartService.add(userId, productId, 1);

        // act
        ConcurrencyResult result = ConcurrencyTestHelper.run(THREAD_COUNT, () -> cartService.add(userId, productId, 1));

        // assert - 최종 수량 = 초기 수량 + 성공한 요청 수
        Cart cart = cartRepository.findByUserIdAndProductId(userId, productId).orElseThrow();
        assertThat(cart.getQuantity()).isEqualTo(1+result.successCount());
        assertThat(result.successCount()+result.failureCount()).isEqualTo(THREAD_COUNT);
    }
}
