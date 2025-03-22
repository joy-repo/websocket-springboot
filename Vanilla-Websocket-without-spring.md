## Steps to  create a simple WebSocket server using Java’s ServerEndpoint annotation from the ***Jakarta EE WebSocket API.***

### 1. Add dependencies : 

```xml
<dependencies>
    <dependency>
        <groupId>javax.websocket</groupId>
        <artifactId>javax.websocket-api</artifactId>
        <version>1.1</version>
    </dependency>
    <dependency>
        <groupId>org.glassfish.tyrus</groupId>
        <artifactId>tyrus-server</artifactId>
        <version>1.17</version>
    </dependency>
</dependencies>
```

### 3. WebSocket Server Implementation

***Structure of the message***

```
@recipientSessionId:message
```

```java

import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@ServerEndpoint("/chat")
public class ChatServer {

    private static final Map<String, Session> sessions = new ConcurrentHashMap<>();

    @OnOpen
    public void onOpen(Session session) {
        sessions.put(session.getId(), session);
        System.out.println("New connection: " + session.getId());
    }

    @OnMessage
    public void onMessage(String message, Session senderSession) throws IOException {
        System.out.println("Received: " + message);
        
        // Check if the message contains a recipient session ID (format: @recipientId:message)
        if (message.startsWith("@")) {
            int separatorIndex = message.indexOf(":");
            if (separatorIndex > 1) {
                String recipientId = message.substring(1, separatorIndex); // Extract recipient session ID
                String actualMessage = message.substring(separatorIndex + 1); // Extract actual message

                Session recipientSession = sessions.get(recipientId);
                if (recipientSession != null) {
                    recipientSession.getBasicRemote().sendText("Private from " + senderSession.getId() + ": " + actualMessage);
                } else {
                    senderSession.getBasicRemote().sendText("Error: Recipient not found.");
                }
            }
        } else {
            senderSession.getBasicRemote().sendText("Invalid format. Use @sessionId:message");
        }
    }

    @OnClose
    public void onClose(Session session) {
        sessions.remove(session.getId());
        System.out.println("Connection closed: " + session.getId());
    }

    @OnError
    public void onError(Session session, Throwable throwable) {
        System.err.println("Error on session " + session.getId() + ": " + throwable.getMessage());
    }
}
```

```
Example Client Interaction
	•	Client 1 (Session ID: abc123) 
	    sends:  @xyz789:Hello, how are you?
	    
	•	Server processes and sends:
	    Private from abc123: Hello, how are you?
	    
	    only xyz789 receives it

```

## Websocket Client

Mapping with the call back class is done at line 126.

```java 

import javax.websocket.*;
import java.net.URI;
import java.util.Scanner;

@ClientEndpoint
public class WebSocketClient {
    private static Session session;

    @OnOpen
    public void onOpen(Session session) {
        WebSocketClient.session = session;
        System.out.println("Connected to WebSocket server!");
    }

    @OnMessage
    public void onMessage(String message) {
        System.out.println("Received: " + message);
    }

    @OnClose
    public void onClose() {
        System.out.println("Connection closed!");
    }

    public static void main(String[] args) {
        try {
            WebSocketContainer container = ContainerProvider.getWebSocketContainer();
            container.connectToServer(WebSocketClient.class, new URI("ws://localhost:8080/chat"));

            // Send messages from the console
            Scanner scanner = new Scanner(System.in);
            while (true) {
                System.out.print("Enter message: ");
                String message = scanner.nextLine();
                if ("exit".equalsIgnoreCase(message)) {
                    session.close();
                    break;
                }
                session.getBasicRemote().sendText(message);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
```
----------------------------------------------------------

## How @ServerEndpoint Works

The @ServerEndpoint annotation is applied at the class level and specifies the WebSocket URL where clients can connect.

**Basic Usage:**

* The @ServerEndpoint("/chat") means clients can connect to ws://localhost:8080/chat (if deployed on a local server).
*  lifecycle methods (@OnOpen, @OnMessage, @OnClose, @OnError) define how the server handles WebSocket events.

```java
@ServerEndpoint("/chat")
public class ChatServer {
    @OnOpen
    public void onOpen(Session session) {
        System.out.println("New client connected: " + session.getId());
    }

    @OnMessage
    public void onMessage(String message, Session session) {
        System.out.println("Received: " + message);
    }

    @OnClose
    public void onClose(Session session) {
        System.out.println("Client disconnected: " + session.getId());
    }
}
```

### Annotations Inside a WebSocket Server (@ServerEndpoint)

#### 1. @Open

Called when a new client connects to the WebSocket server.
```java

@OnOpen
public void onOpen(Session session) {
    System.out.println("New connection: " + session.getId());
}
```

#### 2. @OnMessage

```java

@OnMessage
public void onMessage(String message, Session session) {
    System.out.println("Received: " + message);
}
```

#### 3. @OnClose

```java
@OnClose
public void onClose(Session session) {
    System.out.println("Connection closed: " + session.getId());
}
```

#### 4. @OnError

```java
@OnError
public void onError(Session session, Throwable throwable) {
    System.err.println("Error on session " + session.getId() + ": " + throwable.getMessage());
}
```

Logs errors or attempts to recover from an issue.

### Advanced Usage of @ServerEndpoint

**1. You can add path parameters to the WebSocket endpoint:**

```java
@ServerEndpoint("/chat/{username}")
public class ChatServer {
    @OnOpen
    public void onOpen(Session session, @PathParam("username") String username) {
        System.out.println(username + " connected with session: " + session.getId());
    }
}
```

**2. Sending Messages to All Connected Clients (Broadcast)**

```java
@OnMessage
public void onMessage(String message, Session senderSession) throws IOException {
    for (Session session : sessions.values()) {
        if (session.isOpen() && !session.getId().equals(senderSession.getId())) {
            session.getBasicRemote().sendText("Broadcast: " + message);
        }
    }
}
```

**3. Sending Private Messages (Direct Messaging)**

```java
@OnMessage
public void onMessage(String message, Session senderSession) throws IOException {
    if (message.startsWith("@")) {
        int separatorIndex = message.indexOf(":");
        if (separatorIndex > 1) {
            String recipientId = message.substring(1, separatorIndex);
            String actualMessage = message.substring(separatorIndex + 1);

            Session recipientSession = sessions.get(recipientId);
            if (recipientSession != null) {
                recipientSession.getBasicRemote().sendText("Private from " + senderSession.getId() + ": " + actualMessage);
            }
        }
    }
}
```



-------------------------------------------------------------

## Why is @ClientEndpoint Needed?
1.	**Registers the class as a WebSocket client**
   * The WebSocket container (provided by Java EE/Jakarta EE or a standalone WebSocket API) detects and manages the lifecycle of this client.
2.	**Enables WebSocket event handling**
   * The annotated methods (@OnOpen, @OnMessage, @OnClose) will be automatically invoked when relevant WebSocket events occur.
3.	**Allows automatic connection management**
   * The WebSocketContainer can connect this client to a WebSocket server without manually handling low-level networking.

How It Works in Your Code:
* @OnOpen: This method runs when the WebSocket connection is established.
* @OnMessage: This method is triggered whenever the client receives a message from the server.
* @OnClose: This method executes when the connection is closed.

If you remove @ClientEndpoint, the Java WebSocket API won’t recognize your class as a WebSocket client, 
and the event-handling methods won’t work.


