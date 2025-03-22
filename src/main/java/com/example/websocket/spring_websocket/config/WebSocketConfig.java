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
        registry.setUserDestinationPrefix("/user");


        /*
        Configuration
What It Does
setApplicationDestinationPrefixes("/app") ::
Ensures that only messages starting with /app are handled by controllers (@MessageMapping).
setUserDestinationPrefix("/user") ::
Enables private messaging by routing messages to /user/{username}/queue/messages.

         */


/*
        // Use RabbitMQ as the message broker
        registry.enableStompBrokerRelay("/queue", "/topic")
                .setRelayHost("localhost")  // RabbitMQ Host
                .setRelayPort(61613)        // Default STOMP port for RabbitMQ
                .setClientLogin("guest")    // RabbitMQ username
                .setClientPasscode("guest"); // RabbitMQ password


                /////////////////////////////////////////////////////////////////

                Enable RabbitMQ STOMP Plugin : rabbitmq-plugins enable rabbitmq_stomp
                Check if RabbitMQ STOMP is Running :  rabbitmqctl status
                ---- [{rabbitmq_stomp,"RabbitMQ STOMP adapter","3.x.x"}, ...]---

 */
    }


}
