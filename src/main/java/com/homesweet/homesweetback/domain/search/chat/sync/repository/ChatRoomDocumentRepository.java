package com.homesweet.homesweetback.domain.search.chat.sync.repository;

import com.homesweet.homesweetback.domain.search.chat.repository.document.ChatRoomDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

/**
 * 채팅 엘라스틱 레포지토리
 *
 * @author junnukim1007gmail.com
 * @date 25. 11. 26.
 */
public interface ChatRoomDocumentRepository extends ElasticsearchRepository<ChatRoomDocument, Long> {
}
