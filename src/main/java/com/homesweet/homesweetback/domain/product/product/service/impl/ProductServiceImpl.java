package com.homesweet.homesweetback.domain.product.product.service.impl;

import com.homesweet.homesweetback.domain.product.product.repository.ProductRepository;
import com.homesweet.homesweetback.domain.product.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 제품 서비스 구현 코드
 *
 * @author junnukim1007gmail.com
 * @date 25. 10. 21.
 */
@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository repository;

}
