package com.homesweet.homesweetback.domain.chat.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Pattern;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "chat_image")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class ChatImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "image_id",nullable = false)
    private Long id;

    // 파일 이름, url 타입 수정
    @Column(nullable = false)
    private String fileUrl;

    @Column(name = "thumbnail_url", nullable = false)
    private String thumbUrl;

    @Column(nullable = false)
    private Integer fileSize;

    @CreatedDate
    private LocalDateTime uploadedAt;

    @Pattern(regexp = "jpg|png|gif|webp|mp4|webm", message = "허용되지 않는 파일 타입입니다.")
    @Column(nullable = false, length = 100)
    private String fileType;

    @Column(nullable = false, length = 100)
    private String fileName;


}
