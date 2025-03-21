package com.example.websocket.spring_websocket.config;


import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public  void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws").withSockJS();

    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.setApplicationDestinationPrefixes("/app");
        registry.enableSimpleBroker("/app");
/*
        // Use RabbitMQ as the message broker
        registry.enableStompBrokerRelay("/queue", "/topic")
                .setRelayHost("localhost")  // RabbitMQ Host
                .setRelayPort(61613)        // Default STOMP port for RabbitMQ
                .setClientLogin("guest")    // RabbitMQ username
                .setClientPasscode("guest"); // RabbitMQ password

 */
    }


}
