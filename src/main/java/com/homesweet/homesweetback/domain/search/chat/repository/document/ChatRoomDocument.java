package com.homesweet.homesweetback.domain.search.chat.repository.document;

import org.springframework.data.annotation.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.Setting;

import java.time.LocalDateTime;

/**
 * 채팅 도큐먼트 클래스
 *
 * @author junnukim1007gmail.com
 * @date 25. 11. 26.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Setting(settingPath = "/elasticsearch/chatroom-settings.json")
@Document(indexName = "chatroom")
public class ChatRoomDocument {

    @Id
    @Field(type = FieldType.Keyword, name = "chatroom_id")
    private Long chatRoomId;

    @Field(type = FieldType.Text, analyzer = "chat_name_analyzer", searchAnalyzer = "chat_name_analyzer", name = "chatroom_name")
    private String chatRoomName;

    @Field(type = FieldType.Keyword, name = "thumbnail_url")
    private String thumbnailUrl;

    @Field(type = FieldType.Boolean, name = "is_deleted")
    private Boolean isDeleted;

    @Field(type = FieldType.Date, name = "created_at")
    private LocalDateTime createdAt;

}
