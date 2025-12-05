// k6-scripts/chat-multiroom-fixed.js

/*
* JWT 정수 인증(1~20000) 사용하는 여러 방 분산 테스트
* - STOMP 프레임 형식 수정
* - sleep() 제거하고 타이머 사용
* */

import ws from 'k6/ws';
import { Trend, Counter, Gauge, Rate } from 'k6/metrics';
import { check } from 'k6';

// 커스텀 메트릭
let wsConnectTime = new Trend('ws_connect_time');
let wsConnectionSuccess = new Counter('ws_connection_success');
let wsConnectionErrors = new Counter('ws_connection_errors');
let wsMessagesReceived = new Counter('ws_messages_received');
let wsMessagesSent = new Counter('ws_messages_sent');
let activeConnections = new Gauge('ws_active_connections');
let wsConnectionSuccessRate = new Rate('ws_connection_success_rate');

// 방별 메트릭
let roomMessagesSent = {};
let roomMessagesReceived = {};
for (let i = 1; i <= 10; i++) {
    roomMessagesSent[i] = new Counter(`room_${i}_msgs_sent`);
    roomMessagesReceived[i] = new Counter(`room_${i}_msgs_received`);
}

const CHAT_MESSAGES = [
    "안녕하세요!",
    "반갑습니다 😊",
    "점심 뭐 먹을까요?",
    "회의 시작합니다",
    "네, 알겠습니다",
    "좋은 아이디어네요!",
    "확인했습니다",
    "감사합니다!",
    "ㅋㅋㅋ",
    "ㅇㅇ"
];

export const options = {
    stages: [
        { duration: '30s', target: 100 },
        { duration: '30s', target: 200 },
        { duration: '1m', target: 500 },
        { duration: '30s', target: 0 }
    ],
    thresholds: {
        'ws_connection_success_rate': ['rate>0.90'],
        'ws_connect_time': ['p(95)<5000'],
    }
};

export default function () {
    const url = 'ws://localhost:8080/ws-stomp';

    // JWT를 1~20000 정수로 사용
    const userId = ((__VU - 1) % 20000) + 1;
    const roomId = 1;  // 모든 VU를 Room 1에 집중 (브로드캐스트 테스트)

    let connectStart = new Date();
    let isConnected = false;
    let messagesSent = 0;
    const messagesToSend = 3;

    const response = ws.connect(
        url,
        {
            headers: { 'Sec-WebSocket-Protocol': 'stomp' },
            timeout: '60s'
        },
        function(socket) {

            // 메시지 전송 함수를 맨 위에 정의
            function sendMessage(socket) {
                if (messagesSent >= messagesToSend) {
                    return;
                }

                const message = CHAT_MESSAGES[Math.floor(Math.random() * CHAT_MESSAGES.length)];
                const payload = JSON.stringify({
                    senderId: userId,
                    roomId: roomId,
                    content: message
                });

                // content-length 없이 전송 (null octet으로 종료)
                const sendFrame =
                    "SEND\n" +
                    "destination:/app/chat.send\n" +
                    "content-type:application/json\n" +
                    "\n" +
                    payload +
                    "\u0000";  // \0 대신 \u0000 사용

                socket.send(sendFrame);
                wsMessagesSent.add(1);
                roomMessagesSent[roomId].add(1);  // 방별 전송 카운트
                messagesSent++;

                console.log(`[VU ${__VU}] 메시지 ${messagesSent}/${messagesToSend} 전송: "${message}" (Room ${roomId})`);
            }

            socket.on('open', function() {
                wsConnectTime.add(new Date() - connectStart);

                // STOMP CONNECT 프레임 (host 헤더 포함!)
                const connectFrame =
                    "CONNECT\n" +
                    `Authorization:Bearer ${userId}\n` +
                    "accept-version:1.1,1.2\n" +
                    "heart-beat:10000,10000\n" +
                    "host:localhost\n" +
                    "\n\0";

                socket.send(connectFrame);
            });

            socket.on('message', function(msg) {
                // CONNECTED 프레임 수신
                if (msg.includes('CONNECTED') && !isConnected) {
                    isConnected = true;
                    wsConnectionSuccess.add(1);
                    wsConnectionSuccessRate.add(true);
                    activeConnections.add(1);

                    console.log(`[VU ${__VU}] CONNECTED → Room ${roomId} 구독`);

                    // 채팅방 구독
                    const subscribeFrame =
                        "SUBSCRIBE\n" +
                        `id:sub-${__VU}\n` +
                        `destination:/topic/chat/rooms/${roomId}\n` +
                        "\n\0";

                    socket.send(subscribeFrame);

                    // 첫 메시지 즉시 전송
                    sendMessage(socket);

                    // 두 번째 메시지: 5초 후
                    socket.setTimeout(function() {
                        if (messagesSent < messagesToSend) {
                            sendMessage(socket);
                        }
                    }, 5000);

                    // 세 번째 메시지: 10초 후
                    socket.setTimeout(function() {
                        if (messagesSent < messagesToSend) {
                            sendMessage(socket);
                        }
                    }, 10000);
                }
                // ERROR 프레임
                else if (msg.includes('ERROR')) {
                    console.error(`[VU ${__VU}] STOMP ERROR: ${msg.substring(0, 200)}`);
                    wsConnectionErrors.add(1);
                    if (!isConnected) {
                        wsConnectionSuccessRate.add(false);
                    }
                }
                // MESSAGE 프레임
                else if (msg.includes('MESSAGE')) {
                    wsMessagesReceived.add(1);

                    // 메시지 바디 파싱
                    const bodyIndex = msg.indexOf('\n\n');
                    if (bodyIndex !== -1 && bodyIndex >= 0) {
                        try {
                            const body = msg.substring(bodyIndex + 2).replace(/\0$/g, '');
                            //

                            const data = JSON.parse(body);
                            const payload = data.data;   // 서버가 보낸 실제 메시지

                            if (payload && payload.roomId && roomMessagesReceived[payload.roomId]) {
                                roomMessagesReceived[payload.roomId].add(1);
                            }

                            console.log(`[VU ${__VU}] 수신 "${payload.content}" (User ${payload.senderId} → Room ${payload.roomId})`);

                        } catch (e) {
                            // 파싱 실패는 무시
                        }
                    }
                }
            });

            socket.on('close', function() {
                if (isConnected) {
                    activeConnections.add(-1);
                }
            });

            socket.on('error', function(err) {
                console.error(`[VU ${__VU}] WebSocket 에러: ${err.error()}`);
                wsConnectionErrors.add(1);
                if (!isConnected) {
                    wsConnectionSuccessRate.add(false);
                }
            });

            // 60초 후 연결 종료
            socket.setTimeout(function() {
                socket.close();
            }, 60000);
        }
    );

    // 연결 실패 체크
    check(response, {
        'WebSocket 연결 성공': (r) => r && r.status === 101,
    });

    if (!response || response.status !== 101) {
        console.error(`[VU ${__VU}] WebSocket 연결 실패`);
        wsConnectionErrors.add(1);
        wsConnectionSuccessRate.add(false);
    }
}