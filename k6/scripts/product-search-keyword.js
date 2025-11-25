import http from "k6/http";
import { check } from "k6";
import { sleep } from "k6";


const token = "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiIxMSIsImVtYWlsIjoianVubnVraW0xMDA3QGdtYWlsLmNvbSIsIm5hbWUiOiLquYDspIDsmrAiLCJwcm92aWRlciI6Imdvb2dsZSIsInJvbGUiOiJTRUxMRVIiLCJpYXQiOjE3NjQwNTI1MDgsImV4cCI6MTc2NDA3MDUwOH0.JFS1lEv4bFErFnvyqPtWWE7ogtS71MPn5kkxQQisfOcGJMcuOwDRkkc65sRwX4AS0FS207_z74xbFpW1YyhYPA"
;

export const options = {
    vus: 1000,
    duration: "60s"
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