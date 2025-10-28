package com.homesweet.homesweetback.domain.chat.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QChatImage is a Querydsl query type for ChatImage
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QChatImage extends EntityPathBase<ChatImage> {

    private static final long serialVersionUID = -1050767900L;

    public static final QChatImage chatImage = new QChatImage("chatImage");

    public final StringPath fileName = createString("fileName");

    public final NumberPath<Integer> fileSize = createNumber("fileSize", Integer.class);

    public final StringPath fileType = createString("fileType");

    public final StringPath fileUrl = createString("fileUrl");

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final StringPath thumbUrl = createString("thumbUrl");

    public final DateTimePath<java.time.LocalDateTime> uploadedAt = createDateTime("uploadedAt", java.time.LocalDateTime.class);

    public QChatImage(String variable) {
        super(ChatImage.class, forVariable(variable));
    }

    public QChatImage(Path<? extends ChatImage> path) {
        super(path.getType(), path.getMetadata());
    }

    public QChatImage(PathMetadata metadata) {
        super(ChatImage.class, metadata);
    }

}

