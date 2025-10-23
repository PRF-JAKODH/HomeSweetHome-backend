//payment엔티티를 다루는 레포지토리.
package com.homesweet.homesweetback.domain.order.repository;

import com.homesweet.homesweetback.domain.order.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    //결제 검증 로직이 모두 성공했을 때, 그 '성공 기록'을 남기기 위해 단 한 번 생성(INSERT)됨.
    //지금 당장 payment테이블은 저장 외에 복잡한 조건으로 조회할 일이 없기 때문에 커스텀 메서드가 필요없음.
}
