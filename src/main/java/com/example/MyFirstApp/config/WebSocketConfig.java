package com.example.MyFirstApp.config;

import com.example.MyFirstApp.websocket.ConversationWebSocketHandler;
import com.example.MyFirstApp.websocket.ResumeLiveInterviewWebSocketHandler;
import com.example.MyFirstApp.websocket.LiveInterviewWebSocketHandler;

import lombok.RequiredArgsConstructor;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig
        implements WebSocketConfigurer {

    private final ConversationWebSocketHandler
            conversationWebSocketHandler;

    private final ResumeLiveInterviewWebSocketHandler
            resumeLiveInterviewWebSocketHandler;

    private final LiveInterviewWebSocketHandler
            liveInterviewWebSocketHandler;


    @Override
    public void registerWebSocketHandlers(
            WebSocketHandlerRegistry registry
    ) {

        // =====================================================
        // OLD LIVE INTERVIEW
        // =====================================================

        registry.addHandler(
                liveInterviewWebSocketHandler,
                "/ws/live-interview"
        );


        // =====================================================
        // RESUME INTERVIEW
        // =====================================================

        registry.addHandler(
                resumeLiveInterviewWebSocketHandler,
                "/ws/resume-live-interview"
        );


        // =====================================================
        // HR + COMMUNICATION CONVERSATION
        // =====================================================

        registry.addHandler(
                conversationWebSocketHandler,
                "/ws/conversation"
        );


        // =====================================================
        // ALLOWED ORIGINS
        // =====================================================

        registry
                .addHandler(
                        conversationWebSocketHandler,
                        "/ws/conversation"
                )
                .setAllowedOrigins("*");
    }
}