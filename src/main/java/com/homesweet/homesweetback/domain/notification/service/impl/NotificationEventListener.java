package com.homesweet.homesweetback.domain.notification.service.impl;

import com.homesweet.homesweetback.domain.auth.entity.User;
import com.homesweet.homesweetback.domain.auth.service.UserService;
import com.homesweet.homesweetback.domain.notification.domain.NotificationCategoryType;
import com.homesweet.homesweetback.domain.notification.domain.event.CustomNotificationEvent;
import com.homesweet.homesweetback.domain.notification.domain.event.TemplateNotificationEvent;
import com.homesweet.homesweetback.domain.notification.domain.notification.CustomNotification;
import com.homesweet.homesweetback.domain.notification.domain.notification.TemplateNotification;
import com.homesweet.homesweetback.domain.notification.dto.PushNotificationDTO;
import com.homesweet.homesweetback.domain.notification.entity.NotificationTemplate;
import com.homesweet.homesweetback.domain.notification.entity.UserNotification;
import com.homesweet.homesweetback.domain.notification.service.SseService;

import io.opentelemetry.instrumentation.annotations.WithSpan;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 알림 이벤트 리스너
 * 
 * 이벤트 기반으로 알림을 비동기 처리합니다.
 * 
 * @author dogyungkim
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventListener {

  private final UserNotificationService userNotificationService;
  private final SseService sseService;
  private final UserService userService;

  /**
   * 템플릿 알림 이벤트 처리
   * 
   * 단일 사용자 또는 다수 사용자 모두 처리합니다.
   * TemplateNotification을 통해 DB에서 템플릿을 조회하고, Payload와 함께 알림을 전송합니다.
   */
  @WithSpan
  @Async("notificationTaskExecutor")
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
  public void handleTemplateNotificationEvent(TemplateNotificationEvent event) {
    log.info("템플릿 알림 이벤트 처리 시작: userIds={}, eventType={}", event.userIds(),
        event.notification().getEventType());

    // 1. 이벤트에서 알림 템플릿 타입 추출
    TemplateNotification notification = event.notification();

    // 2. 알림 템플릿 조회
    NotificationTemplate template = userNotificationService.getNotificationTemplate(notification.getEventType());

    log.info("템플릿 조회 완료: template={}", template);

    // 3. 템플릿을 사용하여 사용자화 된 알림 생성
    List<UserNotification> userNotifications = createUserNotification(event.userIds(), notification, template);

    // 4. 사용자화 된 알림을 DB에 저장(bulk + last_insert_id 활용 해서 id 계산)
    userNotificationService.bulkInsertUserNotifications(userNotifications);

    // 5. 사용자화 된 알림을 DTO로 변환
    Map<Long, PushNotificationDTO> pushNotificationDTOMap = convertToPushNotificationDTO(template, userNotifications);

    // 6. SSE 전송
    Map<Long, PushNotificationDTO> notificationMap = new HashMap<>(pushNotificationDTOMap.size());
    for (Map.Entry<Long, PushNotificationDTO> entry : pushNotificationDTOMap.entrySet()) {
      notificationMap.put(entry.getKey(), entry.getValue());
    }
    sseService.sendNotifications(notificationMap);
  }

  /**
   * 커스텀 알림 이벤트 처리
   */
  @WithSpan
  @Async("notificationTaskExecutor")
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
  public void handleCustomNotificationEvent(CustomNotificationEvent event) {
    log.info("커스텀 알림 이벤트 처리 시작: userIds={}, categoryType={}, title={}", event.userIds(),
        event.notification().getTitle(), event.notification().getContent());

    // 1. 이벤트에서 알림 정보 추출
    CustomNotification notification = event.notification();

    // 2. 커스텀 알림 템플릿 생성
    NotificationTemplate template = userNotificationService.createAndSaveCustomNotificationTemplate(
        notification.getTitle(),
        notification.getContent(),
        notification.getRedirectUrl());

    // 3. 알림 생성
    List<UserNotification> userNotifications = createUserNotification(event.userIds(), notification,
        template);

    // 4. 사용자화 된 알림을 DB에 저장(bulk + last_insert_id 활용 해서 id 계산)
    userNotificationService.bulkInsertUserNotifications(userNotifications);

    // 5. 사용자화 된 알림을 DTO로 변환
    Map<Long, PushNotificationDTO> pushNotificationDTOMap = convertToPushNotificationDTO(template,
        userNotifications);

    // 6. SSE 전송
    Map<Long, PushNotificationDTO> notificationMap = new HashMap<>(pushNotificationDTOMap.size());
    for (Map.Entry<Long, PushNotificationDTO> entry : pushNotificationDTOMap.entrySet()) {
      notificationMap.put(entry.getKey(), entry.getValue());
    }
    sseService.sendNotifications(notificationMap);
  }

  // 내부 메서드
  /**
   * 알림 정보를 활용해 사용자 알림 생성
   */

  private List<UserNotification> createUserNotification(
      List<Long> userIds,
      TemplateNotification notification,
      NotificationTemplate template) {
    // 2. 알림 객체 생성 (메모리)
    List<UserNotification> userNotifications = new ArrayList<>();
    Map<String, Object> notificationContextData = notification.toMap();
    List<User> users = userService.getManyUsersById(userIds);

    for (User user : users) {
      try {
        // 3. 사용자 알림 생성
        UserNotification userNotification = userNotificationService.createUserNotification(
            user,
            template,
            notificationContextData);
        userNotifications.add(userNotification);
      } catch (Exception e) {
        log.error("사용자 알림 객체 생성 실패: userId={}, error={}", user.getId(), e.getMessage(), e);
      }
    }
    return userNotifications;
  }

  /**
   * 알림 객체를 푸시 알림 DTO로 변환
   */

  private Map<Long, PushNotificationDTO> convertToPushNotificationDTO(
      NotificationTemplate template,
      List<UserNotification> userNotifications) {
    Map<Long, PushNotificationDTO> pushNotificationDTOMap = new HashMap<>(userNotifications.size());

    for (UserNotification userNotification : userNotifications) {
      try {
        PushNotificationDTO pushNotificationDTO = buildPushNotificationDTO(
            userNotification.getContextData(),
            template,
            userNotification.getId() // JDBC Insert로 할당된 ID 사용
        );
        pushNotificationDTOMap.put(userNotification.getUser().getId(), pushNotificationDTO);
      } catch (Exception e) {
        log.error("알림 DTO 생성 실패: userId={}, error={}", userNotification.getUser().getId(),
            e.getMessage(), e);
      }
    }
    return pushNotificationDTOMap;
  }

  private PushNotificationDTO buildPushNotificationDTO(
      Map<String, Object> contextData,
      NotificationTemplate template,
      Long notificationId) {
    return PushNotificationDTO.builder()
        .notificationId(notificationId)
        .title(template.getTitle())
        .content(template.getContent())
        .redirectUrl(template.getRedirectUrl())
        .contextData(contextData)
        .categoryType(NotificationCategoryType.fromCategoryId(template.getCategory().getId()))
        .isRead(false)
        .createdAt(LocalDateTime.now())
        .build();
  }
}
