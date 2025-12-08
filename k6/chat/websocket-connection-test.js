// // k6-scripts/chat-connection-hold-test.js

/*
* 예상 시나리오
* : 동시 연결 유지 테스트, 최종 10,000명 동시 접속 + 3분 유지 테스트
* */

import ws from 'k6/ws';
import { Trend, Counter, Gauge, Rate } from 'k6/metrics';
import { check } from 'k6';

// 커스텀 메트릭
let wsConnectTime = new Trend('ws_connect_time');
let wsConnectionSuccess = new Counter('ws_connection_success');
let wsConnectionErrors = new Counter('ws_connection_errors');
let wsMessagesReceived = new Counter('ws_messages_received');
let wsSessionDuration = new Trend('ws_session_seconds');
let activeConnections = new Gauge('ws_active_connections');
let wsConnectionSuccessRate = new Rate('ws_connection_success_rate');

export const options = {
    stages: [
        { duration: '30ms', target: 2000 },
        { duration: '30ms', target: 4000 },
        { duration: '1m', target: 8000 },
        // { duration: '3m', target: 9000 },
        { duration: '1m', target: 0 }
    ],
    thresholds: {
        'ws_connection_success_rate': ['rate>0.99'],  // 연결 성공률 99% 이상
        'ws_connect_time': ['p(95)<2000'],            // 연결 시간 95% 2초 이하
        'ws_connection_errors': ['count<30'],         // 전체 에러 30개 미만
    }
};

export default function () {
    const url = 'ws://localhost:8080/ws-stomp';
    const userId = Math.floor(Math.random() * 20012) + 1;


    const response = ws.connect(url, {headers: {'Sec-WebSocket-Protocol': 'stomp',}}, function(socket) {
        let connectStart = new Date();
        let isConnected = false;

        socket.on('open', function() {

            wsConnectTime.add(new Date() - connectStart);

            // STOMP CONNECT 프레임 전송
            const connectFrame =
                "CONNECT\n" +
                `Authorization:Bearer ${userId}\n` +
                "accept-version:1.1,1.2\n" +
                "heart-beat:10000,10000\n" +
                "\n\0";

            socket.send(connectFrame);
        });

        socket.on('message', function(msg) {
            // CONNECTED 프레임 수신 확인
            if (msg.includes('CONNECTED')) {
                isConnected = true;
                wsConnectionSuccess.add(1);
                wsConnectionSuccessRate.add(true);
                activeConnections.add(1);

                // // 채팅방 구독
                // const subscribeFrame =
                //     "SUBSCRIBE\n" +
                //     `id:sub-${__VU}\n` +
                //     `destination:/sub/chat/rooms/${roomId}\n` +
                //     "\n\0";
                //
                // socket.send(subscribeFrame);
            }
            // // 일반 메시지 수신 (다른 사람이 보낸 메시지)
            // else if (msg.includes('MESSAGE')) {
            //     wsMessagesReceived.add(1);
            //     // console.log(`[VU ${__VU}] 메시지 수신`);
            // }
        });

        socket.on('close', function() {
            const sessionDuration = (new Date() - connectStart) / 1000;
            wsSessionDuration.add(sessionDuration);
            activeConnections.add(-1);
        });

        socket.on('error', function (err) {
            console.error(`[VU ${__VU}] 에러 발생: ${err.error()}`);
            wsConnectionErrors.add(1);
            wsConnectionSuccessRate.add(false);
            if (!isConnected) {
                activeConnections.add(-1);
            }
        });

        socket.setTimeout(function() {
            socket.close();
        }, 180000);
    });

    // 연결 실패 체크
    check(response, {
        'WebSocket 연결 성공': (r) => r && r.status === 101,
    });

    if (!response || response.status !== 101) {
        console.error(`[VU ${__VU}] WebSocket 연결 실패`);
        wsConnectionErrors.add(1);
    }
}