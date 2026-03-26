package com.loopers.application.like;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.brand.BrandService;
import com.loopers.domain.like.LikeRepository;
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
import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class LikeConcurrencyTest {

    private static final int THREAD_COUNT = 100;

    @Autowired
    private LikeApplicationService likeApplicationService;

    @Autowired
    private ProductService productService;

    @Autowired
    private LikeRepository likeRepository;

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

    @DisplayName("서로 다른 유저 N명이 동시에 좋아요를 누르면, 모두 성공하고 Like 레코드가 N개 저장된다.")
    @Test
    void concurrentLike_allSucceedAndLikeRecordsAreN() throws Exception {
        // arrange
        Brand brand = brandService.create("Nike", null);
        Product product = productService.create(brand.getId(), "운동화", null, 10000, 100, SellingStatus.SELLING);
        Long productId = product.getId();

        List<Long> userIds = IntStream.range(0, THREAD_COUNT)
            .mapToObj(i -> {
                UserModel user = userJpaRepository.save(new UserModel(
                    "likeuser" + i, "encoded", "좋아요유저" + i, LocalDate.of(1990, 1, 1), "likeuser" + i + "@test.com"
                ));
                return user.getId();
            })
            .toList();

        // act
        List<Callable<Object>> tasks = IntStream.range(0, THREAD_COUNT)
            .<Callable<Object>>mapToObj(i -> () -> {
                likeApplicationService.like(userIds.get(i), productId);
                return null;
            })
            .toList();
        ConcurrencyResult result = ConcurrencyTestHelper.run(tasks);

        // assert
        assertThat(result.successCount()).isEqualTo(THREAD_COUNT);
        assertThat(result.failureCount()).isEqualTo(0);
        // likeCount는 비동기로 product_metrics에 반영되므로, 각 유저의 Like 레코드 저장 여부로 검증한다.
        assertThat(likeRepository.findByUserId(userIds.get(0))).hasSize(1);
        assertThat(likeRepository.findByUserId(userIds.get(THREAD_COUNT - 1))).hasSize(1);
    }
}
