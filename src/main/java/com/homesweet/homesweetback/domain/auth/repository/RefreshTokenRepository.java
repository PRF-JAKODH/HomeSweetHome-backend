package com.homesweet.homesweetback.domain.auth.repository;

//사용자 Email key : RT (Refresh Token) value
public interface RefreshTokenRepository {

    boolean save(String email, String refreshToken);
    String findByEmail(String email);
    void deleteByEmail(String email);
}