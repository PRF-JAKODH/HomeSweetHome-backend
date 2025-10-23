package com.homesweet.homesweetback.domain.product.product.controller.api;

import com.homesweet.homesweetback.common.exception.ErrorResponse;
import com.homesweet.homesweetback.domain.product.product.controller.request.ProductUploadRequest;
import com.homesweet.homesweetback.domain.product.product.controller.response.ProductResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

@Tag(name = "상품 관리", description = "상품 등록 및 관리 API")
public interface ProductAPI {

    @Operation(
            summary = "상품 등록",
            description = """
                    판매자가 상품을 등록합니다.
                    - **대표 이미지(mainImage)** 는 필수입니다.
                    - **상세 이미지(detailImages)** 는 최대 5장까지 업로드할 수 있습니다.
                    - **상품 정보(product)** 는 JSON 형태로 전송됩니다.
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "상품 등록 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ProductResponse.class),
                            examples = @ExampleObject(
                                    name = "상품 등록 성공 예시",
                                    value = """
                                            {
                                              "id": 101,
                                              "categoryId": 3,
                                              "sellerId": 1,
                                              "name": "북유럽 스타일 원목 식탁",
                                              "imageUrl": "https://s3.bucket.com/product/main/abc123.jpg",
                                              "brand": "홈스윗",
                                              "basePrice": 450000,
                                              "discountRate": 15.0,
                                              "description": "4인용 원목 식탁",
                                              "shippingPrice": 5000,
                                              "detailImages": [
                                                "https://s3.bucket.com/product/detail/1.jpg",
                                                "https://s3.bucket.com/product/detail/2.jpg"
                                              ],
                                              "status": "ACTIVE",
                                              "createdAt": "2025-10-23T14:00:00",
                                              "updatedAt": "2025-10-23T14:00:00"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청 (파일 누락, 검증 실패, 이미지 개수 초과 등)",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = {
                                    @ExampleObject(
                                            name = "대표 이미지 누락",
                                            value = """
                                                    {
                                                      "status": 400,
                                                      "message": "대표 이미지는 필수 항목입니다.",
                                                      "timestamp": "2025-10-23T14:00:00+09:00"
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "상세 이미지 개수 초과",
                                            value = """
                                                    {
                                                      "status": 400,
                                                      "message": "상세 이미지는 최대 5개까지 업로드할 수 있습니다.",
                                                      "timestamp": "2025-10-23T14:00:00+09:00"
                                                    }
                                                    """
                                    )
                            }
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "카테고리 또는 판매자를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "서버 내부 오류",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    ResponseEntity<ProductResponse> registerProduct(

            @RequestBody(
                    required = true,
                    description = "상품 등록 요청 데이터 (JSON + 이미지 업로드)",
                    content = @Content(
                            mediaType = "multipart/form-data",
                            schema = @Schema(implementation = ProductUploadRequest.class)
                    )
            )
            ProductUploadRequest request
    );
}