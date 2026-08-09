package com.schwab.shortener.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.schwab.shortener.repository.ClickEventRepository;
import com.schwab.shortener.repository.ShortLinkRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import java.util.Map;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test")
class LinkControllerIntegrationTest {
    @Autowired MockMvc mvc; @Autowired ObjectMapper mapper; @Autowired ShortLinkRepository links; @Autowired ClickEventRepository clicks;
    @BeforeEach void clean(){clicks.deleteAll();links.deleteAll();}
    @Test void endToEndCreateRedirectAnalyticsDeactivate() throws Exception {
        mvc.perform(post("/api/v1/links").contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(Map.of("url","https://example.com/path","customAlias","demo1234"))))
          .andExpect(status().isCreated()).andExpect(jsonPath("$.code").value("demo1234"));
        mvc.perform(get("/demo1234").header("Referer","https://ref.example").header("User-Agent","JUnit"))
          .andExpect(status().isFound()).andExpect(header().string("Location","https://example.com/path"));
        mvc.perform(get("/api/v1/links/demo1234/analytics")).andExpect(status().isOk()).andExpect(jsonPath("$.totalClicks").value(1)).andExpect(jsonPath("$.recentClicks",hasSize(1)));
        mvc.perform(delete("/api/v1/links/demo1234")).andExpect(status().isNoContent());
        mvc.perform(get("/demo1234")).andExpect(status().isGone());
    }
    @Test void duplicateAliasReturnsConflict() throws Exception {String body=mapper.writeValueAsString(Map.of("url","https://example.com","customAlias","fixed123"));mvc.perform(post("/api/v1/links").contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isCreated());mvc.perform(post("/api/v1/links").contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isConflict());}
    @Test void invalidRequestReturns400() throws Exception {mvc.perform(post("/api/v1/links").contentType(MediaType.APPLICATION_JSON).content("{\"url\":\"file:///etc/passwd\",\"customAlias\":\"x\"}")).andExpect(status().isBadRequest());}

    @Test void generatedCodeAndMetadataWork() throws Exception {
        String response = mvc.perform(post("/api/v1/links").contentType(MediaType.APPLICATION_JSON)
            .content(mapper.writeValueAsString(Map.of("url", "https://example.org/generated"))))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.code", matchesPattern("[A-Za-z0-9]{8}")))
            .andReturn().getResponse().getContentAsString();
        String code = mapper.readTree(response).get("code").asText();
        mvc.perform(get("/api/v1/links/" + code)).andExpect(status().isOk())
            .andExpect(jsonPath("$.originalUrl").value("https://example.org/generated"));
    }

    @Test void reservedAliasIsRejected() throws Exception {
        mvc.perform(post("/api/v1/links").contentType(MediaType.APPLICATION_JSON)
            .content(mapper.writeValueAsString(Map.of("url", "https://example.com", "customAlias", "actuator"))))
            .andExpect(status().isBadRequest());
    }
    @Test void missingCodeReturns404() throws Exception {mvc.perform(get("/api/v1/links/noSuchCode")).andExpect(status().isNotFound());}
    @Test
    void blankUrlReturnsValidationErrorBody() throws Exception {
        mvc.perform(post("/api/v1/links").contentType(MediaType.APPLICATION_JSON)
                .content("{\"url\":\"   \"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Validation failed"))
            .andExpect(jsonPath("$.details", not(empty())));
    }

    @Test
    void shortAliasReturnsValidationError() throws Exception {
        mvc.perform(post("/api/v1/links").contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(Map.of("url", "https://example.com", "customAlias", "abc"))))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Validation failed"));
    }

    @Test
    void pastExpirationReturnsValidationError() throws Exception {
        mvc.perform(post("/api/v1/links").contentType(MediaType.APPLICATION_JSON)
                .content("{\"url\":\"https://example.com\",\"expiresAt\":\"2020-01-01T00:00:00Z\"}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void redirectHasNoStoreAndSecurityHeaders() throws Exception {
        mvc.perform(post("/api/v1/links").contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(Map.of("url", "https://example.com", "customAlias", "secure01"))))
            .andExpect(status().isCreated());

        mvc.perform(get("/secure01"))
            .andExpect(status().isFound())
            .andExpect(header().string("Cache-Control", containsString("no-store")))
            .andExpect(header().string("X-Content-Type-Options", "nosniff"))
            .andExpect(header().string("Referrer-Policy", "no-referrer"))
            .andExpect(header().string("Content-Security-Policy", containsString("default-src 'none'")));
    }

    @Test
    void deactivateIsIdempotent() throws Exception {
        mvc.perform(post("/api/v1/links").contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(Map.of("url", "https://example.com", "customAlias", "delete01"))))
            .andExpect(status().isCreated());
        mvc.perform(delete("/api/v1/links/delete01")).andExpect(status().isNoContent());
        mvc.perform(delete("/api/v1/links/delete01")).andExpect(status().isNoContent());
    }

    @Test
    void malformedJsonReturnsBadRequest() throws Exception {
        mvc.perform(post("/api/v1/links").contentType(MediaType.APPLICATION_JSON)
                .content("{not-json"))
            .andExpect(status().isBadRequest());
    }

}
