package com.homesweet.homesweetback.common.util.scroll;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

/**
 * Cursor 인코딩 및 디코딩 유틸 클래스
 *
 * @author junnukim1007gmail.com
 * @date 25. 11. 25.
 */
@Component
public class CursorUtil {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public String encode(List<Object> sortValues) {
        if (sortValues == null || sortValues.isEmpty()) return null;

        try {
            String json = MAPPER.writeValueAsString(sortValues);
            return Base64.getEncoder()
                    .encodeToString(json.getBytes(StandardCharsets.UTF_8));

        } catch (Exception e) {
            throw new RuntimeException("Failed to encode cursor", e);
        }
    }

    public List<Object> decode(String cursor, CursorStrategy strategy) {
        if (cursor == null || cursor.isBlank()) return null;

        try {
            byte[] bytes = Base64.getDecoder().decode(cursor);

            List<?> rawList = MAPPER.readValue(bytes, List.class);

            return strategy.extractSortValues(rawList);

        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid cursor", e);
        }
    }
}