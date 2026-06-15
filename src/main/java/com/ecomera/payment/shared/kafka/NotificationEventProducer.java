package com.ecomera.payment.shared.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationEventProducer {

    private static final String TOPIC = "ecomera.notifications";

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public void sendNotification(String recipient, String subject, String body, String type, String sourceService) {
        try {
            String payload = objectMapper.writeValueAsString(Map.of(
                    "recipient", recipient,
                    "subject", subject,
                    "body", body,
                    "type", type,
                    "sourceService", sourceService
            ));
            kafkaTemplate.send(TOPIC, payload);
            log.info("Notification event sent to topic {}: {}", TOPIC, subject);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize notification event", e);
        }
    }
}
