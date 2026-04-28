package com.cableops.tracker.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Slf4j
@Service
public class TelegramService {

    @Value("${telegram.bot.token}")
    private String botToken;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    /**
     * Sends an HTML-formatted message to a Telegram chat.
     * Never throws — failures are logged only.
     */
    public void sendMessage(String chatId, String text) {
        if (chatId == null || chatId.isBlank()) {
            log.warn("Telegram chatId is blank — skipping notification");
            return;
        }

        try {
            String payload = buildJson(chatId, text);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.telegram.org/bot" + botToken + "/sendMessage"))
                    .timeout(Duration.ofSeconds(10))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();

            HttpResponse<String> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200 && response.body().contains("\"ok\":true")) {
                log.info("Telegram ✓ chatId={}", chatId);
            } else {
                log.warn("Telegram ✗ chatId={} status={} body={}",
                        chatId, response.statusCode(), response.body());
            }

        } catch (Exception e) {
            log.error("Telegram send failed chatId={}: {}", chatId, e.getMessage(), e);
        }
    }

    private String buildJson(String chatId, String text) {
        return "{"
                + "\"chat_id\":\"" + escape(textOrEmpty(chatId)) + "\","
                + "\"text\":\"" + escape(textOrEmpty(text)) + "\","
                + "\"parse_mode\":\"HTML\""
                + "}";
    }

    private static String textOrEmpty(String s) {
        return s == null ? "" : s;
    }

    /**
     * Minimal safe JSON escaping
     */
    private static String escape(String s) {
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\b", "\\b")
                .replace("\f", "\\f")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}