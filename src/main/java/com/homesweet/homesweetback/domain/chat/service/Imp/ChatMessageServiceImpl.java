package com.homesweet.homesweetback.domain.chat.service.Imp;


import com.homesweet.homesweetback.common.exception.BusinessException;
import com.homesweet.homesweetback.common.exception.ErrorCode;
import com.homesweet.homesweetback.domain.auth.entity.User;
import com.homesweet.homesweetback.domain.chat.dto.ChatMessageDto;
import com.homesweet.homesweetback.domain.chat.dto.response.ChatMessageSendResponse;
import com.homesweet.homesweetback.domain.chat.dto.response.PreMessageResponse;
import com.homesweet.homesweetback.domain.chat.entity.ChatMessage;
import com.homesweet.homesweetback.domain.chat.entity.ChatRoom;
import com.homesweet.homesweetback.domain.chat.entity.RoomMember;
import com.homesweet.homesweetback.domain.chat.entity.enums.MessageType;
import com.homesweet.homesweetback.domain.chat.repository.jpa.ChatMessageRepository;
import com.homesweet.homesweetback.domain.chat.repository.jpa.ChatRoomRepository;
import com.homesweet.homesweetback.domain.chat.repository.jpa.RoomMemberRepository;
import com.homesweet.homesweetback.domain.chat.service.ChatMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.util.List;
import java.util.stream.Collectors;


@Slf4j
@Service
@RequiredArgsConstructor
public class ChatMessageServiceImpl implements ChatMessageService {

    private final ChatMessageRepository chatMessageRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final RoomMemberRepository roomMemberRepository;

    /**
     * 메시지 전송/저장
     */
    @Override
    @Transactional
    public ChatMessageSendResponse sendMessage(Long roomId, Long senderId, String content) {

        // 채팅방 조회
        ChatRoom chatRoom = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ROOM_NOT_FOUND));

        // 발신자 조회
        RoomMember sender = roomMemberRepository.findByUserIdAndRoomId(senderId, roomId);
        if (sender == null || sender.isExit()) {
            throw new BusinessException(ErrorCode.ROOM_MEMBER_NOT_FOUND);
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

//        chatRoomMetadataService.updateLastMessage(roomId, content, savedMessage.getSentAt());

        chatRoom.updateLastMessage(content, savedMessage.getSentAt());

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
    @Transactional(readOnly = true)
    public PreMessageResponse getPreMessage(Long roomId, Long lastMessageId, int size) {
        Pageable pageable = PageRequest.of(0, size);
        Slice<ChatMessage> slice;

        if(lastMessageId == null) {
            // 최초 로드 시
            slice = chatMessageRepository.findByRoom_IdOrderBySentAtDesc(
            roomId, pageable
            );
        } else {
            // 추가 로드 요청 시
            slice = chatMessageRepository.findOlderMessages(
                    roomId, lastMessageId, pageable
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


        // 다음 페이지 존재 여부 함께 반환
        return PreMessageResponse.builder()
                .messages(messageDtos)
                .hasMore(slice.hasNext()).build();
    }

    @Override
    public boolean canSendMessage(Long userId, Long roomId) {
        RoomMember roomMember = roomMemberRepository.findByUserIdAndRoomId(userId, roomId);
        if (roomMember == null || roomMember.isExit()) return false;
        return true;
    }

}






