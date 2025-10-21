package com.homesweet.homesweetback.domain.community.controller;

import com.homesweet.homesweetback.domain.helllo.dto.HelloCreateRequest;
import com.homesweet.homesweetback.domain.helllo.dto.HelloResponse;
import com.homesweet.homesweetback.domain.helllo.service.HelloService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/*
 * === How to verify custom logging & exception flow (Quick Check) ==========================
 *
 * 1) 정상 흐름
 *    - 요청
 *      POST /api/hello
 *      Body: {"name":"junwoo"}
 *    - 후속 조회
 *      GET  /api/hello/1
 *    - 기대 로그 (MVCLoggingAspect):
 *      Controller --> Service --> Repository 순서로
 *        "--> [Request] ..."  (진입)
 *        "<-- [Response] ..." (정상 반환, 경과 ms 포함)
 *      루트 호출(Controller) 기준으로 START / END 배너 로그 출력
 *
 * 2) 비즈니스 예외 흐름
 *    - 요청
 *      POST /api/hello
 *      Body: {"name":"금지어"}
 *    - 동작
 *      BusinessException(ErrorCode.BUSINESS_ERROR) 발생
 *    - 기대 로그/응답
 *      Aspect: "<-X- [Exception] ..." (예외 로그 + 스택트레이스)
 *      GlobalExceptionHandler: JSON 응답 { "status": 400, "message": "...", "timestamp": "ISO-8601" }
 *
 */


/**
 * Hello 컨트롤러(정상 흐름과 예외 흐름을 통해 로그 및 예외 처리를 확인할 수 있습니다)
 *
 * @author junnukim1007gmail.com
 * @date 25. 10. 18.
 */
@RestController
@RequestMapping("/api/hello")
@RequiredArgsConstructor
public class CommunityController {

    private final HelloService helloService;

    @PostMapping
    public ResponseEntity<HelloResponse> create(@Valid @RequestBody HelloCreateRequest req) {
        return ResponseEntity.ok(helloService.create(req));
    }
}

