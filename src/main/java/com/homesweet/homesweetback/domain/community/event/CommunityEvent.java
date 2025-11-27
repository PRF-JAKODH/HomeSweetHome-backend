package com.homesweet.homesweetback.domain.community.event;

import com.homesweet.homesweetback.common.event.DomainEvent;
import lombok.Getter;

/**
 * 게시글 이벤트
 *
 * @author junnukim1007gmail.com
 * @date 25. 11. 27.
 */
@Getter
public class CommunityEvent extends DomainEvent {
    private final Long communityId;
    private final CommunityEventType communityEventType;

    protected CommunityEvent(Long communityId, CommunityEventType eventType) {
        super("community." + eventType.name().toLowerCase());
        this.communityId = communityId;
        this.communityEventType = eventType;
    }

    public static CommunityEvent created(Long communityId) {
        return new CommunityEvent(communityId, CommunityEventType.CREATED);
    }

    public static CommunityEvent updated(Long communityId) {
        return new CommunityEvent(communityId, CommunityEventType.UPDATED);
    }

    public static CommunityEvent deleted(Long communityId) {
        return new CommunityEvent(communityId, CommunityEventType.DELETED);
    }
}
