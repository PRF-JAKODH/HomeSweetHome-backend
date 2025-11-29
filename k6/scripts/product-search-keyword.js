import http from "k6/http";
import { check } from "k6";
import { sleep } from "k6";


const token = "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiIxMSIsImVtYWlsIjoianVubnVraW0xMDA3QGdtYWlsLmNvbSIsIm5hbWUiOiLquYDspIDsmrAiLCJwcm92aWRlciI6Imdvb2dsZSIsInJvbGUiOiJTRUxMRVIiLCJpYXQiOjE3NjQzMTcwNDksImV4cCI6MTc2NDMzNTA0OX0.JLjtm2_8u81nw3vH37qL0f3_76gCkZhvYx_B1HNOX03JK91KPCapEnb3YQyHNtPXt4R-O0EaI7K-nicsxL-H4Q"

export const options = {
    scenarios: {
        ramp_tps: {
            executor: "ramping-arrival-rate",
            startRate: 200,
            timeUnit: "1s",
            preAllocatedVUs: 300,
            maxVUs: 8000,
            stages: [
                { target: 200, duration: "1m" },
                { target: 400, duration: "1m" },
                { target: 800, duration: "1m" },
                { target: 2000, duration: "2m" },
            ],
        }
    },
};


export default function () {
    const keyword = encodeURIComponent("침대프레임");

    const response = http.get(
        `http://localhost:8080/api/v1/products/search?size=12&keyword=${keyword}&sortType=RECOMMENDED`,
        {
            headers: {
                "Authorization": `Bearer ${token}`
            }
        }
    );

    check(response, {
        "is status 200": (r) => r.status === 200,
    });

}