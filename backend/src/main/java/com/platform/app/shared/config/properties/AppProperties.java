package com.platform.app.shared.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Data
@Configuration
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private Jwt jwt = new Jwt();
    private Cors cors = new Cors();
    private Swagger swagger = new Swagger();
    private Ai ai = new Ai();

    @Data
    public static class Jwt {
        private String secret = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";
        private long expirationMs = 86400000; // 24h
        private long refreshExpirationMs = 604800000; // 7d
    }

    @Data
    public static class Cors {
        private List<String> allowedOrigins = List.of("http://localhost:3000", "http://localhost:5173");
    }

    @Data
    public static class Swagger {
        private boolean enabled = true;
        private String title = "Document and Knowledge Operations Platform API";
        private String description = "RESTful API documentation for Document and Knowledge Operations Platform";
        private String version = "1.0.0";
    }

    @Data
    public static class Ai {
        private Llm llm = new Llm();

        @Data
        public static class Llm {
            private String url = "http://localhost:8000";
            private String generatePath = "/api/v1/ai/generate";
            private String timeout = "10s";
        }
    }
}
