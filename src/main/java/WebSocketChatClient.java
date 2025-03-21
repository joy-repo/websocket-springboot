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

        // Send a chat message
        ChatMessage chatMessage = new ChatMessage("User1", "Hello, WebSocket!");
        session.send("/app/chat.sendMessage", chatMessage);
    }

    @Override
    public void handleTransportError(StompSession session, Throwable exception) {
        System.out.println("WebSocket Error: " + exception.getMessage());
    }
}

// ChatMessage Model
class ChatMessage {
    private String sender;
    private String content;

    public ChatMessage() {}

    public ChatMessage(String sender, String content) {
        this.sender = sender;
        this.content = content;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}