package com.loopers.domain.user;

import com.loopers.domain.BaseEntity;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

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

    protected UserModel() {
    }

    public UserModel(String loginId, String encodedPassword, String name, LocalDate birthDate, String email) {
        validateBirthDate(birthDate);

        this.loginId = new LoginId(loginId);
        this.password = new EncodedPassword(encodedPassword);
        this.name = new UserName(name);
        this.birthDate = birthDate;
        this.email = new Email(email);
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

    public void changePassword(String newEncodedPassword) {
        this.password = new EncodedPassword(newEncodedPassword);
    }

    private void validateBirthDate(LocalDate birthDate) {
        if (birthDate == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "생년월일은 비어있을 수 없습니다.");
        }
    }
}
