package com.homesweet.homesweetback.common.util;

import java.util.regex.Pattern;

/**
 * 한국 핸드폰 번호 형식 검증 유틸리티
 */
public class PhoneNumberValidator {
    
    // 한국 핸드폰 번호 정규식 패턴
    // 010-XXXX-XXXX, 011-XXX-XXXX, 016-XXX-XXXX, 017-XXX-XXXX, 018-XXX-XXXX, 019-XXX-XXXX
    private static final Pattern PHONE_NUMBER_PATTERN = Pattern.compile(
        "^01[0-9]-[0-9]{3,4}-[0-9]{4}$"
    );
    
    // 유효한 통신사 코드
    private static final String[] VALID_PREFIXES = {
        "010", "011", "016", "017", "018", "019"
    };
    
    /**
     * 한국 핸드폰 번호 형식이 유효한지 검증
     * 
     * @param phoneNumber 검증할 핸드폰 번호
     * @return 유효하면 true, 그렇지 않으면 false
     */
    public static boolean isValid(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            return false;
        }
        
        // 정규식 패턴 검증
        if (!PHONE_NUMBER_PATTERN.matcher(phoneNumber).matches()) {
            return false;
        }
        
        // 통신사 코드 검증
        String prefix = phoneNumber.substring(0, 3);
        for (String validPrefix : VALID_PREFIXES) {
            if (validPrefix.equals(prefix)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * 핸드폰 번호를 정규화 (하이픈 제거)
     * 
     * @param phoneNumber 정규화할 핸드폰 번호
     * @return 정규화된 핸드폰 번호 (01012345678)
     */
    public static String normalize(String phoneNumber) {
        if (phoneNumber == null) {
            return null;
        }
        return phoneNumber.replaceAll("-", "");
    }
    
    /**
     * 핸드폰 번호를 표준 형식으로 포맷팅
     * 
     * @param phoneNumber 포맷팅할 핸드폰 번호 (하이픈 포함 또는 제외)
     * @return 표준 형식의 핸드폰 번호 (010-1234-5678)
     */
    public static String format(String phoneNumber) {
        if (phoneNumber == null) {
            return null;
        }
        
        // 하이픈 제거
        String normalized = normalize(phoneNumber);
        
        // 11자리 숫자인지 확인
        if (normalized.length() != 11) {
            return phoneNumber; // 원본 반환
        }
        
        // 010-XXXX-XXXX 형식으로 포맷팅
        if (normalized.startsWith("010")) {
            return normalized.substring(0, 3) + "-" + 
                   normalized.substring(3, 7) + "-" + 
                   normalized.substring(7);
        } else {
            // 011-XXX-XXXX 형식으로 포맷팅
            return normalized.substring(0, 3) + "-" + 
                   normalized.substring(3, 6) + "-" + 
                   normalized.substring(6);
        }
    }
}
