import http from "k6/http";
import { check } from "k6";

export const options = {
    batchPerHost: 10,
    scenarios: {
        category_scenario: {
            executor: "shared-iterations",
            startTime: "0s",
            vus: 10,
            iterations: 10,
            maxDuration: "180s",
        },
    },
};

export default function () {
    const response = http.get("http://localhost:8080/api/v1/products/previews?limit=12&sortType=LATEST");
    check(response, {
        "is status 200": (r) => r.status === 200,
    });
}