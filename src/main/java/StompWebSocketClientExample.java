import org.springframework.messaging.simp.stomp.*;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.lang.reflect.Type;
import java.util.concurrent.CompletableFuture;

public class StompWebSocketClientExample {
    public static void main(String[] args) {
        String url = "ws://localhost:8080/websocket-endpoint";

        // Standard WebSocket client
        StandardWebSocketClient client = new StandardWebSocketClient();
        WebSocketStompClient stompClient = new WebSocketStompClient(client);

        StompSessionHandler sessionHandler = new CustomStompSessionHandler();

        // Using connectAsync instead of the deprecated connect method
        CompletableFuture<StompSession> futureSession = stompClient.connectAsync(url, sessionHandler);

        // Optional: Handle connection completion
        futureSession.thenAccept(session -> System.out.println("Connected to WebSocket!"));
        futureSession.exceptionally(ex -> {
            System.err.println("Connection failed: " + ex.getMessage());
            return null;
        });

        // Keep the application running
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

class CustomStompSessionHandler extends StompSessionHandlerAdapter {
    @Override
    public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
        System.out.println("Connected!");

        session.subscribe("/topic/messages", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return String.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                System.out.println("Received: " + payload);
            }
        });

        session.send("/app/send-message", "Hello WebSocket Server!");
    }

    @Override
    public void handleTransportError(StompSession session, Throwable exception) {
        System.out.println("Error: " + exception.getMessage());
    }
}