package com.homesweet.homesweetback.common.log;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.spi.FilterReply;

/**
 * TraceId 로그만 추적
 *
 * @author junnukim1007gmail.com
 * @date 25. 10. 18.
 */
public class TraceIdFilter extends Filter<ILoggingEvent> {

    @Override
    public FilterReply decide(ILoggingEvent event) {
        String traceId = event.getMDCPropertyMap().get("traceId");
        if (traceId != null) {
            return FilterReply.ACCEPT;
        }
        return FilterReply.DENY;
    }
}