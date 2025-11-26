package com.homesweet.homesweetback.domain.search.community.repository.document;

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
 * 게시글 도큐먼트 클래스
 *
 * @author junnukim1007gmail.com
 * @date 25. 11. 26.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Setting(settingPath = "/elasticsearch/community-settings.json")
@Document(indexName = "community_posts")
public class CommunityPostDocument {

    @Id
    @Field(type = FieldType.Long, name = "post_id")
    private Long postId;

    @Field(type = FieldType.Text,
            name = "title",
            analyzer = "post_text_analyzer",
            searchAnalyzer = "post_text_analyzer")
    private String title;

    @Field(type = FieldType.Text,
            name = "title.ngram",
            analyzer = "post_ngram_analyzer",
            searchAnalyzer = "post_text_analyzer")
    private String titleNgram;

    @Field(type = FieldType.Text,
            name = "title.autocomplete",
            analyzer = "post_autocomplete_analyzer",
            searchAnalyzer = "standard")
    private String titleAutocomplete;

    @Field(type = FieldType.Keyword, name = "title.keyword")
    private String titleKeyword;

    @Field(type = FieldType.Text,
            analyzer = "post_text_analyzer",
            searchAnalyzer = "post_text_analyzer")
    private String content;

    @Field(type = FieldType.Keyword)
    private String category;

    @Field(type = FieldType.Long, name = "author_id")
    private Long authorId;

    @Field(type = FieldType.Integer, name = "view_count")
    private Integer viewCount;

    @Field(type = FieldType.Integer, name = "like_count")
    private Integer likeCount;

    @Field(type = FieldType.Integer, name = "comment_count")
    private Integer commentCount;

    @Field(type = FieldType.Boolean, name = "is_deleted")
    private Boolean isDeleted;

    @Field(type = FieldType.Date, name = "created_at")
    private LocalDateTime createdAt;
}