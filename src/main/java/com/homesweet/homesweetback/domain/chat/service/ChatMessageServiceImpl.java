package com.homesweet.homesweetback.domain.chat.service;


import com.homesweet.homesweetback.domain.auth.repository.UserRepository;
import com.homesweet.homesweetback.domain.chat.dto.response.ChatMessageResponse;
import com.homesweet.homesweetback.domain.chat.entity.ChatMessage;
import com.homesweet.homesweetback.domain.chat.entity.ChatRoom;
import com.homesweet.homesweetback.domain.chat.entity.RoomMember;
import com.homesweet.homesweetback.domain.chat.entity.enums.MessageType;
import com.homesweet.homesweetback.domain.chat.repository.ChatMessageRepository;
import com.homesweet.homesweetback.domain.chat.repository.ChatRoomRepository;
import com.homesweet.homesweetback.domain.chat.repository.RoomMemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatMessageServiceImpl implements ChatMessageService {

    private final ChatMessageRepository chatMessageRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final RoomMemberRepository roomMemberRepository;

    /**
     * 채팅 메시지 전송 및 저장
     */
    @Override
    @Transactional
    public ChatMessageResponse sendMessage(Long roomId, Long senderId, String content) {

        // 채팅방 조회
        ChatRoom chatRoom = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 채팅방입니다."));

        log.debug("방번호 조회됨? " + roomId);

        // 발신자 조회
        RoomMember sender = roomMemberRepository.findByUserIdAndRoomId(senderId, roomId);

        log.debug("발신자 조회됨? " + senderId);

        // 채팅 메시지 저장
        ChatMessage message = new ChatMessage(
                chatRoom,
                MessageType.TEXT,
                content,
                null,
                sender.getUser()
        );

        log.debug("메세지 전송됨? " + message);


        ChatMessage savedMessage = chatMessageRepository.save(message);

        return ChatMessageResponse.from(savedMessage, senderId);
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

}
