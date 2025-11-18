import http from "k6/http";
import { check, sleep } from "k6";
import { Counter } from "k6/metrics";

export const errors = new Counter("errors");

export const options = {
    vus: 1000,
    duration: "120s"
};

export default function () {
    const url = "http://localhost:8080/api/v1/products/previews?limit=12&sortType=LATEST";

    const response = http.get(url);

    // Check metric 저장 (InfluxDB로 전송됨)
    const ok = check(response, {
        "status is 200": (r) => r.status === 200,
    });

    if (!ok) {
        errors.add(1);
    }
}