package com.cableops.tracker.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;

/**
 * Accepts both "2026-04-27 16:51:37" (space) and "2026-04-27T16:51:37.208Z" (ISO).
 * Applied via @JsonDeserialize(using = FlexibleDateDeserializer.class) on LocalDateTime fields.
 */
public class FlexibleDateDeserializer extends StdDeserializer<LocalDateTime> {

    private static final DateTimeFormatter FLEXIBLE = new DateTimeFormatterBuilder()
        .appendOptional(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        .appendOptional(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"))
        .appendOptional(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'"))
        .appendOptional(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS"))
        .appendOptional(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"))
        .toFormatter();

    public FlexibleDateDeserializer() {
        super(LocalDateTime.class);
    }

    @Override
    public LocalDateTime deserialize(JsonParser p, DeserializationContext ctxt)
            throws IOException {
        return LocalDateTime.parse(p.getText().trim(), FLEXIBLE);
    }
}
