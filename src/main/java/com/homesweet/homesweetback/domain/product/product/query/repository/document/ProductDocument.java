package com.homesweet.homesweetback.domain.product.product.query.repository.document;

import org.springframework.data.annotation.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.Setting;

import java.time.LocalDateTime;

/**
 * 상품 Elastic 매핑
 *
 * @author junnukim1007gmail.com
 * @date 25. 11. 24.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Setting(settingPath = "/elasticsearch/product-settings.json")
@Document(indexName = "products")
public class ProductDocument {

    @Id
    @Field(type = FieldType.Long, name = "product_id")
    private Long productId;

    @Field(type = FieldType.Text, analyzer = "product_search_analyzer", searchAnalyzer = "product_search_analyzer")
    private String name;

    @Field(type = FieldType.Text, name = "name.ngram", analyzer = "product_ngram_analyzer", searchAnalyzer = "product_search_analyzer")
    private String nameNgram;

    @Field(type = FieldType.Text, name = "name.autocomplete", analyzer = "autocomplete_analyzer", searchAnalyzer = "standard")
    private String nameAutocomplete;

    @Field(type = FieldType.Keyword, name = "name.keyword")
    private String nameKeyword;

    @Field(type = FieldType.Keyword)
    private String brand;

    @Field(type = FieldType.Text, analyzer = "product_search_analyzer", searchAnalyzer = "product_search_analyzer")
    private String description;

    @Field(type = FieldType.Integer, name = "base_price")
    private Integer basePrice;

    @Field(type = FieldType.Float, name = "discount_rate")
    private Float discountRate;

    @Field(type = FieldType.Integer, name = "shipping_price")
    private Integer shippingPrice;

    @Field(type = FieldType.Keyword)
    private String status;

    @Field(type = FieldType.Keyword, name = "image_url")
    private String imageUrl;

    @Field(type = FieldType.Long, name = "category_id")
    private Long categoryId;

    @Field(type = FieldType.Keyword, name = "category_name")
    private String categoryName;

    @Field(type = FieldType.Text, name = "category_name.text", analyzer = "product_search_analyzer")
    private String categoryNameText;

    @Field(type = FieldType.Date, name = "created_at")
    private LocalDateTime createdAt;

    @Field(type = FieldType.Date, name = "updated_at")
    private LocalDateTime updatedAt;
}