// k6-scripts/chat-multiroom-fixed.js

/*
* 그룹채팅방 1개, vu 500까지
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
let msgSendTime = new Trend('msg_send_time');       // 메시지 전송 시각 기록
let msgReceiveTime = new Trend('msg_receive_time'); // 메시지 수신 시각 기록
let msgLatency = new Trend('msg_latency');          // 지연 시간(ms)


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
        { duration: '1m', target: 100 },
        { duration: '1m', target: 200 },
        { duration: '1m', target: 300 },
        // { duration: '3m', target: 800 },
        { duration: '1m', target: 0 }
    ],
    thresholds: {
        'ws_connection_success_rate': ['rate>0.90'],
        'ws_connect_time': ['p(95)<5000'],
    }
};

export default function () {
    const url = 'ws://localhost:8080/ws-stomp';

    const userId = __VU;
    const roomId = 1;

    let connectStart = new Date();
    let isConnected = false;

    // 🔥 전체 사용자 중 10%만 발화자(Speaker)
    const isSpeaker = (__VU % 10 === 0);

    const response = ws.connect(
        url,
        {
            headers: { 'Sec-WebSocket-Protocol': 'stomp' },
            timeout: '60s'
        },
        function(socket) {

            function sendMessage() {
                const message = CHAT_MESSAGES[Math.floor(Math.random() * CHAT_MESSAGES.length)];
                const payload = JSON.stringify({
                    senderId: userId,
                    roomId: roomId,
                    content: message,
                    timestamp: now
                });

                msgSendTime.add(now);

                const sendFrame =
                    "SEND\n" +
                    "destination:/app/chat.send\n" +
                    "content-type:application/json\n" +
                    "\n" +
                    payload +
                    "\u0000";

                socket.send(sendFrame);
                wsMessagesSent.add(1);
                roomMessagesSent[roomId].add(1);
            }

            socket.on('open', function() {
                wsConnectTime.add(new Date() - connectStart);

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

                if (msg.includes('CONNECTED') && !isConnected) {
                    isConnected = true;
                    wsConnectionSuccess.add(1);
                    wsConnectionSuccessRate.add(true);
                    activeConnections.add(1);

                    const subscribeFrame =
                        "SUBSCRIBE\n" +
                        `id:sub-${__VU}\n` +
                        `destination:/topic/chat/rooms/${roomId}\n` +
                        "\n\0";
                    socket.send(subscribeFrame);

                    // 🔥 발화자만 주기적으로 메시지 전송
                    if (isSpeaker) {
                        // 10초마다 메시지 1개 송신
                        socket.setInterval(function () {
                            sendMessage();
                        }, 10000);
                    }
                }

                // MESSAGE 처리
                else if (msg.includes('MESSAGE')) {
                    wsMessagesReceived.add(1);
                    msgReceiveTime.add(Date.now());

                    const bodyIndex = msg.indexOf('\n\n');
                    if (bodyIndex !== -1) {
                        try {
                            const body = msg.substring(bodyIndex + 2).replace(/\0$/g, '');
                            const data = JSON.parse(body);
                            const payload = data.data;

                            if (payload && payload.timestamp) {
                                const latency = Date.now() - payload.timestamp; // 지연 시간 계산
                                msgLatency.add(latency);
                            }

                            if (payload && payload.roomId) {
                                roomMessagesReceived[payload.roomId].add(1);
                            }
                        } catch (_) {}
                    }
                }
            });

            socket.on('close', function() {
                if (isConnected) activeConnections.add(-1);
            });

            socket.on('error', function(err) {
                wsConnectionErrors.add(1);
                if (!isConnected) wsConnectionSuccessRate.add(false);
            });

            // 60초 후 연결 종료
            socket.setTimeout(function() { socket.close(); }, 60000);
        }
    );

    check(response, {
        'WebSocket 연결 성공': (r) => r && r.status === 101,
    });
}


