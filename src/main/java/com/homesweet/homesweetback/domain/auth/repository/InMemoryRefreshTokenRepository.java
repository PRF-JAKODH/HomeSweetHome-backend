package com.homesweet.homesweetback.domain.auth.repository;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

import org.springframework.stereotype.Repository;

@Repository
public class InMemoryRefreshTokenRepository implements RefreshTokenRepository {

    private final Map<String, String> refreshTokenMap = new ConcurrentHashMap<>();

    @Override
    public boolean save(String email, String refreshToken) {
        refreshTokenMap.put(email, refreshToken);
        return true;
    }

    @Override
    public String findByEmail(String email) {
        return refreshTokenMap.get(email);
    }

    @Override
    public void deleteByEmail(String email) {
        refreshTokenMap.remove(email);
    }
}