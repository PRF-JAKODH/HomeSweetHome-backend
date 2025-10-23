package com.homesweet.homesweetback.domain.order.service;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OrderServiceTest2 {


    @Test
    public void test2() {
        int a=3;

        //값을 검증 or 기대하는 동작, 예외 확인
        Assertions.assertThat(a).isEqualTo(4);
    }
}