import http from "k6/http";
import { check } from "k6";
import { sleep } from "k6";


const token = "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiIxMSIsImVtYWlsIjoianVubnVraW0xMDA3QGdtYWlsLmNvbSIsIm5hbWUiOiLquYDspIDsmrAiLCJwcm92aWRlciI6Imdvb2dsZSIsInJvbGUiOiJTRUxMRVIiLCJpYXQiOjE3NjQxNTE2NjgsImV4cCI6MTc2NDE2OTY2OH0.oiXnHA9BlYKgXLuMJKnEeINcFMFRIVWYjjEvW9ZyncZd8JifGA0i0C686wYtvjmnAFPf81i2hnbO2LwtjOR6ZQ"

export const options = {
    stages: [
        {duration: '3m', target: 1000}
    ]
};

export default function () {
    const keyword = encodeURIComponent("침대프레임");

    const response = http.get(
        `http://localhost:8080/api/v1/products/search?size=12&keyword=${keyword}&sortType=LATEST`,
        {
            headers: {
                "Authorization": `Bearer ${token}`
            }
        }
    );

    check(response, {
        "is status 200": (r) => r.status === 200,
    });

    sleep(1);
}