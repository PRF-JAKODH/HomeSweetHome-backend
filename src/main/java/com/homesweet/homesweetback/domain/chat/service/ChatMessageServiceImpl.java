package com.homesweet.homesweetback.domain.chat.service;


import com.homesweet.homesweetback.domain.auth.repository.UserRepository;
import com.homesweet.homesweetback.domain.chat.dto.ChatMessageDto;
import com.homesweet.homesweetback.domain.chat.dto.response.ChatMessageResponse;
import com.homesweet.homesweetback.domain.chat.dto.response.PreMessageResponse;
import com.homesweet.homesweetback.domain.chat.entity.ChatMessage;
import com.homesweet.homesweetback.domain.chat.entity.ChatRoom;
import com.homesweet.homesweetback.domain.chat.entity.RoomMember;
import com.homesweet.homesweetback.domain.chat.entity.enums.MessageType;
import com.homesweet.homesweetback.domain.chat.repository.ChatMessageRepository;
import com.homesweet.homesweetback.domain.chat.repository.ChatRoomRepository;
import com.homesweet.homesweetback.domain.chat.repository.RoomMemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static com.homesweet.homesweetback.domain.chat.entity.QChatMessage.chatMessage;


@Slf4j
@Service
@RequiredArgsConstructor
public class ChatMessageServiceImpl implements ChatMessageService {

    private final ChatMessageRepository chatMessageRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final RoomMemberRepository roomMemberRepository;
    private final UserRepository userRepository;

    /**
     * 채팅 메시지 전송 및 저장
     */
    @Override
    @Transactional
    public ChatMessageResponse sendMessage(Long roomId, Long senderId, String content) {

        // 채팅방 조회
        ChatRoom chatRoom = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 채팅방입니다."));

        // 발신자 조회
        log.info("💬 [메시지 전송 요청] senderId={}, roomId={}", senderId, roomId);
        RoomMember sender = roomMemberRepository.findByUserIdAndRoomId(senderId, roomId);
        log.info("📌 RoomMember 조회 결과: {}", sender);        // ws세션은 끊지않으므로 퇴장자 검증 필요
        if (sender == null || sender.getIsExit() == true) {
            throw new IllegalStateException("🚫 채팅방 멤버가 아니거나 이미 퇴장한 사용자입니다.");
        }

        // 채팅 메시지 저장
        ChatMessage message = ChatMessage.builder()
                .room(chatRoom)
                .messageType(MessageType.TEXT)
                .content(content)
                .sender(sender.getUser())
                .build();

        ChatMessage savedMessage = chatMessageRepository.save(message);

        log.info("메세지 저장 완료 - message: {}", savedMessage);

        return ChatMessageResponse.from(savedMessage, senderId);
    }

    /*
    * 이전 메세지 조회 (채팅방 입장 or 스크롤)
    * */
    @Override
    public PreMessageResponse getPreMessage(Long roomId, Long lastMessageId, int size) {

        log.info(" 메시지 조회 시작 - roomId: {}, lastMessageId: {}, size: {}",
                roomId, lastMessageId, size);

        Slice<ChatMessage> slice;

        if(lastMessageId == null) {
            // 최초 로드 시
            slice = chatMessageRepository.findByRoomIdOrderBySentAtDesc(
            roomId, PageRequest.of(0, size)
                    );
        } else {
            // 추가 로드 요청 시
            slice = chatMessageRepository.findOlderMessages(
                    roomId, lastMessageId, PageRequest.of(0, size)
            );
        }

        // DTO 변환
        List<ChatMessageDto> messageDtos = slice.getContent().stream()
                .map(entity -> ChatMessageDto.from(
                        entity,
                        entity.getSender().getName(),
                        entity.getSender().getProfileImageUrl()
                ))
                .collect(Collectors.toList());

        // 오래된 메시지 → 최신 메시지 순으로 뒤집기
        Collections.reverse(messageDtos);

        // 다음 페이지 존재 여부 함께 반환
        return PreMessageResponse.of(messageDtos, slice.hasNext());
    }


    /**
     * 메시지 읽음 처리 (추후)
     */
    @Override
    @Transactional
    public void markAsRead(Long roomId, Long userId, Long lastReadMessageId) {

        // 채팅방 멤버 조회 (한 번의 쿼리로 존재 여부와 정보를 모두 확인)
        RoomMember roomMember = roomMemberRepository.findByRoomIdAndUserId(roomId, userId)
                .orElseThrow(() -> new IllegalStateException("채팅방 멤버가 아닙니다."));

        // 마지막 읽은 메시지 ID 업데이트
        roomMember.updateLastReadMessageId(lastReadMessageId);
    }

    @Override
    public void checkMember(Long subRoomId, Long subUser) {

    }

    @Override
    public boolean canSendMessage(Long userId, Long roomId) {
        RoomMember roomMember = roomMemberRepository.findByUserIdAndRoomId(userId, roomId);
        if (roomMember == null) return false;
        if (Boolean.TRUE.equals(roomMember.getIsExit())) return false;
        return true;
    }

}
