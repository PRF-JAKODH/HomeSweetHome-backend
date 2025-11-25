package com.homesweet.homesweetback.common.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.homesweet.homesweetback.domain.product.product.command.controller.request.ProductSortType;
import lombok.SneakyThrows;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.core.type.TypeReference;

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
    private static final TypeReference<List<Object>> LIST_TYPE = new TypeReference<>() {};

    /**
     * 정렬 값들을 Base64 인코딩된 커서 문자열로 변환
     */
    @SneakyThrows
    public String encodeSortValues(List<Object> sortValues) {
        if (sortValues == null || sortValues.isEmpty()) {
            return null;
        }

        try {
            ObjectMapper mapper = new ObjectMapper();
            String jsonArray = mapper.writeValueAsString(sortValues);
            return Base64.getEncoder().encodeToString(jsonArray.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new RuntimeException("Failed to encode cursor", e);
        }
    }

    /**
     * Base64 커서를 디코딩해서 List<Object>로 반환
     */
    @SneakyThrows
    public List<Object> decodeCursor(String cursor, ProductSortType sortType) {
        if (cursor == null || cursor.isBlank()) {
            return null;
        }

        try {
            byte[] bytes = Base64.getDecoder().decode(cursor);
            List<?> list = MAPPER.readValue(bytes, List.class);

            return switch (sortType) {
                case POPULAR -> list.size() >= 3 ? List.of(list.get(0), list.get(1), list.get(2)) : null;
                default      -> list.size() >= 2 ? List.of(list.get(0), list.get(1)) : null;
            };
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid cursor", e);
        }
    }
}
