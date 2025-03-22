
## Steps 

### 1 dependencies :

```xml
<dependencies>
    <!-- Spring Boot WebSocket -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-websocket</artifactId>
    </dependency>

    <!-- STOMP over WebSockets -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-messaging</artifactId>
    </dependency>

    <!-- RabbitMQ for message broker -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-amqp</artifactId>
    </dependency>

    <!-- Jackson for JSON conversion -->
    <dependency>
        <groupId>com.fasterxml.jackson.core</groupId>
        <artifactId>jackson-databind</artifactId>
    </dependency>
</dependencies>
```


### 2. Configure WebSocket with STOMP

```java
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws") // WebSocket connection endpoint
                .setAllowedOrigins("*") // Allow all origins
                .withSockJS(); // Enable SockJS fallback
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableStompBrokerRelay("/topic", "/queue") // Routes messages via RabbitMQ
                .setRelayHost("localhost") // RabbitMQ host
                .setRelayPort(61613) // RabbitMQ STOMP port
                .setSystemLogin("guest") // RabbitMQ username
                .setSystemPasscode("guest"); // RabbitMQ password
        registry.setApplicationDestinationPrefixes("/app"); // Prefix for app messages
    }
}
```


### 3. Model message details

```java
public class ChatMessage {
    private String sender;
    private String content;
    private String type; // JOIN, MESSAGE, LEAVE

    // Getters and Setters
}
```

### 4. Create Controller to Handle Messages

````java
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

@Controller
public class ChatController {

    @MessageMapping("/chat.sendMessage") // Handles messages sent to "/app/chat.sendMessage"
    @SendTo("/topic/public") // Broadcasts the message to all subscribers of "/topic/public"
    public ChatMessage sendMessage(ChatMessage chatMessage) {
        return chatMessage;
    }

    @MessageMapping("/chat.addUser")
    @SendTo("/topic/public")
    public ChatMessage addUser(ChatMessage chatMessage, SimpMessageHeaderAccessor headerAccessor) {
        // Store username in WebSocket session
        headerAccessor.getSessionAttributes().put("username", chatMessage.getSender());
        return chatMessage;
    }
}
````


### 5. rabbitMQ connection details (application.properties)

```yml
spring:
  rabbitmq:
    host: localhost
    port: 5672
    username: guest
    password: guest
  websocket:
    stomp:
      relay:
        host: localhost
        port: 61613
```

### 6. javascrit client 

```javascript
<script src="https://cdn.jsdelivr.net/sockjs/1/sockjs.min.js"></script>
<script src="https://cdnjs.cloudflare.com/ajax/libs/stomp.js/2.3.3/stomp.min.js"></script>
<script>
    var socket = new SockJS('/ws');
    var stompClient = Stomp.over(socket);

    stompClient.connect({}, function(frame) {
        console.log('Connected: ' + frame);

        // Subscribe to topic
        stompClient.subscribe('/topic/public', function(message) {
            console.log("Received: " + message.body);
        });

        // Send a test message
        stompClient.send("/app/chat.sendMessage", {}, JSON.stringify({
            sender: "User1",
            content: "Hello World!",
            type: "MESSAGE"
        }));
    });
</script>
```

###  7. Start RabbitMQ with STOMP Plugin

```shell
rabbitmq-plugins enable rabbitmq_stomp
rabbitmq-plugins enable rabbitmq_web_stomp

rabbitmq-server

```

How Messages Flow in the System
1.	Client connects to WebSocket (/ws)
2.	Client subscribes to /topic/public to receive messages
3.	Client sends messages to /app/chat.sendMessage or /app/chat.addUser
4.	ChatController processes the message and broadcasts it to /topic/public
5.	All subscribed clients receive the message in real-time




## Private messaging


## 1. Enable private messaging:

```java
   @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableStompBrokerRelay("/queue", "/topic")
                .setRelayHost("localhost")
                .setRelayPort(61613)
                .setSystemLogin("guest")
                .setSystemPasscode("guest");

        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user"); // Enables private messaging -> /user/{receiver}/queue/messages
    }
```

## 2. modify the controller

```java
 @MessageMapping("/chat.sendMessage") // Client sends to /app/chat.sendMessage
    public void sendMessage(ChatMessage chatMessage) {
        // Send the message to a specific user destination: "/user/{receiver}/queue/messages"
        messagingTemplate.convertAndSendToUser(
                chatMessage.getReceiver(), // Send to this user
                "/queue/messages",         // Private queue for the user
                chatMessage
        );
    }

```

|Configuration |What It Does|
|---|---|
|setApplicationDestinationPrefixes("/app")|Ensures that only messages starting with /app are handled by controllers (@MessageMapping).|
|setUserDestinationPrefix("/user")|Enables private messaging by routing messages to /user/{username}/queue/messages.|


This ensures that:
* Public messages use /topic/*.
* Private messages use /user/queue/*.

## Fully customized with destination Variable:

```java
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
public class ChatController {

    private final SimpMessagingTemplate messagingTemplate;

    public ChatController(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @MessageMapping("/chat/{roomId}/{userType}/sendMessage")
    public void sendMessage(
            @DestinationVariable String roomId,
            @DestinationVariable String userType,
            @Payload ChatMessage chatMessage) {

        String destination = "/chat/room/" + roomId + "/" + userType;

        // Send message to specific room and user type
        messagingTemplate.convertAndSend(destination, chatMessage);
    }
}
```
