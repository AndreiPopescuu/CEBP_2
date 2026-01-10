package com.bureaucracy.service;

import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;

@Service
public class EventPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final AmqpAdmin amqpAdmin;

    public EventPublisher(RabbitTemplate rabbitTemplate, AmqpAdmin amqpAdmin) {
        this.rabbitTemplate = rabbitTemplate;
        this.amqpAdmin = amqpAdmin;
    }

    @PostConstruct
    public void init() {
        try {
            // Ensure the queue exists
            amqpAdmin.declareQueue(new Queue("bureaucracy-logs", true));
            System.out.println("✅ CONNECTED TO DOCKER RABBITMQ");
        } catch (Exception e) {
            System.err.println("❌ COULD NOT CONNECT TO RABBITMQ: " + e.getMessage());
        }
    }

    public void publishEvent(String type, String clientName, String message) {

        String payload = clientName + "|" + type + "|" + message;

        System.out.println("📤 LOGGING: " + payload);

        try {
            rabbitTemplate.convertAndSend("bureaucracy-logs", payload);
        } catch (Exception e) {
            System.err.println("⚠️ Failed to send to RabbitMQ: " + e.getMessage());
        }
    }
}