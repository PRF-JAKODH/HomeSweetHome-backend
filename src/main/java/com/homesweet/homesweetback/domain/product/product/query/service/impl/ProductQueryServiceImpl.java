package com.homesweet.homesweetback.domain.product.product.query.service.impl;

import com.homesweet.homesweetback.domain.product.product.query.repository.ProductQueryRepository;
import com.homesweet.homesweetback.domain.product.product.query.service.ProductQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 상품 검색 서비스 구현체
 *
 * @author junnukim1007gmail.com
 * @date 25. 11. 24.
 */
@Service
@RequiredArgsConstructor
public class ProductQueryServiceImpl implements ProductQueryService {

    private final ProductQueryRepository productQueryRepository;


    @Override
    public List<String> autocomplete(String keyword) {
        return productQueryRepository.autocomplete(keyword);
    }
}
