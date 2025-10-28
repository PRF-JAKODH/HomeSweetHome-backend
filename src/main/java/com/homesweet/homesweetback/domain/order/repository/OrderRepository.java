//OrderService(비즈니스 로직)가 Order(엔티티)를 DB에 저장하거나 조회해야 할 때, Service가 직접 SQL 쿼리문을 짜지 않기 위해 만듦.
package com.homesweet.homesweetback.domain.order.repository;
//package: 이 코드가 속한 폴더 경로를 지정하는 키워드

import com.homesweet.homesweetback.domain.order.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
//Spring Data JPA가 제공하는 핵심 인터페이스. 별도의 구현 코드 없이도 CRUD 메서드 사용가능
import java.util.Optional;
//값이 있을 수도 있고, 없을 수도 있다는 것을 명시적으로 나타내는 래퍼(Wrapper)클래스.
//null체크를 강제하여 NullPointerException 발생 위험을 줄이는 모범 사례(?)

//스프링 빈
public interface OrderRepository extends JpaRepository<Order, Long> {
    //스프링 데이터 JPA는 interface를 보고 런타임에 구현 클래스를 자동 생성해줌.
    //extends: 인터페이스가 다른 인터페이스의 기능을 상속받을 때 사용하는 키워드.
    //JpaRepository<Order, Long> 을 extends(상속)하는 순간, CRUD기능을 자동으로 얻음.
//    Optional<Order> findByMerchantUid(String merchantUid);
    //Spring Data JPA가 이 메서드를 보고 merchant_uid컬럼으로 SELECT 쿼리를 만들어 줌.
}