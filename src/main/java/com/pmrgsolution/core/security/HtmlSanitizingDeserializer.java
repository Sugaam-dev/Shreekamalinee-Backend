package com.pmrgsolution.core.security;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import org.springframework.web.util.HtmlUtils;
import java.io.IOException;

/**
 * HTML-escaping deserializer to prevent XSS in user-supplied text fields.
 *
 * SECURITY FIX: @JsonComponent has been intentionally removed.
 * The previous version applied this globally to ALL String fields, which broke:
 *   - Passwords containing '&' or '<' (stored as &amp; / &lt;, failing login)
 *   - JWT tokens containing special characters
 *   - Any field not meant for display
 *
 * USAGE: Apply explicitly to specific display-facing fields only:
 *   @JsonDeserialize(using = HtmlSanitizingDeserializer.class)
 *   private String fieldName;
 */
public class HtmlSanitizingDeserializer extends JsonDeserializer<String> {

    @Override
    public String deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String value = p.getValueAsString();
        if (value == null) {
            return null;
        }
        // Escape HTML to prevent XSS injection in display fields
        return HtmlUtils.htmlEscape(value.trim());
    }
}
