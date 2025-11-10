package com.homesweet.homesweetback.domain.chat.service.Imp;


import com.homesweet.homesweetback.domain.auth.entity.User;
import com.homesweet.homesweetback.domain.auth.repository.UserRepository;
import com.homesweet.homesweetback.domain.chat.dto.ChatMessageDto;
import com.homesweet.homesweetback.domain.chat.dto.response.ChatMessageSendResponse;
import com.homesweet.homesweetback.domain.chat.dto.response.PreMessageResponse;
import com.homesweet.homesweetback.domain.chat.entity.ChatMessage;
import com.homesweet.homesweetback.domain.chat.entity.ChatRoom;
import com.homesweet.homesweetback.domain.chat.entity.RoomMember;
import com.homesweet.homesweetback.domain.chat.entity.enums.MessageType;
import com.homesweet.homesweetback.domain.chat.repository.ChatMessageRepository;
import com.homesweet.homesweetback.domain.chat.repository.ChatRoomRepository;
import com.homesweet.homesweetback.domain.chat.repository.RoomMemberRepository;
import com.homesweet.homesweetback.domain.chat.service.ChatMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;


@Slf4j
@Service
@RequiredArgsConstructor
public class ChatMessageServiceImpl implements ChatMessageService {

    private final ChatMessageRepository chatMessageRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final RoomMemberRepository roomMemberRepository;
    private final UserRepository userRepository;


    /**
     * 메시지 저장 (기존 로직 활용)
     */
    @Override
    @Transactional
    public ChatMessageSendResponse sendMessage(Long roomId, Long senderId, String content) {

        // 채팅방 조회
        ChatRoom chatRoom = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 채팅방입니다."));

        // 발신자 조회
        RoomMember sender = roomMemberRepository.findByUserIdAndRoomId(senderId, roomId);
        if (sender == null || Boolean.TRUE.equals(sender.getIsExit())) {
            throw new IllegalStateException("채팅방 멤버가 아니거나 이미 퇴장한 사용자입니다.");
        }

        //  트랜잭션 내부에서 User 정보 미리 가져오기
        User user = sender.getUser();
        String senderName = user.getName();
        String senderProfileImg = user.getProfileImageUrl();

        // 채팅 메시지 저장
        ChatMessage message = ChatMessage.builder()
                .room(chatRoom)
                .messageType(MessageType.TEXT)
                .content(content)
                .sender(user)
                .build();

        ChatMessage savedMessage = chatMessageRepository.save(message);

        chatRoom.updateLastMessage(content, savedMessage.getSentAt());

        chatRoomRepository.save(chatRoom);

        log.info("메시지 저장 완료 - message: {}", savedMessage);

        return ChatMessageSendResponse.from(
                savedMessage,
                roomId,
                senderId,
                senderName,
                senderProfileImg,
                senderId
        );
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

    @Override
    public boolean canSendMessage(Long userId, Long roomId) {
        RoomMember roomMember = roomMemberRepository.findByUserIdAndRoomId(userId, roomId);
        if (roomMember == null) return false;
        if (Boolean.TRUE.equals(roomMember.getIsExit())) return false;
        return true;
    }

}
