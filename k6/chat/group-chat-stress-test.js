
/*
 * 파일명: k6-scripts/chat-connection-hold-test.js
 * 목적: 10,000 VU WebSocket 그룹 채팅 방 참여 및 메시지 전송 + RTT 측정
 */

import ws from 'k6/ws';
import http from 'k6/http';
import { Trend, Counter, Gauge, Rate } from 'k6/metrics';
import { check, sleep } from 'k6';

// 1. 커스텀 메트릭 정의
let wsConnectTime = new Trend('ws_connect_time');
let wsConnectionSuccess = new Counter('ws_connection_success');
let wsConnectionErrors = new Counter('ws_connection_errors');
let wsMessagesReceived = new Counter('ws_messages_received');
let wsMessagesSent = new Counter('ws_messages_sent');
let wsMessageRTT = new Trend('ws_message_rtt');
let wsSessionDuration = new Trend('ws_session_seconds');
let activeConnections = new Gauge('ws_active_connections');
let wsConnectionSuccessRate = new Rate('ws_connection_success_rate');
let wsMessageSendSuccess = new Rate('ws_message_send_success');


// 2. 테스트 옵션
export const options = {
    stages: [
        { duration: '1m', target: 1000 },
        // { duration: '3m', target: 6000 },
        // { duration: '1m', target: 8000 },
        // { duration: '3m', target: 10000 },
        // { duration: '5m', target: 10000 },
        { duration: '1m', target: 0 }
    ],
    thresholds: {
        'ws_connection_success_rate': ['rate>0.99'],
        'ws_connect_time': ['p(95)<2000'],
        'ws_connection_errors': ['count<30'],
        'ws_message_send_success': ['rate>0.95'],
        'ws_message_rtt': ['p(95)<500'],   // RTT 임계값 ✔ 추가
    }
};


export default function () {
    const wsUrl = 'ws://localhost:8080/ws-stomp';
    const MIN_ID = 1;
    const MAX_ID = 20000;
    const ACTIVE_PERCENT = 10;

    const userId = MIN_ID + Math.floor(Math.random() * (MAX_ID - MIN_ID + 1));
    const roomId = 1;
    const authToken = String(userId);

    const isActive = __VU  * (ACTIVE_PERCENT/100);  // 약 10% ~ 15%만 메시지 전송

    // HTTP Join 사전작업
    const httpJoinUrl = `http://localhost:8080/api/v1/chat/rooms/${roomId}/join`;
    const joinResponse = http.post(httpJoinUrl, null, {
        headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${authToken}`
        }
    });

    const joinSuccess = check(joinResponse, {
        'HTTP Join API 성공 (2xx)': (r) => r.status >= 200 && r.status < 300,
    });

    if (!joinSuccess) {
        wsConnectionErrors.add(1);
        wsConnectionSuccessRate.add(false);
        sleep(1);
        return;
    }


    // WebSocket 메인 로직
    const response = ws.connect(wsUrl, { headers: { 'Sec-WebSocket-Protocol': 'stomp' } }, function(socket) {
        let connectStart = new Date();
        let heartbeat = null;
        let msgTimer = null;

        const ongoingMessages = new Map(); // messageId → sendTimestamp (RTT 측정용)

        socket.on('open', function() {
            wsConnectTime.add(new Date() - connectStart);

            const connectFrame =
                "CONNECT\n" +
                `Authorization:Bearer ${authToken}\n` +
                "accept-version:1.1,1.2\n" +
                "heart-beat:0,0\n" +
                "\n\0";

            socket.send(connectFrame);
        });

        socket.on('message', function(msg) {
            console.log("connect : ", msg )
            // 1. CONNECTED 프레임 처리
            if (msg.includes('CONNECTED')) {
                wsConnectionSuccess.add(1);
                wsConnectionSuccessRate.add(true);
                activeConnections.add(1);

                // SUBSCRIBE
                const subscribeFrame =
                    "SUBSCRIBE\n" +
                    `id:sub-${__VU}\n` +
                    `destination:/sub/chat/rooms/${roomId}\n\n\0`;

                console.log("subscribeFrame: ", subscribeFrame)
                socket.send(subscribeFrame);

                if (isActive) {
                    scheduleNextMessage();
                }
            }

            // 2. MESSAGE 수신
            else if (msg.includes('MESSAGE')) {
                wsMessagesReceived.add(1);

                const matchMsgId = msg.match(/"messageId":\s*(\d+)/);
                if (matchMsgId) {
                    const msgId = matchMsgId[1];
                    const sentTime = ongoingMessages.get(msgId);
                    if (sentTime) {
                        const rtt = new Date() - sentTime;
                        wsMessageRTT.add(rtt);
                        ongoingMessages.delete(msgId);
                    }
                }
            }

            // 3. ERROR 프레임
            else if (msg.includes('ERROR')) {
                wsConnectionErrors.add(1);
                wsConnectionSuccessRate.add(false);
                socket.close();
            }
        });

        socket.on('close', function() {
            activeConnections.add(-1);
            if (msgTimer) socket.cancelTimeout(msgTimer);
        });

        socket.on('error', function(err) {
            wsConnectionErrors.add(1);
            wsConnectionSuccessRate.add(false);
        });


        // 메시지 스케줄링 함수
        function scheduleNextMessage() {
            msgTimer = socket.setTimeout(() => {
                const messageId = `${__VU}-${Date.now()}`;

                const sendPayload = {
                    type: 'TALK',
                    roomId: roomId,
                    content: "테스트 메시지",
                    senderId: userId,
                    messageId: messageId    // RTT 측정을 위한 ID ✔
                };

                const sendFrame =
                    "SEND\n" +
                    `destination:/pub/chat/rooms/${roomId}\n` +
                    "content-type:application/json\n\n" +
                    JSON.stringify(sendPayload) + "\0";

                try {
                    ongoingMessages.set(messageId, new Date());
                    socket.send(sendFrame);
                    wsMessagesSent.add(1);
                    wsMessageSendSuccess.add(true);
                } catch {
                    wsMessageSendSuccess.add(false);
                }

                scheduleNextMessage(); // 재귀 호출
            }, 8000 + Math.random() * 8000);  // 8~16초
        }

        socket.setTimeout(() => socket.close(), 720000);
    });

    check(response, { 'WebSocket 연결 성공': (r) => r && r.status === 101 });

    if (!response || response.status !== 101) {
        wsConnectionErrors.add(1);
    }

    sleep(1);
}



