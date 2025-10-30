package com.homesweet.homesweetback.domain.notification.domain.payload;

import com.homesweet.homesweetback.domain.notification.domain.NotificationEventType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Payload 클래스가 지원하는 EventType을 지정하는 어노테이션
 * 
 * @author dogyungkim
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface SupportsEventType {
    NotificationEventType value();
}
