package com.analytics.engineering.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "integrations")
public class ExternalApiProperties {

    private String allowedOrigin = "http://localhost:4200";

    private Sonar sonar = new Sonar();
    private Jenkins jenkins = new Jenkins();

    @Data
    public static class Sonar {
        /** Ex: https://sonar.minhaempresa.com */
        private String url;
        /** Token de usuário do SonarQube (gerado em My Account > Security) */
        private String token;
        /** Chaves de projeto no Sonar, separadas por vírgula, ou vazio para buscar todos */
        private String projectKeys = "";
    }

    @Data
    public static class Jenkins {
        /** Ex: https://jenkins.minhaempresa.com */
        private String url;
        private String user;
        /** API token do Jenkins (não a senha) */
        private String apiToken;
        /** Nome do "view" ou pasta a consultar, ou vazio para raiz */
        private String view = "";
    }
}
