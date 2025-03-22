package com.example.websocket.spring_websocket.controllers;

import com.example.websocket.spring_websocket.dto.ChatMessage;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Controller;

@Controller
public class ChatController {

    private SimpMessageSendingOperations messageTemplate;

    @MessageMapping("/chat.sendMessage")
    @SendTo("/topic/public")
    public ChatMessage sendMessage( @Payload ChatMessage chatMessage){

        return chatMessage;
    }


    @MessageMapping("/chat.sendMessage") // Client sends to /app/chat.sendMessage
    public void sendMessageToUser(ChatMessage chatMessage) {
        // Send the message to a specific user destination: "/user/{receiver}/queue/messages"
        messageTemplate.convertAndSendToUser(
                chatMessage.getSender(), // Send to this user
                "/queue/messages",         // Private queue for the user
                chatMessage
        );
    }

    @MessageMapping("/chat.addUser")
    @SendTo("/topic/public")
    public ChatMessage addUser(@Payload ChatMessage chatMessage,
                               SimpMessageHeaderAccessor headerAccessor){

        headerAccessor.getSessionAttributes().put("username" ,chatMessage.getSender());
        return chatMessage;
    }
}
