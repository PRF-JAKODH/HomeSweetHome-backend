package com.homesweet.homesweetback.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.homesweet.homesweetback.domain.product.product.controller.request.ProductCreateRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * String -> ProductCreateRequest 컨버터 등록
 *
 * @author junnukim1007gmail.com
 * @date 25. 10. 23.
 */
@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final ObjectMapper objectMapper;

    @Override
    public void addFormatters(FormatterRegistry registry) {
        // String → ProductCreateRequest 컨버터 등록
        registry.addConverter(String.class, ProductCreateRequest.class, source -> {
            try {
                return objectMapper.readValue(source, ProductCreateRequest.class);
            } catch (Exception e) {
                throw new IllegalArgumentException("JSON 변환 실패: ProductCreateRequest 파싱 불가", e);
            }
        });
    }

}
