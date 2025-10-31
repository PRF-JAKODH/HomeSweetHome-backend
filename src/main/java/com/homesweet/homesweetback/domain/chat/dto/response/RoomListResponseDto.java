package com.homesweet.homesweetback.domain.chat.dto.response;

import lombok.Builder;

import java.time.LocalDateTime;

/**
 * 채팅방 목록 조회 시 클라이언트에게 반환할 응답 DTO
 *
 * [ResponseDto란?]
 * - 서버 → 클라이언트로 보내는 데이터 형식
 * - Entity를 그대로 노출하면 위험 (비밀번호 등 민감정보 포함 가능)
 * - 필요한 필드만 선별하여 전달
 *
 * [record vs class]
 * - record: 불변 객체, getter 자동 생성, 간결함 (Java 14+)
 * - class: 가변 객체, setter 필요 시 사용
 *
 * [JSON 변환 예시]
 * {
 *   "roomId": 123,
 *   "partnerId": 456,
 *   "partnerName": "김철수",
 *   "thumbnailUrl": "https://...",
 *   "lastMessage": "안녕하세요",
 *   "lastMessageAt": "2025-10-30T14:30:00",
 *   "lastMessageId": 789,
 *   "lastMessageIsRead": false
 * }
 */

@Builder
public record RoomListResponseDto (

    // 채팅방 기본 정보
    Long roomId,                    // 채팅방 ID

    // 상대방 정보
    Long partnerId,                 // 상대방 User ID (프로필 조회 시 사용)
    String partnerName,             // 상대방 이름 (화면에 표시)
    String thumbnailUrl,            // 상대방 프로필 이미지 URL

    // 마지막 메시지 정보
    String lastMessage,             // 마지막 메시지 내용
    LocalDateTime lastMessageAt,    // 마지막 메시지 전송 시간
    Long lastMessageId,             // 마지막 메시지 ID
    Boolean lastMessageIsRead       // 읽음 여부
) { }
