package com.bureaucracy.insight.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;

import java.util.List;

@Service
public class AiAnalyst {

    @Value("${google.ai.api-key}")
    private String apiKey;

    @Value("${google.ai.url}")
    private String apiUrl;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper mapper = new ObjectMapper();

    public void analyzeLogs(List<String> logs) {
        try {
            // 1. Construim Prompt-ul (Instrucțiunea pentru AI)
            String promptText = "Act as a System Auditor for a Bureaucracy simulation. " +
                    "Here are the latest system logs:\n" +
                    String.join("\n", logs) +
                    "\n\nTASK: Briefly summarize the system status. " +
                    "Identify any bottlenecks (queues) or inefficiencies. " +
                    "Format the output as a short bullet-point report. No conversational filler.";

            // 2. Construim JSON-ul cerut de Gemini API
            // Structura este: { "contents": [{ "parts": [{ "text": "..." }] }] }
            ObjectNode root = mapper.createObjectNode();
            ArrayNode contents = root.putArray("contents");
            ObjectNode part = contents.addObject();
            ArrayNode parts = part.putArray("parts");
            parts.addObject().put("text", promptText);

            // 3. Setăm Header-ele HTTP
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<String> request = new HttpEntity<>(root.toString(), headers);

            // 4. Trimitem Request-ul (POST)
            String finalUrl = apiUrl + apiKey;
            ResponseEntity<String> response = restTemplate.postForEntity(finalUrl, request, String.class);

            // 5. Extragem doar textul din răspunsul complex
            JsonNode responseJson = mapper.readTree(response.getBody());
            String analysis = responseJson.path("candidates").get(0)
                    .path("content").path("parts").get(0)
                    .path("text").asText();

            // 6. Afișăm rezultatul
            System.out.println("\n📊 --- RAPORT AI GENERAT --- 📊");
            System.out.println(analysis);
            System.out.println("------------------------------\n");

        } catch (Exception e) {
            System.err.println("Eroare la comunicarea cu AI: " + e.getMessage());
            // e.printStackTrace(); // Uncomment pentru debug
        }
    }
}