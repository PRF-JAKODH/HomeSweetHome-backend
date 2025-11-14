import http from "k6/http";
import { check } from "k6";

// 가능한 시나리오 16개
const testCases = [
    { caseId: "1-1", keyword: "",       categoryId: null, sortType: "LATEST"      },
    { caseId: "1-2", keyword: "",       categoryId: null, sortType: "POPULAR"     },
    { caseId: "1-3", keyword: "",       categoryId: null, sortType: "PRICE_LOW"   },
    { caseId: "1-4", keyword: "",       categoryId: null, sortType: "PRICE_HIGH"  },

    { caseId: "2-1", keyword: "침대",   categoryId: null, sortType: "LATEST"      },
    { caseId: "2-2", keyword: "침대",   categoryId: null, sortType: "POPULAR"     },
    { caseId: "2-3", keyword: "침대",   categoryId: null, sortType: "PRICE_LOW"   },
    { caseId: "2-4", keyword: "침대",   categoryId: null, sortType: "PRICE_HIGH"  },

    { caseId: "3-1", keyword: "",       categoryId: 1,    sortType: "LATEST"      },
    { caseId: "3-2", keyword: "",       categoryId: 1,    sortType: "POPULAR"     },
    { caseId: "3-3", keyword: "",       categoryId: 1,    sortType: "PRICE_LOW"   },
    { caseId: "3-4", keyword: "",       categoryId: 1,    sortType: "PRICE_HIGH"  },

    { caseId: "4-1", keyword: "침대",   categoryId: 1,    sortType: "LATEST"      },
    { caseId: "4-2", keyword: "침대",   categoryId: 1,    sortType: "POPULAR"     },
    { caseId: "4-3", keyword: "침대",   categoryId: 1,    sortType: "PRICE_LOW"   },
    { caseId: "4-4", keyword: "침대",   categoryId: 1,    sortType: "PRICE_HIGH"  },
];

// 1명의 사용자, 1회 요청
export const options = {
    vus: 1,          // 동시 사용자 1명
    iterations: 1,   // 딱 1번만 실행
};

// API host
const BASE_URL = "http://localhost:8080/api/v1/products/previews";

export default function () {
    // 테스트 케이스 랜덤 선택
    const test = testCases[Math.floor(Math.random() * testCases.length)];

    // Build params
    const params = {
        keyword: test.keyword || "",
        cursorId: "",
        size: 12,
        sortType: test.sortType,
    };
    if (test.categoryId !== null) {
        params.categoryId = test.categoryId;
    }

    // API 호출
    const res = http.get(BASE_URL, { params: { ...params } });

    // 응답 검증
    check(res, {
        "status is 200": (r) => r.status === 200,
    });
}