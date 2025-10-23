package com.homesweet.homesweetback.domain.chat.dto;


public record MessageRequest (
        String roomId,
        String sender,
        String message)
    {}
