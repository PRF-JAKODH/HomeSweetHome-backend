import http from "k6/http";
import { check, sleep } from "k6";

// 16가지 테스트 케이스
const testCases = {
    "1-1": { keyword: "",       categoryId: null, sortType: "LATEST"      },
    "1-2": { keyword: "",       categoryId: null, sortType: "POPULAR"     },
    "1-3": { keyword: "",       categoryId: null, sortType: "PRICE_LOW"   },
    "1-4": { keyword: "",       categoryId: null, sortType: "PRICE_HIGH"  },

    "2-1": { keyword: "소파",   categoryId: null, sortType: "LATEST"      },
    "2-2": { keyword: "소파",   categoryId: null, sortType: "POPULAR"     },
    "2-3": { keyword: "소파",   categoryId: null, sortType: "PRICE_LOW"   },
    "2-4": { keyword: "소파",   categoryId: null, sortType: "PRICE_HIGH"  },

    "3-1": { keyword: "",       categoryId: 1,    sortType: "LATEST"      },
    "3-2": { keyword: "",       categoryId: 1,    sortType: "POPULAR"     },
    "3-3": { keyword: "",       categoryId: 1,    sortType: "PRICE_LOW"   },
    "3-4": { keyword: "",       categoryId: 1,    sortType: "PRICE_HIGH"  },

    "4-1": { keyword: "소파",   categoryId: 1,    sortType: "LATEST"      },
    "4-2": { keyword: "소파",   categoryId: 1,    sortType: "POPULAR"     },
    "4-3": { keyword: "소파",   categoryId: 1,    sortType: "PRICE_LOW"   },
    "4-4": { keyword: "소파",   categoryId: 1,    sortType: "PRICE_HIGH"  },
};

// k6 실행 시 환경변수로 TEST_CASE 전달 필요
const caseId = __ENV.TEST_CASE;

if (!caseId || !testCases[caseId]) {
    throw new Error(`❌ TEST_CASE 환경 변수를 설정하세요. 사용 가능: ${Object.keys(testCases).join(", ")}`);
}

const selected = testCases[caseId];

// 반복 횟수 10회 — 한 케이스를 연속 10번 실행
export const options = {
    vus: 1,
    iterations: 10,  // 한 케이스를 10회 반복 측정
};

export default function () {
    const params = {
        keyword: selected.keyword || "",
        cursorId: 9,
        size: 12,
        sortType: selected.sortType,
    };

    if (selected.categoryId !== null) {
        params.categoryId = selected.categoryId;
    }

    const res = http.get(BASE_URL, { params });

    check(res, {
        "status is 200": (r) => r.status === 200,
    });

    sleep(0.2);
}