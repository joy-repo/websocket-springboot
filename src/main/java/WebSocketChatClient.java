import com.example.websocket.spring_websocket.dto.ChatMessage;
import com.example.websocket.spring_websocket.dto.MessageType;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.lang.reflect.Type;
import java.util.concurrent.CompletableFuture;

public class WebSocketChatClient {

    private static final String WEBSOCKET_URL = "ws://localhost:8080/ws";

    public static void main(String[] args) {
        // Create a WebSocket client
        StandardWebSocketClient webSocketClient = new StandardWebSocketClient();
        WebSocketStompClient stompClient = new WebSocketStompClient(webSocketClient);

        // Configure message converter (to send/receive JSON)
        stompClient.setMessageConverter(new MappingJackson2MessageConverter());

        // Connect asynchronously
        StompSessionHandler sessionHandler = new CustomStompSessionHandler();
        CompletableFuture<StompSession> futureSession = stompClient.connectAsync(WEBSOCKET_URL, sessionHandler);

        // Handle connection completion
        futureSession.thenAccept(session -> System.out.println("Connected to WebSocket!"));
        futureSession.exceptionally(ex -> {
            System.err.println("Connection failed: " + ex.getMessage());
            return null;
        });

        // Keep the application running to receive messages
        try {
            Thread.sleep(10000); // Adjust as needed
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

// Custom STOMP session handler
class CustomStompSessionHandler extends StompSessionHandlerAdapter {

    @Override
    public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
        System.out.println("WebSocket Connected!");

        // Subscribe to the "/topic/public" topic
        session.subscribe("/topic/public", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return ChatMessage.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                ChatMessage message = (ChatMessage) payload;
                System.out.println("Received message: " + message.getSender() + " -> " + message.getContent());
            }
        });
        //Add user
        ChatMessage chatMessage = ChatMessage.builder()
                .messageType(MessageType.JOIN)
                .sender("User1")
                .build();

        session.send("/app/chat.addUser", chatMessage);

        // Send a chat message
        chatMessage = ChatMessage.builder()
                .messageType(MessageType.CHAT)
                .sender("User1")
                .content("Hello, WebSocket!")
                .build();

        session.send("/app/chat.sendMessage", chatMessage);
    }

    @Override
    public void handleTransportError(StompSession session, Throwable exception) {
        System.out.println("WebSocket Error: " + exception.getMessage());
    }
}

