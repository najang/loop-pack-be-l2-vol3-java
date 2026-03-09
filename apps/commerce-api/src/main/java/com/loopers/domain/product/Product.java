package com.loopers.domain.product;

import com.loopers.domain.BaseEntity;
import com.loopers.domain.vo.LikeCount;
import com.loopers.domain.vo.Money;
import com.loopers.domain.vo.Stock;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;

@Getter
@Entity
@Table(name = "products")
public class Product extends BaseEntity {

    @Column(name = "brand_id", nullable = false)
    private Long brandId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description")
    private String description;

    @Embedded
    @Getter(AccessLevel.NONE)
    private Money price;

    @Embedded
    @Getter(AccessLevel.NONE)
    private Stock stock;

    @Enumerated(EnumType.STRING)
    @Column(name = "selling_status", nullable = false)
    private SellingStatus sellingStatus;

    @Embedded
    @Getter(AccessLevel.NONE)
    private LikeCount likeCount;

    @Version
    @Column(name = "version")
    private Long version;

    protected Product() {
    }

    public Product(Long brandId, String name, String description, int price, int stock, SellingStatus sellingStatus) {
        if (brandId == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "브랜드 ID는 필수입니다.");
        }
        if (name == null || name.isBlank()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "상품명은 비어있을 수 없습니다.");
        }
        this.brandId = brandId;
        this.name = name;
        this.description = description;
        this.price = new Money(price);
        this.stock = new Stock(stock);
        this.sellingStatus = sellingStatus;
        this.likeCount = new LikeCount(0);
    }

    public int getPrice() {
        return price.getValue();
    }

    public int getStock() {
        return stock.getValue();
    }

    public int getLikeCount() {
        return likeCount.getValue();
    }

    public boolean canOrder() {
        return sellingStatus == SellingStatus.SELLING && stock.getValue() > 0;
    }

    public void deductStock(int quantity) {
        this.stock = stock.deduct(quantity);
    }

    public void restoreStock(int quantity) {
        this.stock = stock.restore(quantity);
    }

    public void increaseLikes() {
        this.likeCount = likeCount.increase();
    }

    public void decreaseLikes() {
        this.likeCount = likeCount.decrease();
    }

    public void changeProductInfo(String name, String description, int price, SellingStatus sellingStatus) {
        if (name == null || name.isBlank()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "상품명은 비어있을 수 없습니다.");
        }
        this.name = name;
        this.description = description;
        this.price = new Money(price);
        this.sellingStatus = sellingStatus;
    }
}
