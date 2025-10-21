package com.homesweet.homesweetback.domain.community.service;

import com.homesweet.homesweetback.common.exception.ErrorCode;
import com.homesweet.homesweetback.domain.helllo.dto.HelloCreateRequest;
import com.homesweet.homesweetback.domain.helllo.dto.HelloResponse;
import com.homesweet.homesweetback.domain.helllo.repository.HelloEntity;
import com.homesweet.homesweetback.domain.helllo.repository.HelloRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Hello 서비스
 *
 * @author junnukim1007gmail.com
 * @date 25. 10. 18.
 */
@Service
@RequiredArgsConstructor
public class CommunityService {

    private final HelloRepository helloRepository;

    public HelloResponse create(HelloCreateRequest req) {
        // 간단 비즈니스 규칙 예시
        if (req.name().contains("금지")) {
            throw new CommunityException(ErrorCode.HELLO_SAMPLE_ERROR, "name에 '금지'는 사용할 수 없습니다.");
        }
        if (helloRepository.existsByName(req.name())) {
            throw new CommunityException(ErrorCode.HELLO_SAMPLE_ERROR, "이미 존재하는 name 입니다.");
        }

        HelloEntity saved = helloRepository.save(req.name());
        return new HelloResponse(saved.getId(), "Hello, " + saved.getName() + "!");
    }
}
