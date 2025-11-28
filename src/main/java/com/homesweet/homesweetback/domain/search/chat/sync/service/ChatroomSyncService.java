package com.homesweet.homesweetback.domain.search.chat.sync.service;

import com.homesweet.homesweetback.common.exception.BusinessException;
import com.homesweet.homesweetback.common.exception.ErrorCode;
import com.homesweet.homesweetback.domain.chat.entity.ChatRoom;
import com.homesweet.homesweetback.domain.chat.repository.ChatRoomRepository;
import com.homesweet.homesweetback.domain.search.chat.repository.document.ChatRoomDocument;
import com.homesweet.homesweetback.domain.search.chat.sync.mapping.ChatroomDocumentMapping;
import com.homesweet.homesweetback.domain.search.chat.sync.repository.ChatRoomDocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 채팅방 엘라스틱 동기화 서비스
 *
 * @author junnukim1007gmail.com
 * @date 25. 11. 27.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatroomSyncService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomDocumentRepository chatRoomDocumentRepository;
    private final ChatroomDocumentMapping chatroomDocumentMapping;

    @Transactional(readOnly = true)
    public void syncToElasticsearch(Long chatroomId) {
        try {
            ChatRoom chatRoom = chatRoomRepository.findById(chatroomId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.ROOM_NOT_FOUND));

            ChatRoomDocument document = chatroomDocumentMapping.convertToDocument(chatRoom);

            chatRoomDocumentRepository.save(document);
            log.info("성공적으로 엘라스틱서치에 동기화가 되었습니다 -> chatroomId: {}", chatroomId);
        } catch (Exception e) {
            log.error("Failed to sync product {} to Elasticsearch", chatroomId, e);
        }
    }

    @Transactional
    public void deleteFromElasticsearch(Long chatroomId) {
        try {
            chatRoomDocumentRepository.deleteById(chatroomId);
            log.info("엘라스틱서치 채팅방 데이터 제거가 완료되었습니다");
        } catch (Exception e) {
            log.error("Failed to delete chatroom {} from Elasticsearch", chatroomId, e);
        }
    }

}
