package com.homesweet.homesweetback.domain.chat.controller;

import org.springframework.stereotype.Repository;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

@RestController
@RequestMapping("/api/rooms")
public class RoomController {
    // 두사람의 조합으로 방번호 저장
    private final Map<String, Long> pairToRoom = new ConcurrentHashMap<>();
    // 방번호 시퀀스
    private final AtomicLong seq = new AtomicLong(1);

    private String keyOf(long a, long b) {
        return (a < b) ? (a + ":" + b) : (b + ":" + a);
    }

    @GetMapping("/individual")
    public Map<String, Long> createOrGet(@RequestParam Long me,@RequestParam Long targetId) {
        String key = keyOf(me, targetId);
        Long roomId = pairToRoom.get(key);

        if (roomId == null) {
            roomId = seq.getAndIncrement();
            pairToRoom.put(key, roomId);
        }
        return Map.of("roomId", roomId);
    }

}
