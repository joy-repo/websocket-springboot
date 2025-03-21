package com.example.websocket.spring_websocket.config;

import com.example.websocket.spring_websocket.dto.ChatMessage;
import com.example.websocket.spring_websocket.dto.MessageType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketEventListner {

    private SimpMessageSendingOperations messageTemplate;

    @EventListener
    public void handleWeSocketDisconnectListener(SessionDisconnectEvent event){

        StompHeaderAccessor stompHeaderAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String username = (String)stompHeaderAccessor.getSessionAttributes().get("username");

        if(username!=null){
            log.info("User Disconnected :{}",username);
            ChatMessage chatMessage = ChatMessage.builder()
                    .messageType(MessageType.LEAVE)
                    .sender(username)
                    .build();
            messageTemplate.convertAndSend("/topic/public", chatMessage);
        }

    }
}
