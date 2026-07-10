package com.proyecto.toolboxcr;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class StorageConfig {

    @Value("${firebase.bucket.name:sin-configurar}")
    private String bucketName;

    @Value("${firebase.json.content:}")
    private String firebaseJsonContent;

    @Bean
    public Storage storage() {
        try {
            System.out.println(">>> BUCKET leído: " + bucketName);
            System.out.println(">>> JSON vacío?: " + (firebaseJsonContent == null || firebaseJsonContent.isBlank()));

            if (firebaseJsonContent == null || firebaseJsonContent.isBlank()) {
                return StorageOptions.getDefaultInstance().getService();
            }

            try (InputStream is = new ByteArrayInputStream(firebaseJsonContent.getBytes(StandardCharsets.UTF_8))) {
                GoogleCredentials credentials = GoogleCredentials.fromStream(is);
                return StorageOptions.newBuilder()
                        .setCredentials(credentials)
                        .build()
                        .getService();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return StorageOptions.getDefaultInstance().getService();
        }
    }
}