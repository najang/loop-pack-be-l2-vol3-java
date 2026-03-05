package com.loopers.infrastructure.coupon;

import com.loopers.domain.coupon.UserCoupon;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.ZonedDateTime;
import java.util.List;

public interface UserCouponJpaRepository extends JpaRepository<UserCoupon, Long> {

    List<UserCoupon> findByUserIdAndDeletedAtIsNull(Long userId);

    Page<UserCoupon> findByCouponTemplateIdAndDeletedAtIsNull(Long couponTemplateId, Pageable pageable);

    boolean existsByUserIdAndCouponTemplateIdAndDeletedAtIsNull(Long userId, Long couponTemplateId);

    @Modifying
    @Query("UPDATE UserCoupon uc SET uc.status = 'USED', uc.usedAt = :usedAt " +
           "WHERE uc.id = :id AND uc.status = 'AVAILABLE' AND uc.deletedAt IS NULL")
    int useIfAvailable(@Param("id") Long id, @Param("usedAt") ZonedDateTime usedAt);
}
