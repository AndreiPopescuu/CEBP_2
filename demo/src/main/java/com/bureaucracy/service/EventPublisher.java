package com.bureaucracy.service;

import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Service
public class EventPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final AmqpAdmin amqpAdmin;

    // Constructor Injection for RabbitMQ tools
    public EventPublisher(RabbitTemplate rabbitTemplate, AmqpAdmin amqpAdmin) {
        this.rabbitTemplate = rabbitTemplate;
        this.amqpAdmin = amqpAdmin;
    }

    /**
     * This runs automatically when the server starts.
     * It creates the queue in Docker if it doesn't exist.
     */
    @PostConstruct
    public void init() {
        try {
            // Create the queue named "bureaucracy-logs"
            amqpAdmin.declareQueue(new Queue("bureaucracy-logs", true));
            System.out.println("\n==========================================");
            System.out.println("✅ CONNECTED TO DOCKER RABBITMQ");
            System.out.println("   Queue 'bureaucracy-logs' is ready.");
            System.out.println("==========================================\n");
        } catch (Exception e) {
            System.err.println("\n❌ COULD NOT CONNECT TO RABBITMQ");
            System.err.println("   Is Docker running? Is the container started?");
            System.err.println("   Error: " + e.getMessage() + "\n");
        }
    }

    public void publishEvent(String type, String clientName, String message) {
        Map<String, Object> event = new HashMap<>();
        event.put("timestamp", Instant.now().toString());
        event.put("type", type);
        event.put("client", clientName);
        event.put("message", message);

        // 1. Print to local console (so you can see it working)
        System.out.println("[EVENT] " + event);

        // 2. Send to RabbitMQ (Docker)
        try {
            rabbitTemplate.convertAndSend("bureaucracy-logs", event);
        } catch (Exception e) {
            System.err.println("⚠️ Failed to send to RabbitMQ: " + e.getMessage());
        }
    }
}