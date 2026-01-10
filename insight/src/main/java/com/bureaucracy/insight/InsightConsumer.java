package com.bureaucracy.insight;

import com.fasterxml.jackson.databind.node.ObjectNode; // Import this
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class InsightConsumer {

    private final RabbitTemplate rabbitTemplate;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    // Memory to store logs
    private final Map<String, List<String>> citizenLogs = new ConcurrentHashMap<>();

    // YOUR KEY
    private final String apiKey = "AIzaSyCzfpmPEdLJmhTA8LyMUXoQSE5TQ6A6EiA";

    public InsightConsumer(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
    }

    @RabbitListener(queues = "bureaucracy-logs")
    public void consumeMessage(String message) {
        String[] parts = message.split("\\|", 3);
        if (parts.length < 3) return;

        String name = parts[0];
        String type = parts[1];
        String content = parts[2];

        citizenLogs.computeIfAbsent(name, k -> new ArrayList<>()).add(content);

        if ("CLIENT_FINISH".equals(type)) {
            generateAiReport(name);
        }
    }

    private void generateAiReport(String name) {
        try {
            List<String> logs = citizenLogs.get(name);
            String prompt = "Act as a System Auditor for a Bureaucracy simulation. " +
                    "Here are the latest system logs:\n" +
                    String.join("\n", logs) +
                    "\n\nTASK: Briefly summarize the system status. " +
                    "Identify any bottlenecks (queues) or inefficiencies. " +
                    "Format the output as a short bullet-point report. No conversational filler.";
            //String prompt = "Generate a funny, sarcastic summary of this bureaucracy experience (max 30 words): " + logs.toString();

            // --- 🔧 FIX STARTS HERE 🔧 ---
            // 1. Create the JSON Tree correctly
            ObjectNode rootNode = objectMapper.createObjectNode();

            // Structure: { "contents": [ { "parts": [ { "text": "..." } ] } ] }
            rootNode.putArray("contents")
                    .addObject()
                    .putArray("parts")
                    .addObject()
                    .put("text", prompt);

            // Convert the WHOLE tree to string
            String jsonPayload = rootNode.toString();
            // --- 🔧 FIX ENDS HERE 🔧 ---

            // 2. Send Request
            String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=" + apiKey;
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            String aiText = "AI Error";
            if (response.statusCode() == 200) {
                // Parse the response safely
                aiText = objectMapper.readTree(response.body())
                        .path("candidates").get(0)
                        .path("content").path("parts").get(0)
                        .path("text").asText();
            } else {
                System.err.println("❌ Google Error: " + response.body());
            }

            System.out.println("🤖 AI SAYS: " + aiText);

            // 3. Send back to GUI
            rabbitTemplate.convertAndSend("ai-responses", name + "###" + aiText);

            citizenLogs.remove(name);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}