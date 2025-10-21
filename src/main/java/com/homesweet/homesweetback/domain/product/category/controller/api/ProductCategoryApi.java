
package com.homesweet.homesweetback.domain.product.category.controller.api;

import com.homesweet.homesweetback.common.exception.ErrorResponse;
import com.homesweet.homesweetback.domain.product.category.controller.request.CategoryCreateRequest;
import com.homesweet.homesweetback.domain.product.category.controller.response.CategoryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

/**
 * 제품 카테고리 API 인터페이스
 *
 * @author junnukim1007gmail.com
 * @date 25. 10. 21.
 */
@Tag(name = "제품 카테고리", description = "제품 카테고리 관리 API")
public interface ProductCategoryApi {

    @Operation(
            summary = "카테고리 생성",
            description = "새로운 제품 카테고리를 생성합니다. 최상위 카테고리(depth 0) 또는 하위 카테고리(depth 1-2)를 생성할 수 있습니다."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "카테고리 생성 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = CategoryResponse.class),
                            examples = @ExampleObject(
                                    name = "성공 응답 예시",
                                    value = """
                                            {
                                              "id": 1,
                                              "name": "가구",
                                              "parentId": null,
                                              "depth": 0,
                                              "createdAt": "2025-10-21T10:00:00",
                                              "updatedAt": "2025-10-21T10:00:00"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청 (검증 실패, 최대 깊이 초과)",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = {
                                    @ExampleObject(
                                            name = "검증 실패",
                                            value = """
                                                    {
                                                      "status": 400,
                                                      "message": "카테고리 이름은 필수입니다",
                                                      "timestamp": "2025-10-21T10:00:00+09:00"
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "카테고리 깊이 초과",
                                            value = """
                                                    {
                                                      "status": 400,
                                                      "message": "카테고리 기준 깊이를 넘었습니다",
                                                      "timestamp": "2025-10-21T10:00:00+09:00"
                                                    }
                                                    """
                                    )
                            }
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "부모 카테고리를 찾을 수 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "부모 카테고리 미존재",
                                    value = """
                                            {
                                              "status": 404,
                                              "message": "부모 카테고리를 찾을 수 없습니다",
                                              "timestamp": "2025-10-21T10:00:00+09:00"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "중복된 카테고리 이름",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "중복된 카테고리 이름",
                                    value = """
                                            {
                                              "status": 409,
                                              "message": "이미 해당하는 카테고리 이름이 존재합니다",
                                              "timestamp": "2025-10-21T10:00:00+09:00"
                                            }
                                            """
                            )
                    )
            )
    })
    ResponseEntity<CategoryResponse> createCategory(
            @Valid @RequestBody CategoryCreateRequest request
    );

    @Operation(
            summary = "부모 ID로 하위 카테고리 목록 조회",
            description = """
                    특정 부모 카테고리의 직계 하위 카테고리 목록을 조회합니다.
                    
                    예시:
                    - parentId가 1인 "가구" 카테고리의 하위 카테고리 조회
                    - 결과: "침실가구", "거실가구", "주방가구" 등
                    
                    하위 카테고리가 없는 경우 빈 배열을 반환합니다.
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = CategoryResponse.class)),
                            examples = @ExampleObject(
                                    name = "하위 카테고리 목록",
                                    value = """
                                            [
                                              {
                                                "id": 2,
                                                "name": "침실가구",
                                                "parentId": 1,
                                                "depth": 1,
                                                "createdAt": "2025-10-21T10:00:00",
                                                "updatedAt": "2025-10-21T10:00:00"
                                              },
                                              {
                                                "id": 3,
                                                "name": "거실가구",
                                                "parentId": 1,
                                                "depth": 1,
                                                "createdAt": "2025-10-21T10:00:00",
                                                "updatedAt": "2025-10-21T10:00:00"
                                              },
                                              {
                                                "id": 4,
                                                "name": "주방가구",
                                                "parentId": 1,
                                                "depth": 1,
                                                "createdAt": "2025-10-21T10:00:00",
                                                "updatedAt": "2025-10-21T10:00:00"
                                              }
                                            ]
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "200",
                    description = "하위 카테고리가 없는 경우",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "빈 목록",
                                    value = "[]"
                            )
                    )
            )
    })
    ResponseEntity<List<CategoryResponse>> getCategoriesByParentId(
            @Parameter(
                    description = "부모 카테고리 ID",
                    required = true,
                    example = "1"
            )
            @PathVariable Long parentId
    );

    @Operation(
            summary = "최상위 카테고리 목록 조회",
            description = """
                    depth가 0인 최상위 카테고리 목록을 조회합니다.
                    
                    최상위 카테고리는 parentId가 null이며, 카테고리 계층 구조의 루트에 해당합니다.
                    
                    사용 예시:
                    - 메인 페이지의 카테고리 메뉴 구성
                    - 상품 등록 시 대분류 카테고리 선택
                    
                    최상위 카테고리가 없는 경우 빈 배열을 반환합니다.
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = CategoryResponse.class)),
                            examples = @ExampleObject(
                                    name = "최상위 카테고리 목록",
                                    value = """
                                            [
                                              {
                                                "id": 1,
                                                "name": "가구",
                                                "parentId": null,
                                                "depth": 0,
                                                "createdAt": "2025-10-21T10:00:00",
                                                "updatedAt": "2025-10-21T10:00:00"
                                              },
                                              {
                                                "id": 5,
                                                "name": "조명",
                                                "parentId": null,
                                                "depth": 0,
                                                "createdAt": "2025-10-21T10:00:00",
                                                "updatedAt": "2025-10-21T10:00:00"
                                              },
                                              {
                                                "id": 10,
                                                "name": "패브릭",
                                                "parentId": null,
                                                "depth": 0,
                                                "createdAt": "2025-10-21T10:00:00",
                                                "updatedAt": "2025-10-21T10:00:00"
                                              }
                                            ]
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "200",
                    description = "최상위 카테고리가 없는 경우",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "빈 목록",
                                    value = "[]"
                            )
                    )
            )
    })
    ResponseEntity<List<CategoryResponse>> getTopLevelCategories();
}