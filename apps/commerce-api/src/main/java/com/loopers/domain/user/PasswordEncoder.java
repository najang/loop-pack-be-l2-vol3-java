package com.loopers.domain.user;

public interface PasswordEncoder {
    /**
     * 평문 비밀번호를 인코딩한다.
     * @param rawPassword 평문 비밀번호 (null 불가)
     * @return 인코딩된 비밀번호
     * @throws IllegalArgumentException rawPassword가 null인 경우
     */
    String encode(String rawPassword);

    /**
     * 평문 비밀번호와 인코딩된 비밀번호가 일치하는지 검증한다.
     * @param rawPassword 평문 비밀번호 (null 불가)
     * @param encodedPassword 인코딩된 비밀번호 (null 불가)
     * @return 일치 여부
     * @throws IllegalArgumentException 파라미터가 null인 경우
     */
    boolean matches(String rawPassword, String encodedPassword);
}
