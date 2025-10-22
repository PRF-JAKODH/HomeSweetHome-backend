package com.homesweet.homesweetback.domain.chat.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Pattern;
import lombok.*;

@Entity
@Table(name = "chat_image")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
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
    private int fileSize;

    @Column(nullable = false)
    private String uploaded_at;

    @Pattern(regexp = "jpg|png|gif|webp|mp4|webm", message = "허용되지 않는 파일 타입입니다.")
    @Column(nullable = false, length = 100)
    private String fileType;

    @Column(nullable = false, length = 100)
    private String fileName;


}
