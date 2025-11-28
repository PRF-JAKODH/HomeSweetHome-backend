package com.homesweet.homesweetback.domain.search.product.sync.mapping;

import com.homesweet.homesweetback.domain.product.category.domain.ProductCategory;
import com.homesweet.homesweetback.domain.product.product.command.domain.Product;
import com.homesweet.homesweetback.domain.search.product.repository.document.ProductDocument;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 상품 도큐먼트 <-> 도메인 매핑
 *
 * @author junnukim1007gmail.com
 * @date 25. 11. 27.
 */
@Component
public class ProductDocumentMapping {

    public ProductDocument convertToDocument(Product product, ProductCategory category) {
        List<ProductDocument.OptionGroup> optionGroups = product.getOptionGroups().stream()
                .map(group -> ProductDocument.OptionGroup.builder()
                        .groupName(group.getGroupName())
                        .values(group.getValues().stream()
                                .map(v -> v.getValue())
                                .collect(Collectors.toList()))
                        .build())
                .collect(Collectors.toList());

        Integer salePrice = calculateSalePrice(product);

        return ProductDocument.builder()
                .productId(product.getId())
                .name(product.getName())
                .brand(product.getBrand())
                .description(product.getDescription())
                .basePrice(product.getBasePrice())
                .discountRate(product.getDiscountRate() != null ?
                        product.getDiscountRate().floatValue() : 0f)
                .salePrice(salePrice)
                .shippingPrice(product.getShippingPrice())
                .status(product.getStatus().name())
                .imageUrl(product.getImageUrl())
                .categoryId(category.id())
                .categoryName(category.name())
                .optionGroups(optionGroups)
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }

    private Integer calculateSalePrice(Product product) {
        if (product.getDiscountRate() == null ||
                product.getDiscountRate().compareTo(BigDecimal.ZERO) == 0) {
            return product.getBasePrice();
        }
        BigDecimal discount = BigDecimal.valueOf(product.getBasePrice())
                .multiply(product.getDiscountRate())
                .divide(BigDecimal.valueOf(100));
        return product.getBasePrice() - discount.intValue();
    }
}
