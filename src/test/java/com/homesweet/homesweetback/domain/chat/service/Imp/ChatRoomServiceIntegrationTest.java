package com.homesweet.homesweetback.domain.chat.service.Imp;

import com.homesweet.homesweetback.common.s3.ImageUploader;
import com.homesweet.homesweetback.domain.auth.repository.UserRepository;
import com.homesweet.homesweetback.domain.chat.mapper.ChatRoomMapper;
import com.homesweet.homesweetback.domain.chat.repository.ChatMessageRepository;
import com.homesweet.homesweetback.domain.chat.repository.ChatRoomRepository;
import com.homesweet.homesweetback.domain.chat.repository.RoomMemberRepository;
import com.homesweet.homesweetback.domain.chat.service.ChatRoomService;
import com.homesweet.homesweetback.domain.chat.service.RoomMemberService;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Transactional
@DisplayName("ChatRoomService 통합테스트")
public class ChatRoomServiceIntegrationTest {

    @Autowired
    private ChatRoomService chatRoomService;
    @Autowired
    private ChatRoomMapper chatRoomMapper;
    @Autowired
    private ChatRoomRepository chatRoomRepository;
    @Autowired
    private RoomMemberRepository roomMemberRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ChatMessageRepository chatMessageRepository;
    @Autowired
    private ImageUploader s3ImageUploader;
    @Autowired
    private RoomMemberService roomMemberService;




}
