import http from "k6/http";
import { check } from "k6";

export const options = {
    vus: 1000,
    duration: "120s"
};

export default function () {
    const keyword = encodeURIComponent("부드러운");

    const response = http.get(
        `http://localhost:8080/api/v1/products/previews?size=12&keyword=${keyword}&sortType=LATEST}`
    );

    check(response, {
        "is status 200": (r) => r.status === 200,
    });
}