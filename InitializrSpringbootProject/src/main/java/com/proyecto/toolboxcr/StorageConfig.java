package com.proyecto.toolboxcr;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configura Google Cloud Storage para Firebase.
 * En local: la variable firebase.json.content queda vacía y el bean devuelve null.
 * En Render: se inyecta el JSON completo de la cuenta de servicio mediante
 * la variable de entorno FIREBASE_JSON_CONTENT.
 */
@Configuration
public class StorageConfig {

    @Value("${firebase.json.content:}")
    private String firebaseJsonContent;

    @Bean
    public Storage storage() throws IOException {
        if (firebaseJsonContent == null || firebaseJsonContent.isBlank()) {
            /* Firebase no configurado — no afecta las funciones de este proyecto */
            return null;
        }
        try (InputStream is = new ByteArrayInputStream(
                firebaseJsonContent.getBytes(StandardCharsets.UTF_8))) {
            GoogleCredentials credentials = GoogleCredentials.fromStream(is);
            return StorageOptions.newBuilder()
                    .setCredentials(credentials)
                    .build()
                    .getService();
        }
    }
}
