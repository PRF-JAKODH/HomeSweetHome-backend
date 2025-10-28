-- ====================================
-- 테스트용 사용자 데이터 추가
-- ====================================
-- 테스트 토큰 1~10으로 접근 가능한 사용자들
-- 주의: user_id를 명시적으로 지정하여 테스트 토큰과 매핑
INSERT INTO users (user_id, name, email, address, phone_number, profile_img_url, provider, provider_id, role, grade_id, birth_date) VALUES 
    (1, '김철수', 'test.user1@example.com', '서울시 강남구 테헤란로 123', '010-1234-5678', 'https://example.com/profile1.jpg', 'GOOGLE', 'google_12345678', 'USER', 1, '1990-01-15'),
    (2, '이영희', 'test.user2@example.com', '서울시 서초구 서초대로 456', '010-2345-6789', 'https://example.com/profile2.jpg', 'GOOGLE', 'google_23456789', 'USER', 2, '1992-03-20'),
    (3, '박민수', 'test.user3@example.com', '서울시 송파구 올림픽로 789', '010-3456-7890', 'https://example.com/profile3.jpg', 'GOOGLE', 'google_34567890', 'USER', 3, '1988-07-10'),
    (4, '최지은', 'test.user4@example.com', '경기도 성남시 분당구 정자로 321', '010-4567-8901', 'https://example.com/profile4.jpg', 'GOOGLE', 'google_45678901', 'SELLER', 1, '1995-11-05'),
    (5, '정대현', 'test.user5@example.com', '인천시 연수구 송도과학로 654', '010-5678-9012', 'https://example.com/profile5.jpg', 'KAKAO', 'kakao_56789012', 'USER', 4, '1993-05-25'),
    (6, '한수진', 'test.user6@example.com', '부산시 해운대구 해운대해변로 987', '010-6789-0123', 'https://example.com/profile6.jpg', 'KAKAO', 'kakao_67890123', 'SELLER', 2, '1991-09-18'),
    (7, '강현우', 'test.user7@example.com', '대구시 수성구 알파시티로 147', '010-7890-1234', 'https://example.com/profile7.jpg', 'GOOGLE', 'google_78901234', 'USER', 5, '1987-12-30'),
    (8, '신유진', 'test.user8@example.com', '광주시 광산구 첨단과기로 258', '010-8901-2345', 'https://example.com/profile8.jpg', 'GOOGLE', 'google_89012345', 'USER', 1, '1994-04-12'),
    (9, '윤성훈', 'test.user9@example.com', '대전시 유성구 대학로 369', '010-9012-3456', 'https://example.com/profile9.jpg', 'KAKAO', 'kakao_90123456', 'SELLER', 3, '1996-08-22'),
    (10, '조미영', 'test.user10@example.com', '경기도 용인시 기흥구 신갈로 741', '010-0123-4567', 'https://example.com/profile10.jpg', 'GOOGLE', 'google_01234567', 'USER', 2, '1992-02-28')
ON DUPLICATE KEY UPDATE name = VALUES(name);

-- AUTO_INCREMENT 시퀀스를 11로 설정하여 다음 사용자는 11부터 시작하도록 설정
ALTER TABLE users AUTO_INCREMENT = 11;
