package com.homesweet.homesweetback.domain.chat.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QRoomMember is a Querydsl query type for RoomMember
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QRoomMember extends EntityPathBase<RoomMember> {

    private static final long serialVersionUID = 185382516L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QRoomMember roomMember = new QRoomMember("roomMember");

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final BooleanPath isExit = createBoolean("isExit");

    public final NumberPath<Long> lastReadId = createNumber("lastReadId", Long.class);

    public final StringPath role = createString("role");

    public final QChatRoom room;

    public final com.homesweet.homesweetback.domain.auth.entity.QUser user;

    public QRoomMember(String variable) {
        this(RoomMember.class, forVariable(variable), INITS);
    }

    public QRoomMember(Path<? extends RoomMember> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QRoomMember(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QRoomMember(PathMetadata metadata, PathInits inits) {
        this(RoomMember.class, metadata, inits);
    }

    public QRoomMember(Class<? extends RoomMember> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.room = inits.isInitialized("room") ? new QChatRoom(forProperty("room")) : null;
        this.user = inits.isInitialized("user") ? new com.homesweet.homesweetback.domain.auth.entity.QUser(forProperty("user"), inits.get("user")) : null;
    }

}

