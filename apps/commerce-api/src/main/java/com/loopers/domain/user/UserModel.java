package com.loopers.domain.user;

import com.loopers.domain.BaseEntity;
import com.loopers.domain.vo.Money;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.LocalDate;

@Entity
@Table(name = "users")
public class UserModel extends BaseEntity {

    @Embedded
    private LoginId loginId;

    @Embedded
    private EncodedPassword password;

    @Embedded
    private UserName name;

    @Column(name = "birth_date", nullable = false)
    private LocalDate birthDate;

    @Embedded
    private Email email;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "point_balance", nullable = false))
    private Money pointBalance;

    @Version
    private Long version;

    protected UserModel() {
    }

    public UserModel(String loginId, String encodedPassword, String name, LocalDate birthDate, String email) {
        validateBirthDate(birthDate);

        this.loginId = new LoginId(loginId);
        this.password = new EncodedPassword(encodedPassword);
        this.name = new UserName(name);
        this.birthDate = birthDate;
        this.email = new Email(email);
        this.pointBalance = new Money(0);
    }

    public String getLoginId() {
        return loginId.getValue();
    }

    public String getPassword() {
        return password.getValue();
    }

    public String getName() {
        return name.getValue();
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public String getEmail() {
        return email.getValue();
    }

    public String getMaskedName() {
        return name.masked();
    }

    public int getPointBalance() {
        return pointBalance.getValue();
    }

    public void chargePoints(int amount) {
        if (amount <= 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "충전 금액은 0보다 커야 합니다.");
        }
        this.pointBalance = new Money(this.pointBalance.getValue() + amount);
    }

    public void deductPoints(int amount) {
        if (amount <= 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "차감 금액은 0보다 커야 합니다.");
        }
        if (this.pointBalance.getValue() < amount) {
            throw new CoreException(ErrorType.BAD_REQUEST, "포인트 잔액이 부족합니다.");
        }
        this.pointBalance = new Money(this.pointBalance.getValue() - amount);
    }

    public void refundPoints(int amount) {
        if (amount <= 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "환불 금액은 0보다 커야 합니다.");
        }
        this.pointBalance = new Money(this.pointBalance.getValue() + amount);
    }

    public void changePassword(String newEncodedPassword) {
        this.password = new EncodedPassword(newEncodedPassword);
    }

    private void validateBirthDate(LocalDate birthDate) {
        if (birthDate == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "생년월일은 비어있을 수 없습니다.");
        }
    }
}
